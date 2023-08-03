/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.srm;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.api.RecoveryApi;
import dr.restapi.examples.apiclient.model.*;

import java.util.Properties;
import java.util.UUID;

import static dr.restapi.examples.srm.PrepareSetup.*;
import static dr.restapi.examples.srm.PrepareSetup.runRemoteLogin;
import static dr.restapi.examples.srm.Util.loadProperties;

public class RecoveryActions {

   private static final Properties properties;
   private static final String REMOTE_VC_NAME;
   private static final String RECOVERY_PLAN_ID;
   private static final String SYNC_DATA;
   private static final String FORCED;
   private static final String SKIP_PROTECTION_SITE_OPERATIONS;
   private static final String MIGRATE_ELIGIBLE_VMS;
   private static final String PLANNED_FAILOVER;

   static {
      properties = loadProperties();
      REMOTE_VC_NAME = properties.getProperty("remote_vc_name");
      RECOVERY_PLAN_ID = properties.getProperty("plan.id");
      SYNC_DATA = properties.getProperty("plan.sync_data");
      FORCED = properties.getProperty("plan.forced");
      SKIP_PROTECTION_SITE_OPERATIONS = properties.getProperty("plan.skip_protection_site_operations");
      MIGRATE_ELIGIBLE_VMS = properties.getProperty("plan.migrate_eligible_vms");
      PLANNED_FAILOVER = properties.getProperty("plan.planned_failover");
   }

   public static void main(String[] args) {
      RecoveryActions obj = new RecoveryActions();
      obj.run();
   }

   /**
    * Private utility method for running a series of operations as part of a Disaster Recovery workflow.
    * The method executes the following tasks in sequence:
    * 1. Initializes the API client.
    * 2. Logs in and obtains a session ID for authentication.
    * 3. Sets the session ID as a default header for subsequent API calls.
    * 4. Retrieves a list of pairings.
    * 5. Identifies the pairing ID for the remote VC with a specific name.
    * 6. Logs in remotely using the identified pairing ID.
    * 7. Runs a test recovery plan using the API client and pairing ID.
    *
    * Note: Additional tasks (commented out) are provided for reference and can be uncommented to run specific operations:
    * - runCancelRecoveryPlan(client, pairingId.toString()): Cancels a recovery plan for the identified pairing.
    * - runCleanupRecoveryPlan(client, pairingId.toString()): Initiates a cleanup on a test recovery plan for the pairing.
    * - runRecovery(client, pairingId.toString()): Initiates a full recovery on the pairing's recovery plan.
    * - runReprotect(client, pairingId.toString()): Initiates a reprotect operation on a previously recovered VM.
    */
   private void run() {

      ApiClient client = initApiClient();
      String sessionId = runLogin(client);

      client.addDefaultHeader("x-dr-session", sessionId);

      PairingDrResponseList pairingList = runGetAllPairings(client);
      System.out.println(pairingList);

      UUID pairingId = null;
      for (Pairing pairing : pairingList.getList()) {
         if (pairing.getRemoteVcServer().getName().equals(REMOTE_VC_NAME)) {
            pairingId = pairing.getPairingId();
         }
      }

      runRemoteLogin(client, pairingId.toString());

      runTestRecoveryPlan(client, pairingId.toString());

      // Step 7: (Optional) Uncomment the following lines to run specific operations
      // runCancelRecoveryPlan(client, pairingId.toString());
      // runCleanupRecoveryPlan(client, pairingId.toString());
      // runRecovery(client, pairingId.toString());
      // runReprotect(client, pairingId.toString());
   }

   /**
    * Private utility method for running a test recovery plan using the API client and pairing ID.
    * The method calls the RecoveryApi to initiate a test recovery operation for a specified recovery plan.
    *
    * @param client    The ApiClient instance used for making API calls.
    * @param pairingId The pairing ID associated with the remote VC for the test recovery.
    */
   private void runTestRecoveryPlan(ApiClient client, String pairingId) {

      RecoveryApi instance = new RecoveryApi(client);

      TestPlanSpec spec = new TestPlanSpec();
      spec.setSyncData(Boolean.valueOf(SYNC_DATA));

      instance.runTestRecovery(spec, pairingId, RECOVERY_PLAN_ID);
   }

   /**
    * Private utility method for running a cleanup on a test recovery plan using the API client and pairing ID.
    * The method calls the RecoveryApi to initiate a cleanup operation on a previously executed test recovery plan.
    *
    * @param client    The ApiClient instance used for making API calls.
    * @param pairingId The pairing ID associated with the remote VC for the cleanup operation.
    */
   private void runCleanupRecoveryPlan(ApiClient client, String pairingId) {

      RecoveryApi instance = new RecoveryApi(client);

      CleanupTestPlanSpec spec = new CleanupTestPlanSpec();
      spec.forced(Boolean.valueOf(FORCED));

      instance.runCleanupTestRecovery(spec, pairingId, RECOVERY_PLAN_ID);
   }

   /**
    * Private utility method for canceling a recovery plan using the API client and pairing ID.
    * The method calls the RecoveryApi to cancel an ongoing or scheduled recovery plan associated with a pairing.
    *
    * @param client    The ApiClient instance used for making API calls.
    * @param pairingId The pairing ID associated with the remote VC and the recovery plan.
    */
   private void runCancelRecoveryPlan(ApiClient client, String pairingId) {

      RecoveryApi instance = new RecoveryApi(client);

      instance.cancelRecoveryPlan(pairingId, RECOVERY_PLAN_ID);
   }

   /**
    * Private utility method for initiating a recovery operation using the API client and pairing ID.
    * The method calls the RecoveryApi to execute a recovery plan based on the specified recovery plan ID.
    *
    * @param client    The ApiClient instance used for making API calls.
    * @param pairingId The pairing ID associated with the remote VC and the recovery plan.
    */
   private void runRecovery(ApiClient client, String pairingId) {

      RecoveryApi instance = new RecoveryApi(client);

      RecoverPlanSpec spec = new RecoverPlanSpec();
      spec.setSyncData(Boolean.valueOf(SYNC_DATA));
      spec.setPlannedFailover(Boolean.valueOf(PLANNED_FAILOVER));
      spec.setMigrateEligibleVms(Boolean.valueOf(MIGRATE_ELIGIBLE_VMS));
      spec.setSkipProtectionSiteOperations(Boolean.valueOf(SKIP_PROTECTION_SITE_OPERATIONS));

      instance.runRecovery(spec, pairingId, RECOVERY_PLAN_ID);
   }

   /**
    * Private utility method for initiating a reprotect operation using the API client and pairing ID.
    * The method calls the RecoveryApi to execute a reprotect operation on a previously recovered VM.
    *
    * @param client    The ApiClient instance used for making API calls.
    * @param pairingId The pairing ID associated with the remote VC and the recovery plan.
    */
   private void runReprotect(ApiClient client, String pairingId) {

      RecoveryApi instance = new RecoveryApi(client);

      ReprotectPlanSpec spec = new ReprotectPlanSpec();
      spec.forced(Boolean.valueOf(FORCED));

      instance.runReprotect(spec, pairingId, RECOVERY_PLAN_ID);
   }
}