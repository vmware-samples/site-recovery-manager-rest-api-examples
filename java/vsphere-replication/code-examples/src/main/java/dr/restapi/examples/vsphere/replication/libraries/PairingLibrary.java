/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.libraries;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.ApiException;
import dr.restapi.examples.apiclient.api.PairingApi;
import dr.restapi.examples.apiclient.auth.HttpBasicAuth;
import dr.restapi.examples.apiclient.model.*;
import dr.restapi.examples.vsphere.replication.exceptions.ExamplesExecutionException;

import java.util.List;

import static dr.restapi.examples.vsphere.replication.util.ClientUtils.toSystemOut;

public class PairingLibrary {
   private final PairingApi pairingApi;

   /**
    * Constructor.
    * @param apiClient api client
    */
   public PairingLibrary(ApiClient apiClient) {
      this.pairingApi = new PairingApi(apiClient);
   }

   /**
    * Get a list of all existing pairings.
    * @return list of pairings
    * @throws ExamplesExecutionException when get a list of all existing pairings failed
    */
   public List<Pairing> callGetAllPairings() {
      List<Pairing> pairings;
      try {
         pairings = this.pairingApi.getVrPairings(null, null, null, null, null, null)
                                   .getList();

         toSystemOut("Get a list of all existing pairings completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'PairingApi.getVrPairings' failed.");
      }

      return pairings;
   }

   /**
    * Get information about the vSphere Replication Management Servers (VRMS) that are paired.
    * @param pairingId ID of the pairing between this vSphere Replication Management Server and the remote one
    * @return list of vSphere Replication Management Servers
    * @throws ExamplesExecutionException when get a list of vSphere Replication Management Servers failed
    */
   public List<VrmsInfo> callGetAllVrmsDetails(String pairingId) {
      List<VrmsInfo> vrmsInfos;
      try {
         vrmsInfos = this.pairingApi.getAllVrDetailsInPairing(pairingId)
                                    .getList();

         toSystemOut("Get a list of all paired vSphere Replication Management Servers completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'PairingApi.getAllVrDetailsInPairing' failed.");
      }

      return vrmsInfos;
   }

   /**
    * Get a list of all registered vSphere Replication Servers (VRS)
    * for given vSphere Replication Management Server (VRMS) with ID {@code vrmsId} in the specified {@code pairing}.
    * @param pairingId ID of the pairing between this vSphere Replication Management Server and the remote one
    * @param vrmsId ID of the VRMS
    * @return list of vSphere Replication Servers
    * @throws ExamplesExecutionException when get a list of all registered vSphere Replication Servers failed
    */
   public List<ReplicationServerInfo> callGetAllVrsDetails(String pairingId, String vrmsId) {
      List<ReplicationServerInfo> replicationServerInfos;
      try {
         replicationServerInfos = this.pairingApi.getAllVrServersInPairing(pairingId,
                                                                           vrmsId,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null)
                                                 .getList();

         toSystemOut("Get a list of all registered vSphere Replication Servers completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'PairingApi.getAllVrServersInPairing' failed.");
      }

      return replicationServerInfos;
   }

   /**
    * Create a remote session to the remote vSphere Replication Management Server (VRMS).
    * @param pairingId ID of the pairing between this vSphere Replication Management Server and the remote one
    * @throws ExamplesExecutionException when remote login failed
    */
   public void callRemoteLogin(String pairingId, String username, String password) {
      HttpBasicAuth basicAuth =
            (HttpBasicAuth) this.pairingApi.getApiClient().getAuthentication(Constants.REMOTE_LOGIN_BASIC_AUTH);
      basicAuth.setUsername(username);
      basicAuth.setPassword(password);

      try {
         this.pairingApi.createRemoteSession(pairingId);

         toSystemOut("Remote session successfully created.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'PairingApi.createRemoteSession' failed.");
      }
   }
}
