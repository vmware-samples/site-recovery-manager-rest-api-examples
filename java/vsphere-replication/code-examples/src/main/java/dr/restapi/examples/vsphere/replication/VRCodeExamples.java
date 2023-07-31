/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.model.SessionIdData;
import dr.restapi.examples.apiclient.model.SessionInfo;
import dr.restapi.examples.vsphere.replication.config.Config;
import dr.restapi.examples.vsphere.replication.libraries.AuthenticationLibrary;
import dr.restapi.examples.vsphere.replication.libraries.Constants;
import dr.restapi.examples.vsphere.replication.util.Utils;

public class VRCodeExamples {
   public static void main(String[] args) {
      ApiClient apiClient = Utils.getApiClient();

      VRCodeExamples codeExamples = new VRCodeExamples(apiClient);

      codeExamples.runAuthenticationScenario();
   }

   private final ApiClient apiClient;
   private final AuthenticationLibrary authenticationLibrary;

   private VRCodeExamples(ApiClient apiClient) {
      this.apiClient = apiClient;
      this.authenticationLibrary = new AuthenticationLibrary(apiClient);
   }

   private void runAuthenticationScenario() {
      SessionIdData sessionIdData =
            this.authenticationLibrary.callLogin(Config.get().getPropertyNotEmpty(Constants.Config.SSO_USERNAME),
                                                 Config.get().getPropertyNotEmpty(Constants.Config.SSO_PASSWORD));

      apiClient.addDefaultHeader(Constants.SESSION_HEADER, sessionIdData.getSessionId());

      this.authenticationLibrary.callGetCurrentSession();

      this.authenticationLibrary.callLogout();
   }
}
