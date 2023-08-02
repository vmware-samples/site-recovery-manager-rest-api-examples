/* Copyright (c) 2023 VMware, Inc. All rights reserved. -- VMware Confidential */
package dr.restapi.examples.srm;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.api.TasksApi;
import dr.restapi.examples.apiclient.model.Task.StatusEnum;
import dr.restapi.examples.apiclient.model.TaskDrResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Util {

   private static final String CONFIG_FILE_NAME = "dr-rest-api-examples.properties";

   private static final String MOREF_PARTS_SEPARATOR = ":";


   public static TaskDrResponseEntity waitTaskEnd(ApiClient client, String taskId) {

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

   public static Properties loadProperties() {

      ClassLoader loader = Util.class.getClassLoader();
      InputStream input = loader.getResourceAsStream(CONFIG_FILE_NAME);
      Properties prop = new Properties();
      try (InputStream io = input) {
         prop.load(io);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return prop;
   }

   public static String[] stringToMoref(String stringValue) {

      String[] taskParts = stringValue.split(MOREF_PARTS_SEPARATOR);
      if (taskParts.length != 3) {
         throw new IllegalArgumentException(stringValue + " is not a valid ManagedObjectReference.");
      }

      return taskParts;
   }
}