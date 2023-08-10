/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.libraries;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.ApiException;
import dr.restapi.examples.apiclient.api.ReplicationApi;
import dr.restapi.examples.apiclient.model.*;
import dr.restapi.examples.vsphere.replication.exceptions.ExamplesExecutionException;

import java.util.List;
import java.util.UUID;

import static dr.restapi.examples.vsphere.replication.util.ClientUtils.toSystemOut;

public class ReplicationLibrary {
   private final ReplicationApi replicationApi;

   /**
    * Constructor.
    * @param apiClient api client
    */
   public ReplicationLibrary(ApiClient apiClient) {
      this.replicationApi = new ReplicationApi(apiClient);
   }

   /**
    * Get a list of all VMs on the VC.
    * @param pairingId pairing ID
    * @param vcenterId VC ID
    * @param suitableForReplication whether VM is suitable for a replication
    * @return session ID data
    * @throws ExamplesExecutionException when get VMs of a VC failed
    */
   public List<VirtualMachine> callGetLocalVms(String pairingId, UUID vcenterId, boolean suitableForReplication) {
      List<VirtualMachine> vms;
      try {
         vms = this.replicationApi.getLocalVms(pairingId,
                                               vcenterId,
                                               null,
                                               null,
                                               null,
                                               null,
                                               null,
                                               null,
                                               suitableForReplication)
                                  .getList();

         toSystemOut("Get a list of all VMs on a VC completed.", pairingId, vcenterId);
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'ReplicationApi.getLocalVms' failed.");
      }

      return vms;
   }

   /**
    * Get vSphere Replication capability information about a given VM.
    * @param pairingId pairing ID
    * @param vcenterId VC ID
    * @param vmId VM ID
    * @return VM capabilities
    * @throws ExamplesExecutionException when get VM capability information failed
    */
   public VmCapabilitiesDrResponseEntity callGetVmCapability(String pairingId, UUID vcenterId, String vmId) {
      VmCapabilitiesDrResponseEntity vmCapabilities;
      try {
         vmCapabilities = this.replicationApi.getVmCapability(pairingId,
                                                              vcenterId,
                                                              vmId,
                                                              null,
                                                              null,
                                                              null,
                                                              null,
                                                              null,
                                                              null);

         toSystemOut("Get vSphere Replication capability information about a given VM with Id [{0}] completed.",
                     vmId);
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'ReplicationApi.getVmCapability' failed.");
      }

      return vmCapabilities;
   }

   /**
    * Get VC storage policies.
    * @param pairingId pairing ID
    * @param vcenterId VC ID
    * @return storage policies
    * @throws ExamplesExecutionException when get VC storage policies failed
    */
   public List<StoragePolicy> callGetVcStoragePolicies(String pairingId, UUID vcenterId) {
      List<StoragePolicy> storagePolicies;
      try {
         storagePolicies = this.replicationApi.getVcStoragePolicies(pairingId,
                                                                    vcenterId,
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    null)
                                              .getList();

         toSystemOut("Get VC storage policies completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'ReplicationApi.getVcStoragePolicies' failed.");
      }

      return storagePolicies;
   }

   /**
    * Get VR supported datastores.
    * @param pairingId pairing ID
    * @param vcenterId VC ID
    * @return list of datastores
    * @throws ExamplesExecutionException when get VC supported datastores failed
    */
   public List<Datastore> callGetVrCapableTargetDatastores(String pairingId, UUID vcenterId) {
      List<Datastore> datastores;
      try {
         datastores = this.replicationApi.getVrCapableTargetDatastores(pairingId,
                                                                       vcenterId,
                                                                       null,
                                                                       null,
                                                                       null,
                                                                       null,
                                                                       null,
                                                                       null)
                                         .getList();

         toSystemOut("Get VR supported datastores completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'ReplicationApi.getVrCapableTargetDatastores' failed.");
      }

      return datastores;
   }

   /**
    * Configure replication for a VM from a source site to a target vCenter Server site.
    * @param pairingId pairing ID
    * @param specs configure replication specs
    * @return list of tasks
    * @throws ExamplesExecutionException when configure replication failed
    */
   public List<Task> callConfigureReplication(String pairingId, List<ConfigureReplicationSpec> specs) {
      List<Task> tasks;
      try {
         tasks = this.replicationApi.configureReplication(pairingId, specs).getList();

         toSystemOut("Configure replication completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'ReplicationApi.configureReplication' failed.");
      }

      return tasks;
   }
}
