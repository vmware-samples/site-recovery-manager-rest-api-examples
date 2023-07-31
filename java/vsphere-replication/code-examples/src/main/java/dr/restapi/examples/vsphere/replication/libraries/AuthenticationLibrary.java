/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.libraries;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.ApiException;
import dr.restapi.examples.apiclient.api.AuthenticationApi;
import dr.restapi.examples.apiclient.auth.HttpBasicAuth;
import dr.restapi.examples.apiclient.model.SessionIdData;
import dr.restapi.examples.apiclient.model.SessionInfo;
import dr.restapi.examples.vsphere.replication.exceptions.ExamplesExecutionException;

import static dr.restapi.examples.vsphere.replication.util.Utils.toSystemOut;

public class AuthenticationLibrary {
   private final AuthenticationApi authenticationApi;

   /**
    * Constructor.
    * @param apiClient api client
    */
   public AuthenticationLibrary(ApiClient apiClient) {
      this.authenticationApi = new AuthenticationApi(apiClient);
   }

   /**
    * Logs in and returns the session ID. In the subsequent requests, include the {@code x-dr-session} header with the
    * returned session ID value.
    * @param username username
    * @param password password
    * @return SessionIdData session id data
    * @throws ExamplesExecutionException when login failed
    */
   public SessionIdData callLogin(String username, String password) {
      HttpBasicAuth basicAuth = (HttpBasicAuth) this.authenticationApi.getApiClient().getAuthentication(Constants.BASIC_AUTH);
      basicAuth.setUsername(username);
      basicAuth.setPassword(password);

      SessionIdData sessionIdData;
      try {
         sessionIdData = this.authenticationApi.login();

         toSystemOut("New session is created. Session ID is [{0}].", sessionIdData.getSessionId());
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'AuthenticationApi.login' failed.");
      }

      return sessionIdData;
   }

   /**
    * Returns information about the current session, if any.
    * @return SessionInfo session info
    * @throws ExamplesExecutionException when there is no authenticated session
    */
   public SessionInfo callGetCurrentSession() {
      SessionInfo sessionInfo;
      try {
         sessionInfo = this.authenticationApi.getCurrentSession();

         toSystemOut("Current session's username is [{0}].", sessionInfo.getUsername());
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'AuthenticationApi.callGetCurrentSession' failed.");
      }

      return sessionInfo;
   }

   /**
    * Logs out if the session is authenticated. Logs out if the session is authenticated.
    * @throws ExamplesExecutionException when logout failed
    */
   public void callLogout() {
      try {
         this.authenticationApi.logout();

         toSystemOut("Successful session logout.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'AuthenticationApi.callLogout' failed.");
      }
   }
}
