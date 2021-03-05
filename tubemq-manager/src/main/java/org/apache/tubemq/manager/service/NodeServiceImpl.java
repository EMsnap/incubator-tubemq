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

package org.apache.tubemq.manager.service;


import static org.apache.tubemq.manager.service.TubeMQHttpConst.ADD_TUBE_TOPIC;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.BROKER_RUN_STATUS;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.NO_SUCH_CLUSTER;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.RELOAD_BROKER;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.SCHEMA;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.SUCCESS_CODE;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tubemq.manager.controller.TubeMQResult;
import org.apache.tubemq.manager.controller.node.request.AddTopicReq;
import org.apache.tubemq.manager.controller.node.request.BatchAddTopicReq;
import org.apache.tubemq.manager.controller.node.request.CloneBrokersReq;
import org.apache.tubemq.manager.controller.node.request.CloneTopicReq;
import org.apache.tubemq.manager.entry.MasterEntry;
import org.apache.tubemq.manager.repository.MasterRepository;
import org.apache.tubemq.manager.service.interfaces.MasterService;
import org.apache.tubemq.manager.service.interfaces.NodeService;
import org.apache.tubemq.manager.service.interfaces.TopicService;
import org.apache.tubemq.manager.service.tube.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * node service to query broker/master/standby status of tube cluster.
 */
@Slf4j
@Component
public class NodeServiceImpl implements NodeService {

    private final CloseableHttpClient httpclient = HttpClients.createDefault();
    private final Gson gson = new Gson();

    @Value("${manager.max.configurable.broker.size:50}")
    private int maxConfigurableBrokerSize;

    @Value("${manager.max.retry.adding.topic:10}")
    private int maxRetryAddingTopic;

    private final TopicBackendWorker worker;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private TopicService topicService;

    @Autowired
    private MasterService masterService;

    public NodeServiceImpl(TopicBackendWorker worker) {
        this.worker = worker;
    }

    /**
     * request node status via http.
     *
     * @param masterEntry - node entry
     * @return
     * @throws IOException
     */
    private TubeHttpBrokerInfoList requestClusterNodeStatus(MasterEntry masterEntry) {
        String url = SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort() + BROKER_RUN_STATUS;
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            TubeHttpBrokerInfoList brokerInfoList =
                    gson.fromJson(new InputStreamReader(response.getEntity().getContent()),
                            TubeHttpBrokerInfoList.class);
            // request return normal.
            if (brokerInfoList.getCode() == SUCCESS_CODE) {
                // divide by state.
                brokerInfoList.divideBrokerListByState();
                return brokerInfoList;
            }
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status", ex);
        }
        return null;
    }















    private boolean configBrokersForTopics(MasterEntry masterEntry,
            Set<String> topics, List<Integer> brokerList, int maxBrokers) {
        List<Integer> finalBrokerList = brokerList.subList(0, maxBrokers);
        String brokerStr = StringUtils.join(finalBrokerList, ",");
        String topicStr = StringUtils.join(topics, ",");
        String url = SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + ADD_TUBE_TOPIC  + "&topicName=" + topicStr + "&brokerId=" + brokerStr;
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            TubeHttpResponse result =
                    gson.fromJson(new InputStreamReader(response.getEntity().getContent()),
                            TubeHttpResponse.class);
            return result.getCode() == SUCCESS_CODE && result.getErrCode() == SUCCESS_CODE;
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status", ex);
        }
        return false;
    }

    /**
     * handle result, if success, complete it,
     * if not success, add back to queue without exceeding max retry,
     * otherwise complete it with exception.
     *
     * @param isSuccess
     * @param topics
     * @param pendingTopic
     */
    private void handleAddingResult(boolean isSuccess, Set<String> topics,
            Map<String, TopicFuture> pendingTopic) {
        for (String topic : topics) {
            TopicFuture future = pendingTopic.get(topic);
            if (future != null) {
                if (isSuccess) {
                    future.complete();
                } else {
                    future.increaseRetryTime();
                    if (future.getRetryTime() > maxRetryAddingTopic) {
                        future.completeExceptional();
                    } else {
                        // add back to queue.
                        worker.addTopicFuture(future);
                    }
                }
            }
        }
    }


    /**
     * Adding topic is an async operation, so this method should
     * 1. check whether pendingTopic contains topic that has failed/succeeded to be added.
     * 2. async add topic to tubemq cluster
     *
     * @param brokerInfoList - broker list
     * @param pendingTopic - topicMap
     */
    private void handleAddingTopic(MasterEntry masterEntry,
            TubeHttpBrokerInfoList brokerInfoList,
            Map<String, TopicFuture> pendingTopic) {
        // 1. check tubemq cluster by topic name, remove pending topic if has added.
        Set<String> brandNewTopics = new HashSet<>();
        for (String topic : pendingTopic.keySet()) {
            TubeHttpTopicInfoList topicInfoList = topicService.requestTopicConfigInfo(masterEntry, topic);
            if (topicInfoList != null) {
                // get broker list by topic request
                List<Integer> topicBrokerList = topicInfoList.getTopicBrokerIdList();
                if (topicBrokerList.isEmpty()) {
                    brandNewTopics.add(topic);
                } else {
                    // remove brokers which have been added.
                    List<Integer> configurableBrokerIdList =
                            brokerInfoList.getConfigurableBrokerIdList();
                    configurableBrokerIdList.removeAll(topicBrokerList);
                    // add topic to satisfy max broker number.
                    Set<String> singleTopic = new HashSet<>();
                    singleTopic.add(topic);
                    int maxBrokers = maxConfigurableBrokerSize - topicBrokerList.size();
                    boolean isSuccess = configBrokersForTopics(masterEntry, singleTopic,
                            configurableBrokerIdList, maxBrokers);
                    handleAddingResult(isSuccess, singleTopic, pendingTopic);
                }
            }
        }
        // 2. add new topics to cluster
        List<Integer> configurableBrokerIdList = brokerInfoList.getConfigurableBrokerIdList();
        int maxBrokers = Math.min(maxConfigurableBrokerSize, configurableBrokerIdList.size());
        boolean isSuccess = configBrokersForTopics(masterEntry, brandNewTopics,
                configurableBrokerIdList, maxBrokers);
        handleAddingResult(isSuccess, brandNewTopics, pendingTopic);
    }

    /**
     * reload broker list, cannot exceed maxConfigurableBrokerSize each time.
     *
     * @param masterEntry
     * @param needReloadList
     */
    private void handleReloadBroker(MasterEntry masterEntry, List<Integer> needReloadList) {
        // reload without exceed max broker.
        int begin = 0;
        int end = 0;
        do {
            end = Math.min(maxConfigurableBrokerSize + begin, needReloadList.size());
            List<Integer> brokerIdList = needReloadList.subList(begin, end);
            String brokerStr = StringUtils.join(brokerIdList, ",");
            String url = SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                    + RELOAD_BROKER + "&brokerId=" + brokerStr;
            HttpGet httpget = new HttpGet(url);
            try (CloseableHttpResponse response = httpclient.execute(httpget)) {
                TubeHttpResponse result =
                        gson.fromJson(new InputStreamReader(response.getEntity().getContent()),
                                TubeHttpResponse.class);
                if (result.getErrCode() == SUCCESS_CODE && result.getCode() == SUCCESS_CODE) {
                    log.info("reload tube broker cgi: " +
                            url + " ; return value : " + result.getCode());
                }
            } catch (Exception ex) {
                log.error("exception caught while requesting broker status", ex);
            }
            begin = end;
        } while (end >= needReloadList.size());
    }


    @Override
    public TubeMQResult cloneBrokersWithTopic(CloneBrokersReq req) throws Exception {
        return null;
    }

    @Override
    public TubeMQResult addTopicsToBrokers(MasterEntry masterEntry, List<Integer> brokerIds,
        List<AddTopicReq> addTopicReqs) {
        return null;
    }

    @Override
    public TubeMQResult addTopicToBrokers(AddTopicReq req, MasterEntry masterEntry)
        throws Exception {
        return null;
    }

    /**
     * update broker status
     */
    @Override
    public void updateBrokerStatus(int clusterId, Map<String, TopicFuture> pendingTopic) {
        MasterEntry masterEntry = masterRepository.getMasterEntryByClusterIdEquals(clusterId);
        if (masterEntry != null) {
            try {
                TubeHttpBrokerInfoList brokerInfoList = requestClusterNodeStatus(masterEntry);
                if (brokerInfoList != null) {
                    handleAddingTopic(masterEntry, brokerInfoList, pendingTopic);
                }

                // refresh broker list
                brokerInfoList = requestClusterNodeStatus(masterEntry);
                if (brokerInfoList != null) {
                    handleReloadBroker(masterEntry, brokerInfoList.getNeedReloadList());
                }

            } catch (Exception ex) {
                log.error("exception caught while requesting broker status", ex);
            }
        } else {
            log.error("cannot get master ip by clusterId {}, please check it", clusterId);
        }
    }

    @Override
    public void close() throws IOException {
        httpclient.close();
    }



    /**
     * given one topic, copy its config and clone to brokers
     * if no broker is is provided, topics will be cloned to all brokers in cluster
     * @param req
     * @return
     * @throws Exception
     */
    @Override
    public TubeMQResult cloneTopicToBrokers(CloneTopicReq req) throws Exception {

        MasterEntry master = masterService.getMasterNode(req.getClusterId());
        if (master == null) {
            return TubeMQResult.errorResult(NO_SUCH_CLUSTER);
        }
        // 1 query topic config
        TubeHttpTopicInfoList topicInfoList = topicService.requestTopicConfigInfo(master, req.getSourceTopicName());

        if (topicInfoList == null) {
            return TubeMQResult.errorResult("no such topic");
        }

        // 2 if there's no specific broker ids then clone to all of the brokers
        List<Integer> brokerId = req.getBrokerId();

        if (CollectionUtils.isEmpty(brokerId)) {
            TubeHttpBrokerInfoList brokerInfoList = requestClusterNodeStatus(master);
            if (brokerInfoList != null) {
                brokerId = brokerInfoList.getConfigurableBrokerIdList();
            }
        }

        // 3 generate add topic req
        AddTopicReq addTopicReq = topicInfoList.getAddTopicReq(brokerId,
            req.getTargetTopicName(), req.getConfModAuthToken());

        // 4 send to master
        return addTopicToBrokers(addTopicReq, master);

    }



    /**
     * add topic to brokers
     * @param req
     * @return
     */
    @Override
    public TubeMQResult batchAddTopic(BatchAddTopicReq req) {
        MasterEntry masterEntry = masterService.getMasterNode(req.getClusterId());
        if (masterEntry == null) {
            return TubeMQResult.errorResult(NO_SUCH_CLUSTER);
        }
        return addTopicsToBrokers(masterEntry, req.getBrokerIds(), req.getAddTopicReqs());
    }


    @Override
    public boolean addNode(MasterEntry masterEntry) {
        try {
            masterRepository.saveAndFlush(masterEntry);
        } catch (Exception e) {
            log.error("create node error with exception", e);
            return false;
        }
        return true;
    }
}
