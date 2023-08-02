/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.srm;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.Configuration;
import dr.restapi.examples.apiclient.api.AuthenticationApi;
import dr.restapi.examples.apiclient.api.PairingApi;
import dr.restapi.examples.apiclient.auth.HttpBasicAuth;
import dr.restapi.examples.apiclient.model.PairingDrResponseList;
import dr.restapi.examples.apiclient.model.SessionIdData;

import java.util.Base64;
import java.util.List;
import java.util.Properties;

import static dr.restapi.examples.srm.Util.loadProperties;

public class PrepareSetup {

   private static final Properties properties;
   private static final String PATH;
   private static final String SSO_USERNAME;
   private static final String SSO_PASSWORD;
   private static final String REMOTE_SSO_USERNAME;
   private static final String REMOTE_SSO_PASSWORD;

   static {
      properties = loadProperties();
      PATH = properties.getProperty("path");
      SSO_USERNAME = properties.getProperty("sso_username");
      SSO_PASSWORD = properties.getProperty("sso_password");
      REMOTE_SSO_USERNAME = properties.getProperty("remote_sso_username");
      REMOTE_SSO_PASSWORD = properties.getProperty("remote_sso_password");
   }

   /**
    * Initializes and configures an instance of the ApiClient for making API calls.
    * The method sets the base path for the API client and disables SSL certificate verification.
    *
    * @return An initialized ApiClient instance.
    */
   public static ApiClient initApiClient() {

      ApiClient client = Configuration.getDefaultApiClient();
      client.setVerifyingSsl(false);
      client.setBasePath(PATH);
      return client;
   }

   /**
    * Runs a login operation using the provided ApiClient with Single Sign-On (SSO) credentials.
    * The method sets the SSO username and password, then invokes the API to log in and obtain a session ID.
    *
    * @param client The ApiClient instance used for making API calls.
    * @return The session ID obtained after a successful login.
    */
   public static String runLogin(ApiClient client) {

      HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("BasicAuth");
      auth.setUsername(SSO_USERNAME);
      byte[] decodedBytes = Base64.getDecoder().decode(SSO_PASSWORD);
      auth.setPassword(new String(decodedBytes));

      AuthenticationApi instance = new AuthenticationApi();
      instance.setApiClient(client);

      SessionIdData sessionIdData = instance.login();

      return sessionIdData.getSessionId();
   }

   /**
    * Runs an operation to get all pairings using the provided ApiClient.
    * The method calls the API to retrieve a list of pairings based on optional filtering and pagination parameters.
    *
    * @param client The ApiClient instance used for making API calls.
    * @return A PairingDrResponseList object containing the list of pairings and related information.
    */
   public static PairingDrResponseList runGetAllPairings(ApiClient client) {

      PairingApi instance = new PairingApi();
      instance.setApiClient(client);

      String filterProp = null;
      List<String> filter = null;
      String sortBy = null;
      String orderBy = null;
      String limit = null;
      String offset = null;

      return instance.getPairings(filterProp, filter, sortBy, orderBy, limit, offset);
   }

   /**
    * Runs a remote login operation using the provided ApiClient and pairing ID for authentication.
    * The method sets the remote SSO username and password, then invokes the API to create a remote session.
    *
    * @param client    The ApiClient instance used for making API calls.
    * @param pairingId The pairing ID used for remote authentication.
    */
   public static void runRemoteLogin(ApiClient client, String pairingId) {

      HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("RemoteLoginBasicAuth");
      auth.setUsername(REMOTE_SSO_USERNAME);
      byte[] decodedBytes = Base64.getDecoder().decode(REMOTE_SSO_PASSWORD);
      auth.setPassword(new String(decodedBytes));

      PairingApi instance = new PairingApi();
      instance.setApiClient(client);

      instance.createRemoteSession(pairingId);
   }
}