/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.util;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.model.*;
import dr.restapi.examples.vsphere.replication.config.Config;
import dr.restapi.examples.vsphere.replication.exceptions.EnvironmentPrerequisiteException;
import dr.restapi.examples.vsphere.replication.libraries.Constants;
import org.apache.commons.collections.CollectionUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Client utility class.
 */
public class ClientUtils {
   /**
    * Print the specified {@code message} with the related {@code messageParams} into the "standard" output stream.
    * @param message message
    * @param messageParams message parameters
    */
   public static void toSystemOut(String message, Object... messageParams) {
      System.out.println(MessageFormat.format(message, messageParams));
   }

   /**
    * Print one empty line into the "standard" output stream.
    */
   public static void toSystemOutEmptyLine() {
      System.out.println();
   }

   /**
    * Create an API Client with URL base path {@link Constants.Config#REST_API_BASE_PATH}.
    * @return api client
    */
   public static ApiClient createApiClient() {
      ApiClient apiClient = new ApiClient();
      apiClient.setVerifyingSsl(false);
      apiClient.setBasePath(Config.get().getPropertyNotEmpty(Constants.Config.REST_API_BASE_PATH));

      return apiClient;
   }

   /**
    * Return a pairing from the specified list {@code pairings}, which remote VC server matches {@link Constants.Config#REMOTE_VC_NAME}.
    * @param pairings list of pairings
    * @return pairing
    * @throws EnvironmentPrerequisiteException when there is no such a pairing
    */
   public static Pairing choosePairing(List<Pairing> pairings) {
      String remoteVcName = Config.get().getPropertyNotEmpty(Constants.Config.REMOTE_VC_NAME);

      if (CollectionUtils.isEmpty(pairings)) {
         throw new EnvironmentPrerequisiteException("No pairing with a remote VC name [{0}] is found.",
                                                    remoteVcName);
      }

      return pairings.stream()
                     .filter(pairing -> pairing.getRemoteVcServer().getName().equals(remoteVcName))
                     .findFirst()
                     .orElseThrow(() -> new EnvironmentPrerequisiteException(
                           "No pairing with a remote VC name [{0}] is found.",
                           remoteVcName));
   }

   /**
    * Get a list of Vms, which names matches {@link Constants.Config#REPLICATION_VMS}.
    * @param vms a list of available VMs
    * @return a list of VMs chosen to set replication
    */
   public static List<VirtualMachine> chooseReplicationVms(List<VirtualMachine> vms) {
      String[] configuredVmNames = Config.get().getPropertyNotEmpty(Constants.Config.REPLICATION_VMS).split(",");

      return Arrays.stream(configuredVmNames)
                   .map(configuredVmName -> vms.stream()
                                               .filter(localVm -> localVm.getName().equals(configuredVmName))
                                               .findFirst()
                                               .orElseThrow(() -> new EnvironmentPrerequisiteException(
                                                     "VM with name [{0}] does not exist on the local site.",
                                                     configuredVmName))
                       )
                   .collect(Collectors.toList());
   }

   /**
    * Get a vSphere Replication Management Server from the specified list {@code vrmsInfos}, which VC server matches {@link Constants.Config#REMOTE_VC_NAME}.
    * @param vrmsInfos list of vSphere Replication Management Servers
    * @return vSphere Replication Management Server
    */
   public static VrmsInfo chooseVrms(List<VrmsInfo> vrmsInfos) {
      String remoteVcName = Config.get().getPropertyNotEmpty(Constants.Config.REMOTE_VC_NAME);

      return vrmsInfos
            .stream()
            .filter(vrmsInfo -> vrmsInfo.getVcName().equals(remoteVcName))
            .findFirst().get();
   }

   /**
    * Get a storage policy from the specified list {@code storagePolicies}, which name matches {@link Constants.Config#REPLICATION_TARGET_STORAGE_POLICY}.
    * @param storagePolicies list of storage policies
    * @return storage policy
    */
   public static StoragePolicy chooseReplicationTargetStoragePolicy(List<StoragePolicy> storagePolicies) {
      String configuredStoragePolicyName = Config.get().getPropertyNotEmpty(Constants.Config.REPLICATION_TARGET_STORAGE_POLICY);

      return storagePolicies
            .stream()
            .filter(storagePolicy -> storagePolicy.getStoragePolicyName().equals(configuredStoragePolicyName))
            .findFirst()
            .orElseThrow(() -> new EnvironmentPrerequisiteException(
                  "Storage policy with name [{0}] does not exist on the remote site.",
                  configuredStoragePolicyName));
   }

   /**
    * Get a datastore from the specified list {@code datastores}, which name matches {@link Constants.Config#REPLICATION_TARGET_DATASTORE}.
    * @param datastores list of datastores
    * @return datastore
    */
   public static Datastore chooseReplicationTargetDatastore(List<Datastore> datastores) {
      String configuredDatastoreName = Config.get().getPropertyNotEmpty(Constants.Config.REPLICATION_TARGET_DATASTORE);

      return datastores
            .stream()
            .filter(datastore -> datastore.getName().equals(configuredDatastoreName))
            .findFirst()
            .orElseThrow(() -> new EnvironmentPrerequisiteException(
                  "Datastore with name [{0}] does not exist on the remote site.",
                  configuredDatastoreName));
   }
}
