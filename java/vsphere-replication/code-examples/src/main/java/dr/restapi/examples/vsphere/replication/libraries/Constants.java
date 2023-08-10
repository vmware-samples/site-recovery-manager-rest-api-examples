/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.libraries;

/**
 * Contain String literal constants.
 */
public final class Constants {
   /**
    * Contain names of the properties in the configuration file {@link dr.restapi.examples.vsphere.replication.config.Config#CONFIG_FILE Config#CONFIG_FILE }.
    */
   public static final class Config {
      public static final String REST_API_BASE_PATH = "rest-api-base-path";
      public static final String SSO_USERNAME = "sso-username";
      public static final String SSO_PASSWORD = "sso-password";
      public static final String REMOTE_VC_NAME = "remote-vc-name";
      public static final String REMOTE_SSO_USERNAME = "remote-sso-username";
      public static final String REMOTE_SSO_PASSWORD = "remote-sso-password";
      public static final String REPLICATION_VMS = "replication-vms";
      public static final String REPLICATION_TARGET_STORAGE_POLICY = "replication-target-storage-policy";
      public static final String REPLICATION_TARGET_DATASTORE = "replication-target-datastore";
      public static final String TASK_COMPLETION_RETRY_INTERVAL = "task-completion-retry-interval";

      private Config() {
      }
   }

   public static final String SESSION_HEADER = "x-dr-session";
   public static final String BASIC_AUTH = "BasicAuth";
   public static final String REMOTE_LOGIN_BASIC_AUTH = "RemoteLoginBasicAuth";

   private Constants() {
   }
}
