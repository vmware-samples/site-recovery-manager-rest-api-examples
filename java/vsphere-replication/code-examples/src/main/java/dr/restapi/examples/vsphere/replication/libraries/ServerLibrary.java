/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.libraries;

import dr.restapi.examples.apiclient.ApiClient;
import dr.restapi.examples.apiclient.ApiException;
import dr.restapi.examples.apiclient.api.ServerApi;
import dr.restapi.examples.apiclient.model.ReplicationServerInfo;
import dr.restapi.examples.vsphere.replication.exceptions.ExamplesExecutionException;

import java.util.List;

import static dr.restapi.examples.vsphere.replication.util.ClientUtils.toSystemOut;

public class ServerLibrary {
   private final ServerApi serverApi;

   /**
    * Constructor.
    * @param apiClient api client
    */
   public ServerLibrary(ApiClient apiClient) {
      this.serverApi = new ServerApi(apiClient);
   }

   /**
    * Get all registered vSphere Replication Servers (VRS).
    * @return all registered vSphere Replication Servers
    * @throws ExamplesExecutionException when get all registered vSphere Replication Servers failed
    */
   public List<ReplicationServerInfo> callGetAllVrServers() {
      List<ReplicationServerInfo> vrs;
      try {
         vrs = this.serverApi.getAllVrServers(null, null, null, null, null, null)
                             .getList();

         toSystemOut("Get all registered vSphere Replication Servers completed.");
      } catch (ApiException ex) {
         throw new ExamplesExecutionException(ex, "Request 'ServerApi.getAllVrServers' failed.");
      }

      return vrs;
   }
}
