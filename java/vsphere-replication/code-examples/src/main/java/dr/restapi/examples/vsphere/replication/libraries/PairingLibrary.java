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
    * Get information about vSphere Replication servers that are paired.
    * @param pairingId ID of the pairing between this vSphere Replication server and the remote one.
    * @return list of vSphere Replication servers
    * @throws ExamplesExecutionException when get a list of vSphere Replication servers failed
    */
   public List<VrmsInfo> callGetAllVrDetailsInPairing(String pairingId) {
      List<VrmsInfo> vrmsInfos;
      try {
         vrmsInfos = this.pairingApi.getAllVrDetailsInPairing(pairingId)
                                    .getList();

         toSystemOut("Get a list of all paired vSphere Replication servers completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'PairingApi.getAllVrDetailsInPairing' failed.");
      }

      return vrmsInfos;
   }

   /**
    * Get a list of all registered replication servers for given vSphere Replication {@code vrId} in the specified {@code pairing}.
    * @param pairingId ID of the pairing between this vSphere Replication server and the remote one.
    * @param vrId Id of the VRMS
    * @return list of vSphere Replication servers
    * @throws ExamplesExecutionException when get a list of all registered replication servers failed
    */
   public List<ReplicationServerInfo> callGetAllVrDetailsInPairing(String pairingId, String vrId) {
      List<ReplicationServerInfo> replicationServerInfos;
      try {
         replicationServerInfos = this.pairingApi.getAllVrServersInPairing(pairingId,
                                                                           vrId,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null)
                                                 .getList();

         toSystemOut("Get a list of all registered replication servers completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'PairingApi.getAllVrServersInPairing' failed.");
      }

      return replicationServerInfos;
   }

   /**
    * Create a remote session to the remote vSphere Replication Management server.
    * @param pairingId ID of the pairing between this vSphere Replication server and the remote one
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
