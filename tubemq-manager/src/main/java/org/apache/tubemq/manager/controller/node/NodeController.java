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

package org.apache.tubemq.manager.controller.node;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tubemq.manager.controller.node.request.AddBrokersReq;
import org.apache.tubemq.manager.controller.node.request.BrokerSetReadOrWriteReq;
import org.apache.tubemq.manager.controller.node.request.CloneBrokersReq;
import org.apache.tubemq.manager.controller.node.request.DeleteBrokerReq;
import org.apache.tubemq.manager.controller.node.request.OnlineOfflineBrokerReq;
import org.apache.tubemq.manager.controller.node.request.ReloadBrokerReq;
import org.apache.tubemq.manager.repository.NodeRepository;
import org.apache.tubemq.manager.service.NodeService;
import org.apache.tubemq.manager.utils.MasterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.apache.tubemq.manager.controller.TubeMQResult.getErrorResult;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.ADD;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.ADMIN_QUERY_CLUSTER_INFO;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.CLONE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.DELETE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.NO_SUCH_METHOD;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.OFFLINE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.ONLINE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.OP_QUERY;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.RELOAD;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.SET_READ_OR_WRITE;
import static org.apache.tubemq.manager.utils.MasterUtils.*;

@RestController
@RequestMapping(path = "/v1/node")
@Slf4j
public class NodeController {


    private final Gson gson = new Gson();
    private static final CloseableHttpClient httpclient = HttpClients.createDefault();

    @Autowired
    NodeService nodeService;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    MasterUtils masterUtil;

    /**
     * query brokers in certain cluster
     * @param type
     * @param method
     * @param clusterId
     * @return
     */
    @RequestMapping(value = "/query/clusterInfo", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String queryInfo(@RequestParam String type, @RequestParam String method,
            @RequestParam(required = false) Integer clusterId) {
        if (method.equals(ADMIN_QUERY_CLUSTER_INFO) && type.equals(OP_QUERY)) {
            return nodeService.queryClusterInfo(clusterId);
        }
        return gson.toJson(getErrorResult(NO_SUCH_METHOD));
    }


    /**
     * query brokers' run status
     * this method supports batch operation
     */
    @RequestMapping(value = "/broker/status", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String queryBrokerDetail(
            @RequestParam Map<String, String> queryBody) throws Exception {
        String url = masterUtil.getQueryUrl(queryBody);
        return queryMaster(url);
    }


    /**
     * query brokers' configuration
     * this method supports batch operation
     */
    @RequestMapping(value = "/broker/config", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String queryBrokerConfig(
            @RequestParam Map<String, String> queryBody) throws Exception {
        String url = masterUtil.getQueryUrl(queryBody);
        return queryMaster(url);
    }



    /**
     * broker method proxy
     * divides the operation on broker to different method
     */
    @RequestMapping(value = "/broker")
    public @ResponseBody String brokerMethodProxy(
        @RequestParam String method, @RequestBody String req) throws Exception {
        switch (method) {
            case CLONE:
                return nodeService.cloneBrokers(gson.fromJson(req, CloneBrokersReq.class));
            case ADD:
                return nodeService.addBrokers(gson.fromJson(req, AddBrokersReq.class));
            case ONLINE:
            case OFFLINE:
                return gson.toJson(masterUtil.redirectToMasterWithBaseReq(gson.fromJson(req, OnlineOfflineBrokerReq.class)));
            case RELOAD:
                return gson.toJson(masterUtil.redirectToMasterWithBaseReq(gson.fromJson(req, ReloadBrokerReq.class)));
            case DELETE:
                return gson.toJson(masterUtil.redirectToMasterWithBaseReq(gson.fromJson(req, DeleteBrokerReq.class)));
            case SET_READ_OR_WRITE:
                return gson.toJson(masterUtil.redirectToMasterWithBaseReq(gson.fromJson(req, BrokerSetReadOrWriteReq.class)));
            default:
                return "no such method";
        }
    }

}
