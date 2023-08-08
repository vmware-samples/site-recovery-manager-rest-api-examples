/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.model.*;
import dr.restapi.examples.apiclient.model.ConfigureReplicationVmDisk.DestinationDiskFormatEnum;
import dr.restapi.examples.vsphere.replication.config.Config;
import dr.restapi.examples.vsphere.replication.libraries.*;
import dr.restapi.examples.vsphere.replication.util.ClientUtils;

import java.util.*;

import static dr.restapi.examples.vsphere.replication.util.ClientUtils.*;

public class VRCodeExamples {
   public static void main(String[] args) {
      ApiClient apiClient = ClientUtils.createApiClient();

      VRCodeExamples codeExamples = new VRCodeExamples(apiClient);
      codeExamples.runAuthenticationScenario();
      codeExamples.runConfigureReplicationScenario();
   }

   private final ApiClient apiClient;
   private final AuthenticationLibrary authenticationLibrary;
   private final PairingLibrary pairingLibrary;
   private final ReplicationLibrary replicationLibrary;
   private final TasksLibrary tasksLibrary;

   private VRCodeExamples(ApiClient apiClient) {
      this.apiClient = apiClient;
      this.authenticationLibrary = new AuthenticationLibrary(apiClient);
      this.pairingLibrary = new PairingLibrary(apiClient);
      this.replicationLibrary = new ReplicationLibrary(apiClient);
      this.tasksLibrary = new TasksLibrary(apiClient);
   }

   /**
    * Prerequisites: none
    */
   private void runAuthenticationScenario() {
      toSystemOut("=== Run Authentication Scenario...");

      SessionIdData sessionIdData = null;
      try {
         sessionIdData =
               this.authenticationLibrary.callLogin(Config.get().getPropertyNotEmpty(Constants.Config.SSO_USERNAME),
                                                    Config.get().getPropertyNotEmpty(Constants.Config.SSO_PASSWORD));
         apiClient.addDefaultHeader(Constants.SESSION_HEADER, sessionIdData.getSessionId());

         this.authenticationLibrary.callGetCurrentSession();
      } finally {
         if (sessionIdData != null) {
            this.authenticationLibrary.callLogout();
         }

         toSystemOutEmptyLine();
      }
   }

   /**
    * Prerequisites:
    * <ul>
    *    <li>Existing site pair with a remote VC.</li>
    *    <li>Existing VMs at the local site. Their names are listed in configuration property {@link Constants.Config#REPLICATION_VMS}.</li>
    * </ul>
    * <p>Steps:
    * <ol>
    *    <li>Authenticate to the API endpoint</li>
    *    <li>Use the session ID in subsequent calls</li>
    *    <li>Get VR pairings</li>
    *    <li>Create a remote session</li>
    *    <li>Search for VMs that are suitable for a replication. Query parameter {@code suitableForReplication} should be true. Choose VMs</li>
    *    <li>For each VM, get disks</li>
    *    <li>Optionally, for each VM, check capabilities if replication is supported and what are the supported replication settings</li>
    *    <li>Get storage policies at the target vCenter Server. Choose a storage policy. If default storage policy will be used, then set {@code destinationStoragePolicyId} in the resulting ConfigureReplicationSpec to {@code null}.</li>
    *    <li>Get available datastores at the target vCenter Server. Choose a target datastore</li>
    *    <li>Optionally check the selected target datastore’s compliance against the selected target storage policy</li>
    *    <li>Optionally search for seeds at the target vCenter Server for the chosen target datastore</li>
    *    <li>Construct ConfigureReplicationSpec and create a replication</li>
    * </ol>
    */
   private void runConfigureReplicationScenario() {
      toSystemOut("=== Run Configure Replication Scenario...");

      SessionIdData sessionIdData = null;
      try {
         sessionIdData =
               this.authenticationLibrary.callLogin(Config.get().getPropertyNotEmpty(Constants.Config.SSO_USERNAME),
                                                    Config.get().getPropertyNotEmpty(Constants.Config.SSO_PASSWORD));
         this.apiClient.addDefaultHeader(Constants.SESSION_HEADER, sessionIdData.getSessionId());

         List<Pairing> pairings = this.pairingLibrary.callGetAllPairings();
         Pairing pairing = ClientUtils.choosePairing(pairings);
         String pairingId = pairing.getPairingId().toString();

         this.pairingLibrary.callRemoteLogin(pairingId,
                                             Config.get().getPropertyNotEmpty(Constants.Config.REMOTE_SSO_USERNAME),
                                             Config.get().getPropertyNotEmpty(Constants.Config.REMOTE_SSO_PASSWORD));

         boolean suitableForReplication = true;
         List<VirtualMachine> vms =
               this.replicationLibrary.callGetLocalVms(pairingId, pairing.getLocalVcServer().getId(), suitableForReplication);
         List<VirtualMachine> vmsToReplication = chooseReplicationVms(vms);

         List<VrmsInfo> vrmsInfos = this.pairingLibrary.callGetAllVrmsDetails(pairingId);
         VrmsInfo vrmsInfo = ClientUtils.chooseVrms(vrmsInfos);
         List<ReplicationServerInfo> vrsInfos =
               this.pairingLibrary.callGetAllVrsDetails(pairingId, vrmsInfo.getId().toString());
         ReplicationServerInfo targetVrs = vrsInfos.get(0);

         List<StoragePolicy> vcStoragePolicies =
               this.replicationLibrary.callGetVcStoragePolicies(pairingId, pairing.getRemoteVcServer().getId());
         StoragePolicy targetVcStoragePolicy = ClientUtils.chooseReplicationTargetStoragePolicy(vcStoragePolicies);

         List<Datastore> datastores =
               this.replicationLibrary.callGetVrCapableTargetDatastores(pairingId,
                                                                        pairing.getRemoteVcServer().getId());
         Datastore targetVcDatastore = ClientUtils.chooseReplicationTargetDatastore(datastores);

         List<ConfigureReplicationSpec> specs =
               createVmsReplicationSpecs(pairing,
                                         vmsToReplication,
                                         targetVcDatastore,
                                         targetVcStoragePolicy,
                                         targetVrs);

         List<Task> replicationTasks = this.replicationLibrary.callConfigureReplication(pairingId, specs);

         for (Task replicationTask : replicationTasks) {
            this.tasksLibrary.waitForTaskCompletion(replicationTask.getId());
         }
      } finally {
         if (sessionIdData != null) {
            this.authenticationLibrary.callLogout();
         }

         toSystemOutEmptyLine();
      }
   }

   private List<ConfigureReplicationSpec> createVmsReplicationSpecs(Pairing pairing,
                                                                    List<VirtualMachine> vmsToReplication,
                                                                    Datastore targetDatastore,
                                                                    StoragePolicy targetStoragePolicy,
                                                                    ReplicationServerInfo targetVRServer) {
      List<ConfigureReplicationSpec> replicationSpecs = new ArrayList<>();

      String pairingId = pairing.getPairingId().toString();

      for (VirtualMachine vmToReplication : vmsToReplication) {
         // VM capabilities can be used to verify if a VM supports a given setting,
         // but it's not always correct to use those as default values.
         // There are different considerations for each of those.
         VmCapabilitiesDrResponseEntity vmCapabilities =
               this.replicationLibrary.callGetVmCapability(pairingId,
                                                           pairing.getLocalVcServer().getId(),
                                                           vmToReplication.getId());

         ConfigureReplicationSpec spec = new ConfigureReplicationSpec();
         spec.setAutoReplicateNewDisks(vmCapabilities.isAutoReplicateNewDisksSupported());
         spec.setRpo(vmCapabilities.getMinRpoMins() + 10L);
         spec.lwdEncryptionEnabled(vmCapabilities.isLwdEncryptionSupported());
         spec.setMpitDays(0);
         spec.setMpitEnabled(false); // can be also vmCapabilities.isMpitSupported()
         spec.setMpitInstances(0);
         spec.networkCompressionEnabled(vmCapabilities.isNetworkCompressionSupported());
         spec.setQuiesceEnabled(vmCapabilities.isQuiescingSupported());
         spec.setVmDataSetsReplicationEnabled(false);
         spec.setTargetReplicationServerId(targetVRServer.getId());
         spec.setVmId(vmToReplication.getId());
         spec.setTargetVcId(pairing.getRemoteVcServer().getId().toString());

         List<ConfigureReplicationVmDisk> diskSpecs =
               createVmDiskSpecs(vmToReplication, targetDatastore, targetStoragePolicy);
         spec.setDisks(diskSpecs);

         replicationSpecs.add(spec);
      }

      return replicationSpecs;
   }

   private List<ConfigureReplicationVmDisk> createVmDiskSpecs(VirtualMachine vm,
                                                              Datastore targetDatastore,
                                                              StoragePolicy targetStoragePolicy) {

      List<ConfigureReplicationVmDisk> diskSpecs = new ArrayList<>();

      for (VmDisk vmDisk : vm.getDisks()) {
         ConfigureReplicationVmDisk diskSpec = new ConfigureReplicationVmDisk();
         diskSpec.setDestinationDatastoreId(targetDatastore.getId());
         diskSpec.setDestinationDiskFormat(DestinationDiskFormatEnum.SAME_AS_SOURCE);
         diskSpec.setEnabledForReplication(true);
         diskSpec.setUseSeeds(false);
         diskSpec.destinationPath(null);
         diskSpec.setDestinationStoragePolicyId(targetStoragePolicy.getStoragePolicyId());
         diskSpec.setVmDisk(vmDisk);

         diskSpecs.add(diskSpec);
      }

      return diskSpecs;
   }
}
