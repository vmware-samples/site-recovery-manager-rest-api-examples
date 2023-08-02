/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.srm;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.api.TasksApi;
import dr.restapi.examples.apiclient.model.Task.StatusEnum;
import dr.restapi.examples.apiclient.model.TaskDrResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class containing helper methods.
 */
public class Util {

   private static final String CONFIG_FILE_NAME = "dr-rest-api-examples.properties";

   private static final String MOREF_PARTS_SEPARATOR = ":";


   /**
    * Waits for a specific task to finish execution and retrieves the final status.
    * The method will block the current thread until the task is no longer in "RUNNING" or "QUEUED" state.
    *
    * @param client The ApiClient instance used for making API calls.
    * @param taskId The ID of the task to monitor.
    * @return A TaskDrResponseEntity object containing the final status and other information about the task.
    * @throws RuntimeException If any error occurs during the API call or the thread is interrupted.
    */
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

   /**
    * Loads properties from a configuration file.
    *
    * @return A Properties object containing the loaded properties.
    * @throws RuntimeException If an IOException occurs during the loading process.
    */
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

   /**
    * Converts a string representation of a ManagedObjectReference (MoRef) to its individual parts.
    * The MoRef string is expected to be in the format "type:value:serverGuid".
    *
    * @param stringValue The string representation of the ManagedObjectReference.
    * @return An array of strings containing the individual parts of the ManagedObjectReference.
    * @throws IllegalArgumentException If the input string is not in the valid format "type:value:serverGuid".
    */
   public static String[] stringToMoref(String stringValue) {

      String[] taskParts = stringValue.split(MOREF_PARTS_SEPARATOR);
      if (taskParts.length != 3) {
         throw new IllegalArgumentException(stringValue + " is not a valid ManagedObjectReference.");
      }

      return taskParts;
   }
}