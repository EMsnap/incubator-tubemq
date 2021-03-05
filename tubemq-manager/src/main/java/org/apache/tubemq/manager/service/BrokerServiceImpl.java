/*
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

import static java.lang.Math.abs;
import static org.apache.tubemq.manager.controller.node.request.AddBrokersReq.getBatchAddBrokersReq;
import static org.apache.tubemq.manager.service.MasterServiceImpl.TUBE_REQUEST_PATH;
import static org.apache.tubemq.manager.service.TubeMQErrorConst.MYSQL_ERROR;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.DEFAULT_REGION;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.NO_SUCH_CLUSTER;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.SCHEMA;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.SUCCESS_CODE;
import static org.apache.tubemq.manager.utils.ConvertUtils.convertReqToQueryStr;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tubemq.manager.controller.TubeMQResult;
import org.apache.tubemq.manager.controller.node.request.AddBrokersReq;
import org.apache.tubemq.manager.controller.node.request.AddTopicReq;
import org.apache.tubemq.manager.controller.node.request.CloneBrokersReq;
import org.apache.tubemq.manager.controller.node.request.QueryBrokerCfgReq;
import org.apache.tubemq.manager.entry.BrokerEntry;
import org.apache.tubemq.manager.entry.ClusterEntry;
import org.apache.tubemq.manager.entry.MasterEntry;
import org.apache.tubemq.manager.repository.BrokerRepository;
import org.apache.tubemq.manager.repository.MasterRepository;
import org.apache.tubemq.manager.service.interfaces.BrokerService;
import org.apache.tubemq.manager.service.interfaces.ClusterService;
import org.apache.tubemq.manager.service.interfaces.MasterService;
import org.apache.tubemq.manager.service.interfaces.TopicService;
import org.apache.tubemq.manager.service.tube.IpIdRelation;
import org.apache.tubemq.manager.service.tube.OpBrokerResult;
import org.apache.tubemq.manager.service.tube.BrokerConf;
import org.apache.tubemq.manager.service.tube.BrokerStatusInfo;
import org.apache.tubemq.manager.utils.ValidateUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sun.reflect.annotation.ExceptionProxy;

@Component
@Slf4j
public class BrokerServiceImpl implements BrokerService {

    public static final String DUPLICATE_RESOURCE = "duplicate resource";
    @Autowired
    BrokerRepository brokerRepository;

    @Autowired
    ClusterService clusterService;

    @Autowired
    MasterService masterService;

    @Autowired
    MasterRepository masterRepository;


    @Autowired
    TopicService topicService;



    private final CloseableHttpClient httpclient = HttpClients.createDefault();

    private static Gson gson = new Gson();

    @Override
    public TubeMQResult createBroker(long clusterId, BrokerEntry brokerEntry) {

        return null;
    }

    @Override
    public void resetBrokerRegions(long regionId, long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
            .findBrokerEntriesByRegionIdEqualsAndClusterIdEquals(regionId, clusterId);
        for (BrokerEntry brokerEntry : brokerEntries) {
            brokerEntry.setRegionId(DEFAULT_REGION);
            brokerRepository.save(brokerEntry);
        }
    }

    @Override
    public void updateBrokersRegion(List<Long> brokerIdList, Long regionId, Long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository.
            findBrokerEntryByBrokerIdInAndClusterIdEquals(brokerIdList, clusterId);
        for (BrokerEntry brokerEntry : brokerEntries) {
            brokerEntry.setRegionId(regionId);
            brokerRepository.save(brokerEntry);
        }
    }

    @Override
    public boolean checkIfBrokersAllExsit(List<Long> brokerIdList, long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
            .findBrokerEntryByBrokerIdInAndClusterIdEquals(brokerIdList, clusterId);
        List<Long> regionBrokerIdList = brokerEntries.stream().map(BrokerEntry::getBrokerId).collect(
            Collectors.toList());
        return regionBrokerIdList.containsAll(brokerIdList);
    }

    @Override
    public List<Long> getBrokerIdListInRegion(long regionId, long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
            .findBrokerEntriesByRegionIdEqualsAndClusterIdEquals(regionId, clusterId);
        List<Long> regionBrokerIdList = brokerEntries.stream().map(BrokerEntry::getBrokerId).collect(
            Collectors.toList());
        return regionBrokerIdList;
    }

    /**
     * clone source broker to generate brokers with the same config and copy the topics in it.
     * @param req
     * @return
     * @throws Exception
     */
    @Override
    public TubeMQResult cloneBrokersWithTopic(CloneBrokersReq req) throws Exception {

        Long clusterId = req.getClusterId();
        // 1. query source broker config
        QueryBrokerCfgReq queryReq = QueryBrokerCfgReq.getReq(req.getSourceBrokerId());
        MasterEntry masterEntry = masterRepository.getMasterEntryByClusterIdEquals(
            clusterId);
        BrokerStatusInfo brokerStatusInfo = getBrokerStatusInfo(queryReq, masterEntry);

        // 2. use source broker config to clone brokers
        BrokerConf sourceBrokerConf = brokerStatusInfo.getData().get(0);
        AddBrokersReq addBrokersReq = getBatchAddBrokersReq(req.getTargetIps(), clusterId, sourceBrokerConf);

        // 3. request master, return broker ids generated by master
        OpBrokerResult addBrokerResult = addBrokersToClusterWithId(addBrokersReq, masterEntry);

        // might have duplicate brokers
        if (addBrokerResult.getErrCode() != SUCCESS_CODE) {
            return TubeMQResult.errorResult(addBrokerResult.getErrMsg());
        }
        List<Long> brokerIds = addBrokerResult.getBrokerIds();
        List<AddTopicReq> addTopicReqs = req.getAddTopicReqs();

        // 4. add topic to brokers
        return topicService.addTopicsToBrokers(masterEntry, brokerIds, addTopicReqs);
    }


    private OpBrokerResult batchAddBrokersToMaster(long clusterId, List<BrokerEntry> brokerEntries)
    {
        MasterEntry masterEntry = masterService.getMasterNode(clusterId);
        if (ValidateUtils.isNull(masterEntry)) {
            return OpBrokerResult.errorResult(NO_SUCH_CLUSTER);
        }
        AddBrokersReq req = AddBrokersReq.getBatchAddBrokerReq(clusterId, brokerEntries);
        return addBrokersToClusterWithId(req, masterEntry);
    }

    /**
     * add brokers to cluster, this method returns opBrokerResult
     * which contains brokerId and BrokerIp relation
     * @param req
     * @param masterEntry
     * @return
     */
    public OpBrokerResult addBrokersToClusterWithId(AddBrokersReq req, MasterEntry masterEntry) {
        String url = masterService.getQueryUrl(masterEntry, req);
        return masterService.addBrokersToCluster(url);
    }


    private BrokerStatusInfo getBrokerStatusInfo(QueryBrokerCfgReq queryReq, MasterEntry masterEntry) throws Exception {
        String queryUrl = masterService.getQueryUrl(masterEntry, queryReq);
        BrokerStatusInfo brokerStatusInfo = gson.fromJson(masterService.queryMaster(queryUrl),
            BrokerStatusInfo.class);
        return brokerStatusInfo;
    }


    @Override
    public List<BrokerEntry> getOnlineBrokerConfigs(long clusterId) {
        List<BrokerEntry> brokerEntries = brokerRepository
            .findBrokerEntriesByClusterIdEquals(clusterId);
        return brokerEntries;
    }

    @Scheduled(cron ="0/5 * * * * ?")
    public void updateBrokerConfigToMaster() {
        log.info("start to check version between local db and online broker configs");
        List<ClusterEntry> allClusters = clusterService.getAllClusters();
        for (ClusterEntry cluster : allClusters) {
            long clusterId = cluster.getClusterId();
            List<BrokerEntry> localBrokerEntries = brokerRepository
                .findBrokerEntriesByClusterIdEquals(clusterId);
            List<BrokerEntry> onlineBrokerEntries = getOnlineBrokerConfigs(clusterId);
            Map<Long, BrokerEntry> onlineIdBrokerMap = onlineBrokerEntries.stream().
                collect(Collectors.toMap(BrokerEntry::getBrokerId, Function.identity()));
            handleBrokerUpdate(localBrokerEntries, onlineIdBrokerMap, clusterId);
        }
    }

    private void handleBrokerUpdate(List<BrokerEntry> localBrokerEntries,
        Map<Long, BrokerEntry> onlineIdBrokerMap, long clusterId) {
        List<BrokerEntry> needToAddBrokers = new ArrayList<>();
        List<BrokerEntry> needToUpdateBrokers = new ArrayList<>();
        localBrokerEntries.forEach(
            brokerEntry -> {
                if (ValidateUtils.isNull(brokerEntry.getBrokerId()) ||
                    !onlineIdBrokerMap.containsKey(brokerEntry.getBrokerId())) {
                    // no brokerId means the broker has not been created in master
                    log.info("local broker config cannot be found in online broker config, need to add. "
                        + "local broker {}", gson.toJson(brokerEntry));
                    needToAddBrokers.add(brokerEntry);
                    return;
                }
                BrokerEntry onlineBroker = onlineIdBrokerMap.get(brokerEntry.getBrokerId());
                // check version
                if (onlineBroker.getVersion() < brokerEntry.getVersion()) {
                    log.info("local broker config is newer than online broker config, need to update. "
                        + "local broker {}, online broker {}", gson.toJson(brokerEntry), gson.toJson(onlineBroker));
                    needToUpdateBrokers.add(brokerEntry);
                }
            }
        );
        OpBrokerResult addBrokerResult = batchAddBrokersToMaster(clusterId, needToUpdateBrokers);
        batchUpdateBrokersToMaster(clusterId, needToUpdateBrokers);
    }


    @Override
    public TubeMQResult batchAddNewBrokers(long clusterId, List<BrokerEntry> brokerEntries) {
        try {
            for (BrokerEntry brokerEntry : brokerEntries) {
                brokerRepository.save(brokerEntry);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("save new brokers to db fail, brokers {}", brokerEntries);
            return TubeMQResult.errorResult(DUPLICATE_RESOURCE);
        } catch (Exception e) {
            log.error("save new brokers to db fail, brokers {}", brokerEntries);
            return TubeMQResult.errorResult(MYSQL_ERROR);
        }

        // after add brokers to db, add to master
        OpBrokerResult opBrokerResult = batchAddBrokersToMaster(clusterId, brokerEntries);
        if (opBrokerResult.getErrCode() != SUCCESS_CODE) {
            return TubeMQResult.errorResult(opBrokerResult.getErrMsg());
        }

        // ids is generated in master so id need to be updated in db
        return updateBrokerIds(brokerEntries, opBrokerResult);
    }

    private TubeMQResult updateBrokerIds(List<BrokerEntry> brokerEntries, OpBrokerResult opBrokerResult) {
        List<IpIdRelation> ipIds = opBrokerResult.getData();
        Map<String, BrokerEntry> ipBrokerMap = brokerEntries.stream().collect
            (Collectors.toMap(BrokerEntry::getBrokerIp, a -> a));
        try {
            for (IpIdRelation ipId : ipIds) {
                BrokerEntry brokerEntry = ipBrokerMap.get(ipId.getIp());
                brokerEntry.setBrokerId(ipId.getId());
                brokerRepository.save(brokerEntry);
            }
        } catch (Exception e) {
            log.error("update brokerIds fail with exception", e);
            return TubeMQResult.errorResult(e.getMessage());
        }
        return TubeMQResult.successResult();
    }


    private boolean batchUpdateBrokersToMaster(long clusterId, List<BrokerEntry> brokerEntries) {
        AddBrokersReq req = AddBrokersReq.getBatchAddBrokerReq(clusterId, brokerEntries);
        TubeMQResult result = masterService.baseRequestMaster(req);
        return result.getErrCode() == SUCCESS_CODE;
    }






}
