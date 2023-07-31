/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.srm;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.Configuration;
import dr.restapi.examples.apiclient.api.*;
import dr.restapi.examples.apiclient.auth.HttpBasicAuth;
import dr.restapi.examples.apiclient.model.*;
import dr.restapi.examples.apiclient.model.ProtectionGroupCreateSpec.ReplicationTypeEnum;
import dr.restapi.examples.apiclient.model.Task.StatusEnum;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MyFirstExample {

   private static final String SSO_USERNAME = "administrator@vsphere.local";
   private static final String SSO_PASSWORD = "vmware";
   private static final String PATH = "https://10.93.25.20:8443/dr-rest-api/srm/v2";
   private static final String REMOTE_SSO_USERNAME = "administrator@vsphere.local";
   private static final String REMOTE_SSO_PASSWORD = "vmware";
   private static final String REMOTE_VC_NAME = "s3-srm3-233-102.eng.vmware.com";
   private static final String CONFIG_FILE_NAME = "dr-rest-api-examples.properties";

   private static final String MOREF_PARTS_SEPARATOR = ":";

   private static final Properties properties;
   private static final String PROTECTED_VC_GUID;
   private static final String NAME;
   private static final String DESCRIPTION;
   private static final String LOCATION;
   private static final String REPLICATION_TYPE;
   private static final String VM;

   static {
      properties = loadProperties();
      PROTECTED_VC_GUID = properties.getProperty("protected_vc_guid");
      NAME = properties.getProperty("name");
      DESCRIPTION = properties.getProperty("description");
      LOCATION = properties.getProperty("location");
      REPLICATION_TYPE = properties.getProperty("replication_type");
      VM = properties.getProperty("vm");
   }


   public static void main(String[] args) {

      MyFirstExample obj = new MyFirstExample();
      obj.runWorkflow();

   }

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

      TaskDrResponseEntity taskEntity = runCreateGroup(client, pairingId.toString());
      taskEntity = waitTaskEnd(client, taskEntity.getId());
      System.out.println(taskEntity);

      String groupId = taskEntity.getResult().toString();
      System.out.println(Arrays.toString(stringToMoref(groupId)));

   }

   private ApiClient initApiClient() {

      ApiClient client = Configuration.getDefaultApiClient();
      client.setVerifyingSsl(false);
      client.setBasePath(PATH);
      return client;
   }

   private String runLogin(ApiClient client) {

      HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("BasicAuth");
      auth.setUsername(SSO_USERNAME);
      auth.setPassword(SSO_PASSWORD);

      AuthenticationApi instance = new AuthenticationApi();
      instance.setApiClient(client);

      SessionIdData sessionIdData = null;
      sessionIdData = instance.login();

      return sessionIdData.getSessionId();

   }

   private PairingDrResponseList runGetAllPairings(ApiClient client) {

      PairingApi instance = new PairingApi();
      instance.setApiClient(client);

      String filterProp = null;
      List<String> filter = null;
      String sortBy = null;
      String orderBy = null;
      String limit = null;
      String offset = null;

      PairingDrResponseList pairingList = instance.getPairings(filterProp, filter, sortBy, orderBy, limit, offset);

      return pairingList;
   }

   private void runRemoteLogin(ApiClient client, String pairingId) {

      HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("RemoteLoginBasicAuth");
      auth.setUsername(REMOTE_SSO_USERNAME);
      auth.setPassword(REMOTE_SSO_PASSWORD);

      PairingApi instance = new PairingApi();
      instance.setApiClient(client);

      instance.createRemoteSession(pairingId);
   }

   private TaskDrResponseEntity runCreateGroup(ApiClient client, String pairingId) {

      ProtectionApi instance = new ProtectionApi(client);

      HbrProtectionGroupSpec hbrSpec = new HbrProtectionGroupSpec();
      hbrSpec.setVms(List.of(VM));

      ProtectionGroupCreateSpec body = new ProtectionGroupCreateSpec();
      body.setName(NAME);
      body.setDescription(DESCRIPTION);
      body.setLocation(LOCATION);
      body.setReplicationType(ReplicationTypeEnum.fromValue(REPLICATION_TYPE));
      body.setProtectedVcGuid(UUID.fromString(PROTECTED_VC_GUID));
      body.setHbrSpec(hbrSpec);

      return instance.createGroup(body, pairingId);
   }

   private TaskDrResponseEntity waitTaskEnd(ApiClient client, String taskId) {

      TasksApi instance = new TasksApi(client);
      TaskDrResponseEntity info = instance.getTaskInfo(taskId);
      StatusEnum taskStatus = info.getStatus();

      while ("RUNNING".equals(taskStatus.toString()) || "QUEUED".equals(taskStatus.toString())) {

         try {
            Thread.sleep(500);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         info = instance.getTaskInfo(taskId);
         taskStatus = info.getStatus();
      }
      return info;
   }

   private static Properties loadProperties() {

      ClassLoader loader = MyFirstExample.class.getClassLoader();
      InputStream input = loader.getResourceAsStream(CONFIG_FILE_NAME);

      Properties prop = new Properties();
      try (InputStream io = input) {
         prop.load(io);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return prop;
   }

   private static String[] stringToMoref(String stringValue) {

      String[] taskParts = stringValue.split(MOREF_PARTS_SEPARATOR);
      if (taskParts.length != 3) {
         throw new IllegalArgumentException(stringValue + " is not a valid ManagedObjectReference.");
      }

      return taskParts;
   }
}