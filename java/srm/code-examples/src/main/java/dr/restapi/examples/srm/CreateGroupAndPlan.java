/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.srm;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.api.ProtectionApi;
import dr.restapi.examples.apiclient.api.RecoveryApi;
import dr.restapi.examples.apiclient.model.*;
import dr.restapi.examples.apiclient.model.ProtectionGroupCreateSpec.ReplicationTypeEnum;

import java.util.*;

import static dr.restapi.examples.srm.PrepareSetup.*;
import static dr.restapi.examples.srm.Util.*;

public class CreateGroupAndPlan {

   private static final Properties properties;
   private static final String REMOTE_VC_NAME;
   private static final String PROTECTED_VC_GUID;
   private static final String GROUP_NAME;
   private static final String GROUP_DESCRIPTION;
   private static final String GROUP_LOCATION;
   private static final String REPLICATION_TYPE;
   private static final String VM;
   private static final String PLAN_NAME;
   private static final String PLAN_DESCRIPTION;
   private static final String PLAN_LOCATION;
   private static final String PLAN_TARGET_NETWORK;
   private static final String PLAN_TEST_NETWORK;

   static {
      properties = loadProperties();
      REMOTE_VC_NAME = properties.getProperty("remote_vc_name");
      PROTECTED_VC_GUID = properties.getProperty("protected_vc_guid");
      GROUP_NAME = properties.getProperty("group.name");
      GROUP_DESCRIPTION = properties.getProperty("group.description");
      GROUP_LOCATION = properties.getProperty("group.location");
      REPLICATION_TYPE = properties.getProperty("replication_type");
      VM = properties.getProperty("vm");
      PLAN_NAME = properties.getProperty("plan.name");
      PLAN_DESCRIPTION = properties.getProperty("plan.description");
      PLAN_LOCATION = properties.getProperty("plan.location");
      PLAN_TARGET_NETWORK = properties.getProperty("plan.target_network");
      PLAN_TEST_NETWORK = properties.getProperty("plan.test_network");

   }

   public static void main(String[] args) {
      CreateGroupAndPlan obj = new CreateGroupAndPlan();
      obj.runWorkflow();
   }

   /**
    * Private utility method for executing a workflow to perform specific tasks using the API client.
    * The method performs the following tasks in sequence:
    * 1. Initializes the API client.
    * 2. Logs in and obtains a session ID for authentication.
    * 3. Sets the session ID as a default header for subsequent API calls.
    * 4. Retrieves a list of pairings.
    * 5. Identifies the pairing ID for the remote VC with a specific name.
    * 6. Logs in remotely using the identified pairing ID.
    * 7. Creates a protection group using the API client and pairing ID.
    * 8. Waits for the protection group creation task to complete.
    * 9. Creates a recovery plan using the API client, pairing ID, and protection group ID.
    * 10. Waits for the recovery plan creation task to complete.
    */
   private void runWorkflow() {

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

      TaskDrResponseEntity groupTaskEntity = runCreateGroup(client, pairingId.toString());
      groupTaskEntity = waitTaskEnd(client, groupTaskEntity.getId());
      System.out.println(groupTaskEntity);

      String groupId = groupTaskEntity.getResult().toString();

      TaskDrResponseEntity planTaskEntity = runCreatePlan(client, pairingId.toString(), groupId);
      planTaskEntity = waitTaskEnd(client, planTaskEntity.getId());
      System.out.println(planTaskEntity);
   }

   /**
    * Private utility method for creating a protection group with the specified parameters using the API client.
    * The method calls the ProtectionApi to create a new protection group based on the provided parameters.
    *
    * @param client    The ApiClient instance used for making API calls.
    * @param pairingId The pairing ID associated with the protection group.
    * @return A TaskDrResponseEntity object representing the asynchronous task response for creating the group.
    */
   private TaskDrResponseEntity runCreateGroup(ApiClient client, String pairingId) {

      ProtectionApi instance = new ProtectionApi(client);

      ProtectionGroupCreateSpec groupSpec = new ProtectionGroupCreateSpec();
      groupSpec.setName(GROUP_NAME);
      groupSpec.setDescription(GROUP_DESCRIPTION);
      groupSpec.setLocation(GROUP_LOCATION);
      groupSpec.setReplicationType(ReplicationTypeEnum.fromValue(REPLICATION_TYPE));
      groupSpec.setProtectedVcGuid(UUID.fromString(PROTECTED_VC_GUID));
      HbrProtectionGroupSpec hbrSpec = new HbrProtectionGroupSpec();
      hbrSpec.setVms(List.of(VM));
      groupSpec.setHbrSpec(hbrSpec);

      return instance.createGroup(groupSpec, pairingId);
   }

   /**
    * Private utility method for creating a recovery plan with the specified parameters using the API client.
    * The method calls the RecoveryApi to create a new recovery plan based on the provided parameters.
    *
    * @param client          The ApiClient instance used for making API calls.
    * @param pairingId       The pairing ID associated with the recovery plan.
    * @param protectionGroup The protection group to be included in the recovery plan.
    * @return A TaskDrResponseEntity object representing the asynchronous task response for creating the plan.
    */
   private TaskDrResponseEntity runCreatePlan(ApiClient client, String pairingId, String protectionGroup) {

      RecoveryApi instance = new RecoveryApi(client);

      RecoveryPlanCreateSpec planSpec = new RecoveryPlanCreateSpec();
      planSpec.setName(PLAN_NAME);
      planSpec.setDescription(PLAN_DESCRIPTION);
      planSpec.setLocation(PLAN_LOCATION);
      planSpec.setProtectedVcGuid(UUID.fromString(PROTECTED_VC_GUID));
      TestNetworkMappingsSpec testNetworkMapping = new TestNetworkMappingsSpec();
      testNetworkMapping.setTestNetwork(PLAN_TEST_NETWORK);
      testNetworkMapping.setTargetNetwork(PLAN_TARGET_NETWORK);
      planSpec.setTestNetworkMappings(List.of(testNetworkMapping));
      planSpec.setProtectionGroups(List.of(protectionGroup));

      return instance.createPlan(planSpec, pairingId);
   }
}