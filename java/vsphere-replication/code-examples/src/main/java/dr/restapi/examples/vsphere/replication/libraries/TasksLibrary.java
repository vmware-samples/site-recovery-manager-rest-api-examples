/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.libraries;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.ApiException;
import dr.restapi.examples.apiclient.api.TasksApi;
import dr.restapi.examples.apiclient.model.Task.StatusEnum;
import dr.restapi.examples.apiclient.model.TaskDrResponseEntity;
import dr.restapi.examples.vsphere.replication.config.Config;
import dr.restapi.examples.vsphere.replication.exceptions.ExamplesExecutionException;

import java.util.EnumSet;

import static dr.restapi.examples.vsphere.replication.util.ClientUtils.toSystemOut;

public class TasksLibrary {
   private static final EnumSet<StatusEnum> TASK_NOT_COMPLETED_STATUSES =
         EnumSet.of(StatusEnum.QUEUED, StatusEnum.RUNNING);

   private final TasksApi tasksApi;

   /**
    * Constructor.
    * @param apiClient api client
    */
   public TasksLibrary(ApiClient apiClient) {
      this.tasksApi = new TasksApi(apiClient);
   }

   /**
    * Retrieve task information.
    * @param taskId ID of a task
    * @return task entity
    * @throws ExamplesExecutionException when retrieve task information failed
    */
   public TaskDrResponseEntity callGetTaskInfo(String taskId) {
      TaskDrResponseEntity taskDrResponseEntity;
      try {
         taskDrResponseEntity = this.tasksApi.getTaskInfo(taskId);

         toSystemOut("Info for task successfully obtained.", taskId);
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'TasksApi.getTaskInfo' failed.");
      }

      return taskDrResponseEntity;
   }

   /**
    * Synchronous wait for the completion of a task with the specified {@code taskId}.
    * @param taskId task ID
    * @return task entity
    */
   public TaskDrResponseEntity waitForTaskCompletion(String taskId) {
      int retryInMs = Config.get().getPositiveInt(Constants.Config.TASK_COMPLETION_RETRY_INTERVAL);

      TaskDrResponseEntity taskInfo = callGetTaskInfo(taskId);

      while (TASK_NOT_COMPLETED_STATUSES.contains(taskInfo.getStatus())) {
         try {
            Thread.sleep(retryInMs);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }

         taskInfo = callGetTaskInfo(taskId);
      }

      toSystemOut("Task ID is [{0}]," +
                  " status is [{1}]," +
                  " description is [{2}]," +
                  " entity is [{3}]," +
                  " entity name is [{4}].",
                  taskInfo.getId(),
                  taskInfo.getStatus().toString(),
                  taskInfo.getDescription(),
                  taskInfo.getEntity(),
                  taskInfo.getEntityName());

      return taskInfo;
   }
}
