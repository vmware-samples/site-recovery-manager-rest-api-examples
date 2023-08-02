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
 * <p>Feel free to redefine the implementations of all methods including those starting with <b>choose</b>.
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
    * Print the specified {@code message} with the related {@code messageParams} into the "standard" error stream.
    * @param message message
    * @param messageParams message parameters
    */
   public static void toSystemErr(String message, Object... messageParams) {
      System.err.println(MessageFormat.format(message, messageParams));
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
    * Create an API Client with URL base path {@link Constants.Config#REMOTE_REST_API_BASE_PATH}.
    * @return api client
    */
   public static ApiClient createRemoteApiClient() {
      ApiClient remoteApiClient = new ApiClient();
      remoteApiClient.setVerifyingSsl(false);
      remoteApiClient.setBasePath(Config.get().getPropertyNotEmpty(Constants.Config.REMOTE_REST_API_BASE_PATH));

      return remoteApiClient;
   }

   /**
    * Client decision to choose one pairing with a remote VC.
    * <p>Takes the first pairing with a remote VC.
    * @param pairings list of pairings
    * @return pairing
    * @throws EnvironmentPrerequisiteException when there is no pairing with a remote VC
    */
   public static Pairing choosePairing(List<Pairing> pairings) {
      if (CollectionUtils.isEmpty(pairings)) {
         throw new EnvironmentPrerequisiteException("No pairing with a remote VC is found.");
      }

      return pairings.stream()
                     .filter(pairing -> !pairing.getLocalVcServer().getId().equals(pairing.getRemoteVcServer().getId()))
                     .findFirst()
                     .orElseThrow(() -> new EnvironmentPrerequisiteException("No pairing with a remote VC is found."));
   }

   /**
    * Client decision to choose VMs to set replication.
    * <p>Decision is made by the value of {@link Constants.Config#LOCAL_VMS}.
    * @param localVms a list of VMs available at the local site
    * @return a list of VMs chosen to set replication
    */
   public static List<VirtualMachine> chooseLocalVms(List<VirtualMachine> localVms) {
      String[] configuredVmNames = Config.get().getPropertyNotEmpty(Constants.Config.LOCAL_VMS).split(",");

      return Arrays.stream(configuredVmNames)
                   .map(configuredVmName -> localVms.stream()
                                                    .filter(localVm -> localVm.getName().equals(configuredVmName))
                                                    .findFirst()
                                                    .orElseThrow(() -> new EnvironmentPrerequisiteException(
                                                          "VM with name [{0}] does not exist on the local site.",
                                                          configuredVmName))
                       )
                   .collect(Collectors.toList());
   }

   /**
    * Client decision to choose one replication server from the list.
    * <p>Takes the first replication server from the list.
    * @param vrs list of replication servers
    * @return replication server
    */
   public static ReplicationServerInfo chooseRemoteVR(List<ReplicationServerInfo> vrs) {
      if (CollectionUtils.isEmpty(vrs)) {
         new EnvironmentPrerequisiteException("No pairing with a remote site.");
      }

      return vrs.get(0);
   }

   /**
    * Client decision to choose one storage policy from the list.
    * <p>Decision is made by the value of {@link Constants.Config#REMOTE_STORAGE_POLICY}.
    * @param remoteStoragePolicies list of storage policies on the remote site
    * @return storage policy
    */
   public static StoragePolicy chooseRemoteStoragePolicy(List<StoragePolicy> remoteStoragePolicies) {
      String configuredStoragePolicyName = Config.get().getPropertyNotEmpty(Constants.Config.REMOTE_STORAGE_POLICY);

      return remoteStoragePolicies
            .stream()
            .filter(storagePolicy -> storagePolicy.getStoragePolicyName().equals(configuredStoragePolicyName))
            .findFirst()
            .orElseThrow(() -> new EnvironmentPrerequisiteException(
                  "Storage policy with name [{0}] does not exist on the remote site.",
                  configuredStoragePolicyName));
   }

   /**
    * Client decision to choose one datastore from the list.
    * <p>Decision is made by the value of {@link Constants.Config#REMOTE_DATASTORE}.
    * @param remoteVcDatastores list of remote VC datastores
    * @return datastore
    */
   public static Datastore chooseRemoteDatastore(List<Datastore> remoteVcDatastores) {
      String configuredDatastoreName = Config.get().getPropertyNotEmpty(Constants.Config.REMOTE_DATASTORE);

      return remoteVcDatastores
            .stream()
            .filter(datastore -> datastore.getName().equals(configuredDatastoreName))
            .findFirst()
            .orElseThrow(() -> new EnvironmentPrerequisiteException(
                  "Datastore with name [{0}] does not exist on the remote site.",
                  configuredDatastoreName));
   }
}
