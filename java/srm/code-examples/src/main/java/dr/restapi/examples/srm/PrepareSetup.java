package dr.restapi.examples.srm;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.Configuration;
import dr.restapi.examples.apiclient.api.AuthenticationApi;
import dr.restapi.examples.apiclient.api.PairingApi;
import dr.restapi.examples.apiclient.auth.HttpBasicAuth;
import dr.restapi.examples.apiclient.model.PairingDrResponseList;
import dr.restapi.examples.apiclient.model.SessionIdData;

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

   public static ApiClient initApiClient() {

      ApiClient client = Configuration.getDefaultApiClient();
      client.setVerifyingSsl(false);
      client.setBasePath(PATH);
      return client;
   }

   public static String runLogin(ApiClient client) {

      HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("BasicAuth");
      auth.setUsername(SSO_USERNAME);
      auth.setPassword(SSO_PASSWORD);

      AuthenticationApi instance = new AuthenticationApi();
      instance.setApiClient(client);

      SessionIdData sessionIdData = instance.login();

      return sessionIdData.getSessionId();

   }

   public static PairingDrResponseList runGetAllPairings(ApiClient client) {

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

   public static void runRemoteLogin(ApiClient client, String pairingId) {

      HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("RemoteLoginBasicAuth");
      auth.setUsername(REMOTE_SSO_USERNAME);
      auth.setPassword(REMOTE_SSO_PASSWORD);

      PairingApi instance = new PairingApi();
      instance.setApiClient(client);

      instance.createRemoteSession(pairingId);
   }
}
