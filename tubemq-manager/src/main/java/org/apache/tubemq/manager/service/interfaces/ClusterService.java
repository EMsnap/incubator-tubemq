/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.manager.service.interfaces;


import java.util.List;
import org.apache.tubemq.manager.controller.cluster.request.AddClusterReq;
import org.apache.tubemq.manager.entry.ClusterEntry;
import org.springframework.stereotype.Component;

@Component
public interface ClusterService {

    /**
     * add cluster and the master node in the cluster
     * @param req
     * @return
     */
    Boolean addClusterAndMasterNode(AddClusterReq req);

    /**
     * delete cluster by id
     * @param clusterId
     */
    void deleteCluster(Integer clusterId);

    /**
     * get one cluster
     * @param clusterId
     * @return
     */
    ClusterEntry getOneCluster(Integer clusterId);

    /**
     * get all clusters
     * @return
     */
    List<ClusterEntry> getAllClusters();
}
