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


import static org.apache.tubemq.corebase.TBaseConstants.OFFSET_TIME_FORMAT;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.QUERY_GROUP_DETAIL_INFO;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.SCHEMA;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.SUCCESS_CODE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.TOPIC_CONFIG_INFO;
import static org.apache.tubemq.manager.utils.ConvertUtils.convertReqToQueryStr;
import static org.apache.tubemq.manager.utils.ConvertUtils.convertToRebalanceConsumerReq;
import static org.apache.tubemq.manager.service.MasterServiceImpl.TUBE_REQUEST_PATH;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tubemq.manager.controller.TubeMQResult;
import org.apache.tubemq.manager.controller.group.request.DeleteOffsetReq;
import org.apache.tubemq.manager.controller.group.request.QueryOffsetAtTimestampReq;
import org.apache.tubemq.manager.controller.group.request.QueryOffsetReq;
import org.apache.tubemq.manager.controller.group.request.ResetTimeOffsetReq;
import org.apache.tubemq.manager.controller.group.request.SetOffsetReq;
import org.apache.tubemq.manager.controller.group.result.AllBrokersOffsetRes;
import org.apache.tubemq.manager.controller.group.result.AllBrokersOffsetRes.OffsetInfo;
import org.apache.tubemq.manager.controller.group.result.OffsetQueryRes;
import org.apache.tubemq.manager.controller.group.result.OffsetTimeQueryRes;
import org.apache.tubemq.manager.controller.group.result.OffsetTimeQueryRes.GroupOffsetItem;
import org.apache.tubemq.manager.controller.group.result.OffsetTimeQueryRes.GroupOffsetItem.TopicOffsetItem;
import org.apache.tubemq.manager.controller.node.request.CloneOffsetReq;
import org.apache.tubemq.manager.controller.topic.request.RebalanceConsumerReq;
import org.apache.tubemq.manager.controller.topic.request.RebalanceGroupReq;
import org.apache.tubemq.manager.entry.NodeEntry;
import org.apache.tubemq.manager.service.interfaces.MasterService;
import org.apache.tubemq.manager.service.interfaces.TopicService;
import org.apache.tubemq.manager.service.tube.CleanOffsetResult;
import org.apache.tubemq.manager.service.tube.CommonResult;
import org.apache.tubemq.manager.service.tube.RebalanceGroupResult;
import org.apache.tubemq.manager.service.tube.TubeHttpGroupDetailInfo;
import org.apache.tubemq.manager.service.tube.TubeHttpTopicInfoList;
import org.apache.tubemq.manager.service.tube.TubeHttpTopicInfoList.TopicInfoList.TopicInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * node service to query broker/master/standby status of tube cluster.
 */
@Slf4j
@Component
public class TopicServiceImpl implements TopicService {

    public static final String MODIFY_USER = "tubemanager";
    private final CloseableHttpClient httpclient = HttpClients.createDefault();
    private final Gson gson = new Gson();

    @Value("${manager.broker.webPort:8081}")
    private String brokerWebPort;

    @Autowired
    private MasterService masterService;

    @Override
    public TubeHttpGroupDetailInfo requestGroupRunInfo(NodeEntry nodeEntry, String group) {
        String url = SCHEMA + nodeEntry.getIp() + ":" + nodeEntry.getWebPort()
            + QUERY_GROUP_DETAIL_INFO + "&consumeGroup=" + group;
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            TubeHttpGroupDetailInfo groupDetailInfo =
                gson.fromJson(new InputStreamReader(response.getEntity().getContent()),
                    TubeHttpGroupDetailInfo.class);
            if (groupDetailInfo.getErrCode() == 0) {
                return groupDetailInfo;
            }
        } catch (Exception ex) {
            log.error("exception caught while requesting group status", ex);
        }
        return null;
    }


    @Override
    public TubeMQResult cloneOffsetToOtherGroups(CloneOffsetReq req) {

        NodeEntry master = masterService.getMasterNode(req);
        if (master == null) {
            return TubeMQResult.getErrorResult("no such cluster");
        }
        // 1. query the corresponding brokers having given topic
        TubeHttpTopicInfoList topicInfoList = requestTopicConfigInfo(master, req.getTopicName());
        TubeMQResult result = new TubeMQResult();

        if (topicInfoList == null) {
            return result;
        }

        List<TopicInfo> topicInfos = topicInfoList.getTopicInfo();
        // 2. for each broker, request to clone offset
        for (TopicInfo topicInfo : topicInfos) {
            String brokerIp = topicInfo.getBrokerIp();
            String url = SCHEMA + brokerIp + ":" + brokerWebPort
                + "/" + TUBE_REQUEST_PATH + "?" + convertReqToQueryStr(req);
            result = masterService.requestTube(url);
            if (result.getErrCode() != SUCCESS_CODE) {
                return result;
            }
        }

        return result;
    }


    @Override
    public TubeHttpTopicInfoList requestTopicConfigInfo(NodeEntry nodeEntry, String topic) {
        String url = SCHEMA + nodeEntry.getIp() + ":" + nodeEntry.getWebPort()
            + TOPIC_CONFIG_INFO + "&topicName=" + topic;
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            TubeHttpTopicInfoList topicInfoList =
                gson.fromJson(new InputStreamReader(response.getEntity().getContent()),
                    TubeHttpTopicInfoList.class);
            if (topicInfoList.getErrCode() == SUCCESS_CODE) {
                return topicInfoList;
            }
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status", ex);
        }
        return null;
    }


    @Override
    public TubeMQResult rebalanceGroup(RebalanceGroupReq req) {

        NodeEntry master = masterService.getMasterNode(req);
        if (master == null) {
            return TubeMQResult.getErrorResult("no such cluster");
        }

        // 1. get all consumer ids in group
        List<String> consumerIds = Objects
            .requireNonNull(requestGroupRunInfo(master, req.getGroupName())).getConsumerIds();
        RebalanceGroupResult rebalanceGroupResult = new RebalanceGroupResult();

        // 2. rebalance consumers in group
        consumerIds.forEach(consumerId -> {
            RebalanceConsumerReq rebalanceConsumerReq = convertToRebalanceConsumerReq(req,
                consumerId);
            String url = SCHEMA + master.getIp() + ":" + master.getWebPort()
                + "/" + TUBE_REQUEST_PATH + "?" + convertReqToQueryStr(rebalanceConsumerReq);
            TubeMQResult result = masterService.requestTube(url);
            if (result.getErrCode() != 0) {
                rebalanceGroupResult.getFailConsumers().add(consumerId);
            }
            rebalanceGroupResult.getSuccessConsumers().add(consumerId);
        });

        TubeMQResult tubeResult = new TubeMQResult();
        tubeResult.setData(gson.toJson(rebalanceGroupResult));

        return tubeResult;
    }


    @Override
    public TubeMQResult deleteOffset(DeleteOffsetReq req) {

        NodeEntry master = masterService.getMasterNode(req);
        if (master == null) {
            return TubeMQResult.getErrorResult("no such cluster");
        }

        // 1. query the corresponding brokers having given topic
        TubeHttpTopicInfoList topicInfoList = requestTopicConfigInfo(master, req.getTopicName());
        TubeMQResult result = new TubeMQResult();
        CleanOffsetResult cleanOffsetResult = new CleanOffsetResult();
        if (topicInfoList == null) {
            return TubeMQResult.getErrorResult("no such topic");
        }

        List<TopicInfo> topicInfos = topicInfoList.getTopicInfo();
        // 2. for each broker, request to delete offset
        for (TopicInfo topicInfo : topicInfos) {
            String brokerIp = topicInfo.getBrokerIp();
            String url = SCHEMA + brokerIp + ":" + brokerWebPort
                + "/" + TUBE_REQUEST_PATH + "?" + convertReqToQueryStr(req);
            result = masterService.requestTube(url);
            if (result.getErrCode() != SUCCESS_CODE) {
                cleanOffsetResult.getFailBrokers().add(brokerIp);
            } else {
                cleanOffsetResult.getSuccessBrokers().add(brokerIp);
            }
        }

        result.setData(gson.toJson(cleanOffsetResult));

        return result;
    }

    @Override
    public TubeMQResult queryOffset(QueryOffsetReq req) {

        NodeEntry master = masterService.getMasterNode(req);
        if (master == null) {
            return TubeMQResult.getErrorResult("no such cluster");
        }

        // 1. query the corresponding brokers having given topic
        TubeHttpTopicInfoList topicInfoList = requestTopicConfigInfo(master, req.getTopicName());
        TubeMQResult result = new TubeMQResult();
        if (topicInfoList == null) {
            return TubeMQResult.getErrorResult("no such topic");
        }

        List<TopicInfo> topicInfos = topicInfoList.getTopicInfo();

        AllBrokersOffsetRes allBrokersOffsetRes = new AllBrokersOffsetRes();
        List<OffsetInfo> offsetPerBroker = allBrokersOffsetRes.getOffsetPerBroker();

        // 2. for each broker, request to query offset
        for (TopicInfo topicInfo : topicInfos) {
            String brokerIp = topicInfo.getBrokerIp();
            String url = SCHEMA + brokerIp + ":" + brokerWebPort
                + "/" + TUBE_REQUEST_PATH + "?" + convertReqToQueryStr(req);
            OffsetQueryRes res = gson.fromJson(masterService.queryTube(url), OffsetQueryRes.class);
            if (res.getErrCode() != SUCCESS_CODE) {
                return TubeMQResult.getErrorResult("query broker id" + topicInfo.getBrokerId() + " fail");
            }
            generateOffsetInfo(offsetPerBroker, topicInfo, res);
        }

        result.setData(gson.toJson(allBrokersOffsetRes));
        return result;
    }


    @Override
    public TubeMQResult setGroupOffset(SetOffsetReq req) {
        if (ObjectUtils.isEmpty(req.getBrokerIp()) || ObjectUtils.isEmpty(req.getBrokerWebPort())) {
            return new TubeMQResult();
        }
        String url = SCHEMA + req.getBrokerIp() + ":" + req.getBrokerWebPort()
            + "/" + TUBE_REQUEST_PATH + "?" + convertReqToQueryStr(req);
        return masterService.requestTube(url);
    }

    @Override
    public TubeMQResult resetOffsetToTime(ResetTimeOffsetReq req) {
        NodeEntry master = masterService.getMasterNode(req);
        if (master == null) {
            return TubeMQResult.getErrorResult("no such cluster");
        }
        String groupName = req.getGroupName();
        String topicName = req.getTopicName();
        Date resetDate = req.getResetDate();
        SimpleDateFormat f = new SimpleDateFormat(OFFSET_TIME_FORMAT);
        String requestTime = f.format(resetDate);

        // 1. query the corresponding brokers having given topic
        TubeHttpTopicInfoList topicInfoList = requestTopicConfigInfo(master, topicName);
        if (topicInfoList == null) {
            return TubeMQResult.getErrorResult("no such topic");
        }

        List<SetOffsetReq> setOffsetReqs = new ArrayList<>();
        // 2. for each broker, request to query offset at timestamp
        List<TopicInfo> topicInfos = topicInfoList.getTopicInfo();
        for (TopicInfo topicPerBroker : topicInfos) {
            // get all offset at timestamp
            String brokerIp = topicPerBroker.getBrokerIp();
            QueryOffsetAtTimestampReq queryOffsetAtTimestampReq =
                new QueryOffsetAtTimestampReq(brokerWebPort,
                    brokerIp, requestTime);
            List<GroupOffsetItem> groupOffsetItems = queryOffsetAtTimeStamp(queryOffsetAtTimestampReq);
            if (CollectionUtils.isEmpty(groupOffsetItems)) {
                return TubeMQResult.getErrorResult("no such offsets in broker " + topicPerBroker.getBrokerId()
                    + "at time: " + requestTime);
            }
            // filter offsets by request topic and group
            List<TopicOffsetItem> topicOffsetFiltered = filterTopicOffsets(
                groupName, topicName, groupOffsetItems);
            SetOffsetReq setOffsetReq = new SetOffsetReq(groupName, brokerIp, brokerWebPort,
                MODIFY_USER, true, topicOffsetFiltered, req.getUseConsumeOffset());
            setOffsetReqs.add(setOffsetReq);
        }
        
        return handleSetOffset(setOffsetReqs);
    }

    private TubeMQResult handleSetOffset(List<SetOffsetReq> setOffsetReqs) {
        CommonResult commonResult = new CommonResult();

        setOffsetReqs.forEach(setOffsetReq -> {
            TubeMQResult result = setGroupOffset(setOffsetReq);
            if (result.getErrCode() != SUCCESS_CODE) {
                commonResult.getFail().add(setOffsetReq.getBrokerIp());
            } else {
                commonResult.getSuccess().add(setOffsetReq.getBrokerIp());
            }
        });

        TubeMQResult result = new TubeMQResult();
        result.setData(gson.toJson(commonResult));
        return result;
    }


    private List<TopicOffsetItem> filterTopicOffsets(String groupName, String topicName,
        List<GroupOffsetItem> groupOffsetItems) {

        List<TopicOffsetItem> topicOffsetFiltered = new ArrayList<>();

        groupOffsetItems
            .stream().filter(groupOffsetItem -> Objects.equals(groupOffsetItem.getGroupName(), groupName))
            .forEach(
                groupOffsetItem -> {
                    List<TopicOffsetItem> topicOffsetsList = groupOffsetItem.getTopicOffsets().stream().filter(
                        topicOffsetItem -> topicOffsetItem.getTopicName()
                            .equals(topicName)).collect(Collectors.toList());
                    topicOffsetFiltered.addAll(topicOffsetsList);
                }
            );

        return topicOffsetFiltered;
    }

    @Override
    public List<GroupOffsetItem> queryOffsetAtTimeStamp(QueryOffsetAtTimestampReq req) {
        String url = SCHEMA + req.getBrokerIp() + ":" + req.getBrokerWebPort()
            + "/" + TUBE_REQUEST_PATH + "?" + convertReqToQueryStr(req);
        OffsetTimeQueryRes res = gson.fromJson(masterService.queryTube(url), OffsetTimeQueryRes.class);
        return res.getGroupOffsets();
    }


    private void generateOffsetInfo(List<OffsetInfo> offsetPerBroker, TopicInfo topicInfo,
        OffsetQueryRes res) {
        OffsetInfo offsetInfo = new OffsetInfo();
        offsetInfo.setBrokerId(topicInfo.getBrokerId());
        offsetInfo.setOffsetQueryRes(res);
        offsetPerBroker.add(offsetInfo);
    }
}
