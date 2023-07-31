/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.libraries;

/**
 * Contain String literal constants.
 */
public final class Constants {
   /**
    * Contain names of the properties in the configuration file {@code dr-rest-api-examples.properties}.
    */
   public static final class Config {
      public static final String SSO_USERNAME = "sso-username";
      public static final String SSO_PASSWORD = "sso-password";
      public static final String REMOTE_SSO_USERNAME = "remote-sso-username";
      public static final String REMOTE_SSO_PASSWORD = "remote-sso-password";
      public static final String REST_API_BASE_PATH = "rest-api-base-path";
      public static final String SESSION_ID = "session-id";

      private Config() {
      }
   }

   public static final String SESSION_HEADER = "x-dr-session";
   public static final String BASIC_AUTH = "BasicAuth";

   private Constants() {
   }
}
