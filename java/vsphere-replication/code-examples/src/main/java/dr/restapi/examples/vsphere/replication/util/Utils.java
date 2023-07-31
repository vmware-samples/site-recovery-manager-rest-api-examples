/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.util;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.Configuration;
import dr.restapi.examples.apiclient.api.TasksApi;
import dr.restapi.examples.apiclient.model.Task.StatusEnum;
import dr.restapi.examples.apiclient.model.TaskDrResponseEntity;
import dr.restapi.examples.vsphere.replication.config.Config;
import dr.restapi.examples.vsphere.replication.libraries.Constants;

import java.text.MessageFormat;
import java.util.EnumSet;

/**
 * Class with utility methods.
 */
public final class Utils {
   private static final EnumSet<StatusEnum> TASK_NOT_COMPLETED_STATUSES = EnumSet.of(StatusEnum.QUEUED, StatusEnum.RUNNING);
   private static final ApiClient API_CLIENT;

   static {
      API_CLIENT = Configuration.getDefaultApiClient();
      API_CLIENT.setVerifyingSsl(false);
      API_CLIENT.setBasePath(Config.get().getPropertyNotEmpty(Constants.Config.REST_API_BASE_PATH));
   }

   /**
    * Return the singleton instance of the API Client.
    * @return API Client singleton instance
    */
   public static ApiClient getApiClient() {
      return API_CLIENT;
   }

   /**
    * Synchronous wait for the completion of a task with the specified {@code taskId}.
    * @param apiClient api client
    * @param taskId task identifier
    * @return task entity
    */
   static TaskDrResponseEntity waitForTaskCompletion(ApiClient apiClient, String taskId) {
      TasksApi taskApi = new TasksApi(apiClient);
      TaskDrResponseEntity taskInfo = taskApi.getTaskInfo(taskId);

      while (TASK_NOT_COMPLETED_STATUSES.contains(taskInfo.getStatus())) {
         try {
            Thread.sleep(200);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }

         taskInfo = taskApi.getTaskInfo(taskId);
      }

      return taskInfo;
   }

   /**
    * Print the specified {@code message} with the related {@code messageParams} into the "standard" output stream.
    * @param message message
    * @param messageParams message parameters
    */
   public static void toSystemOut(String message, Object... messageParams) {
      System.out.println(MessageFormat.format(message, messageParams));
   }

   /**
    * Print the specified {@code message} with the related {@code messageParams} into the "standard" error stream.
    * @param message message
    * @param messageParams message parameters
    */
   public static void toSystemErr(String message, Object... messageParams) {
      System.err.println(MessageFormat.format(message, messageParams));
   }
}
