/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.web.handler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbBlackGroupEntity;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbConsumeGroupSettingEntity;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbConsumerGroupEntity;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbGroupFilterCondEntity;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbTopicAuthControlEntity;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerBandInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.NodeRebInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WebAdminGroupCtrlHandler extends AbstractWebHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(WebAdminGroupCtrlHandler.class);


    public WebAdminGroupCtrlHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_black_consumer_group_info",
                "adminQueryBlackGroupInfo");
        registerQueryWebMethod("admin_query_allowed_consumer_group_info",
                "adminQueryConsumerGroupInfo");
        registerQueryWebMethod("admin_query_group_filtercond_info",
                "adminQueryGroupFilterCondInfo");
        registerQueryWebMethod("admin_query_consume_group_setting",
                "adminQueryConsumeGroupSetting");
        // register modify method
        registerModifyWebMethod("admin_add_black_consumergroup_info",
                "adminAddBlackGroupInfo");
        registerModifyWebMethod("admin_bath_add_black_consumergroup_info",
                "adminBatchAddBlackGroupInfo");
        registerModifyWebMethod("admin_delete_black_consumergroup_info",
                "adminDeleteBlackGroupInfo");
        registerModifyWebMethod("admin_add_authorized_consumergroup_info",
                "adminAddConsumerGroupInfo");
        registerModifyWebMethod("admin_delete_allowed_consumer_group_info",
                "adminDeleteConsumerGroupInfo");
        registerModifyWebMethod("admin_bath_add_authorized_consumergroup_info",
                "adminBatchAddConsumerGroupInfo");
        registerModifyWebMethod("admin_add_group_filtercond_info",
                "adminAddGroupFilterCondInfo");
        registerModifyWebMethod("admin_bath_add_group_filtercond_info",
                "adminBatchAddGroupFilterCondInfo");
        registerModifyWebMethod("admin_mod_group_filtercond_info",
                "adminModGroupFilterCondInfo");
        registerModifyWebMethod("admin_bath_mod_group_filtercond_info",
                "adminBatchModGroupFilterCondInfo");
        registerModifyWebMethod("admin_del_group_filtercond_info",
                "adminDeleteGroupFilterCondInfo");
        registerModifyWebMethod("admin_add_consume_group_setting",
                "adminAddConsumeGroupSettingInfo");
        registerModifyWebMethod("admin_bath_add_consume_group_setting",
                "adminBatchAddConsumeGroupSetting");
        registerModifyWebMethod("admin_upd_consume_group_setting",
                "adminUpdConsumeGroupSetting");
        registerModifyWebMethod("admin_del_consume_group_setting",
                "adminDeleteConsumeGroupSetting");
        registerModifyWebMethod("admin_rebalance_group_allocate",
                "adminRebalanceGroupAllocateInfo");
    }

    /**
     * Add group filter condition info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminAddGroupFilterCondInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            String topicName =
                    WebParameterUtils.validStringParameter("topicName",
                            req.getParameter("topicName"),
                            TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                            true, "");
            Set<String> configuredTopicSet =
                    brokerConfManager.getTotalConfiguredTopicNames();
            if (!configuredTopicSet.contains(topicName)) {
                throw new Exception(sBuilder.append("Topic: ").append(topicName)
                        .append(" not configure in master's topic configure, please configure first!").toString());
            }
            final int filterCondStatus =
                    WebParameterUtils.validIntDataParameter("condStatus",
                            req.getParameter("condStatus"),
                            false, 0, 0);
            String groupName =
                WebParameterUtils.validGroupParameter("groupName",
                    req.getParameter("groupName"),
                    TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                    true, "");
            final String strNewFilterConds =
                    WebParameterUtils.checkAndGetFilterConds(req.getParameter("filterConds"), true, sBuilder);
            BdbTopicAuthControlEntity topicAuthControlEntity =
                    brokerConfManager.getBdbEnableAuthControlByTopicName(topicName);
            if (topicAuthControlEntity == null) {
                try {
                    brokerConfManager.confSetBdbTopicAuthControl(
                            new BdbTopicAuthControlEntity(topicName,
                                    false, createUser, createDate));
                } catch (Exception ee) {
                    //
                }
            }
            BdbConsumerGroupEntity webConsumerGroupEntity =
                    new BdbConsumerGroupEntity();
            webConsumerGroupEntity.setGroupTopicName(topicName);
            webConsumerGroupEntity.setConsumerGroupName(groupName);
            List<BdbConsumerGroupEntity> resultEntities =
                    brokerConfManager.confGetBdbAllowedConsumerGroupSet(webConsumerGroupEntity);
            if (resultEntities.isEmpty()) {
                try {
                    brokerConfManager.confAddAllowedConsumerGroup(
                            new BdbConsumerGroupEntity(topicName,
                                    groupName, createUser, createDate));
                } catch (Throwable e2) {
                    //
                }
            }
            brokerConfManager.confAddNewGroupFilterCond(
                    new BdbGroupFilterCondEntity(topicName, groupName,
                            filterCondStatus, strNewFilterConds, createUser, createDate));
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Add group filter info in batch
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminBatchAddGroupFilterCondInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            List<Map<String, String>> filterJsonArray =
                    WebParameterUtils.checkAndGetJsonArray("filterCondJsonSet",
                            req.getParameter("filterCondJsonSet"),
                            TBaseConstants.META_VALUE_UNDEFINED, true);
            if ((filterJsonArray == null) || (filterJsonArray.isEmpty())) {
                throw new Exception("Null value of filterCondJsonSet, please set the value first!");
            }
            Set<String> configuredTopicSet = brokerConfManager.getTotalConfiguredTopicNames();
            HashMap<String, BdbGroupFilterCondEntity> inGroupFilterCondEntityMap =
                    new HashMap<>();
            for (int j = 0; j < filterJsonArray.size(); j++) {
                Map<String, String> groupObject = filterJsonArray.get(j);
                try {
                    String groupName =
                        WebParameterUtils.validGroupParameter("groupName",
                            groupObject.get("groupName"),
                            TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                            true, "");
                    String groupTopicName =
                            WebParameterUtils.validStringParameter("topicName",
                                    groupObject.get("topicName"),
                                    TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                                    true, "");
                    if (!configuredTopicSet.contains(groupTopicName)) {
                        throw new Exception(sBuilder.append("Topic ").append(groupTopicName)
                                .append(" not configure in master configure, please configure first!").toString());
                    }
                    int filterCondStatus =
                            WebParameterUtils.validIntDataParameter("condStatus",
                                    groupObject.get("condStatus"),
                                    false, 0, 0);
                    String strNewFilterConds =
                            WebParameterUtils.checkAndGetFilterConds(
                                    (String) groupObject.get("filterConds"),
                                    true, sBuilder);
                    String recordKey = sBuilder.append(groupName)
                            .append("-").append(groupTopicName).toString();
                    sBuilder.delete(0, sBuilder.length());
                    inGroupFilterCondEntityMap.put(recordKey,
                            new BdbGroupFilterCondEntity(groupTopicName, groupName,
                                    filterCondStatus, strNewFilterConds,
                                    createUser, createDate));
                } catch (Exception ee) {
                    sBuilder.delete(0, sBuilder.length());
                    throw new Exception(sBuilder.append("Process data exception, data is :")
                            .append(groupObject.toString()).append(", exception is : ")
                            .append(ee.getMessage()).toString());
                }
            }
            if (inGroupFilterCondEntityMap.isEmpty()) {
                throw new Exception("Not found record in filterCondJsonSet parameter");
            }
            for (BdbGroupFilterCondEntity entity : inGroupFilterCondEntityMap.values()) {
                BdbTopicAuthControlEntity topicAuthControlEntity =
                        brokerConfManager.getBdbEnableAuthControlByTopicName(entity.getTopicName());
                if (topicAuthControlEntity == null) {
                    try {
                        brokerConfManager.confSetBdbTopicAuthControl(
                                new BdbTopicAuthControlEntity(entity.getTopicName(),
                                        false, createUser, createDate));
                    } catch (Exception ee) {
                        //
                    }
                }
                BdbConsumerGroupEntity groupEntity =
                        new BdbConsumerGroupEntity();
                groupEntity.setGroupTopicName(entity.getTopicName());
                groupEntity.setConsumerGroupName(entity.getConsumerGroupName());
                List<BdbConsumerGroupEntity> webConsumerGroupEntities =
                        brokerConfManager.confGetBdbAllowedConsumerGroupSet(groupEntity);
                if (webConsumerGroupEntities.isEmpty()) {
                    try {
                        brokerConfManager.confAddAllowedConsumerGroup(
                                new BdbConsumerGroupEntity(entity.getTopicName(),
                                        entity.getConsumerGroupName(), createUser, createDate));
                    } catch (Throwable e2) {
                        //
                    }
                }
                brokerConfManager.confAddNewGroupFilterCond(entity);
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Modify group filter condition info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminModGroupFilterCondInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String modifyUser =
                    WebParameterUtils.validStringParameter("modifyUser",
                            req.getParameter("modifyUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date modifyDate =
                    WebParameterUtils.validDateParameter("modifyDate",
                            req.getParameter("modifyDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            String topicName =
                    WebParameterUtils.validStringParameter("topicName",
                            req.getParameter("topicName"),
                            TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                            true, "");
            Set<String> configuredTopicSet =
                    brokerConfManager.getTotalConfiguredTopicNames();
            if (!configuredTopicSet.contains(topicName)) {
                throw new Exception(sBuilder.append("Topic: ").append(topicName)
                        .append(" not configure in master's topic configure, please configure first!").toString());
            }
            String groupName =
                WebParameterUtils.validGroupParameter("groupName",
                    req.getParameter("groupName"),
                    TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                    true, "");
            BdbGroupFilterCondEntity curFilterCondEntity =
                    brokerConfManager.getBdbAllowedGroupFilterConds(topicName, groupName);
            if (curFilterCondEntity == null) {
                throw new Exception(sBuilder
                        .append("Not found group filter condition configure record by topicName=")
                        .append(topicName).append(", groupName=")
                        .append(groupName).toString());
            }
            boolean foundChange = false;
            BdbGroupFilterCondEntity newFilterCondEntity =
                    new BdbGroupFilterCondEntity(curFilterCondEntity.getTopicName(),
                            curFilterCondEntity.getConsumerGroupName(),
                            curFilterCondEntity.getControlStatus(),
                            curFilterCondEntity.getAttributes(),
                            modifyUser, modifyDate);
            int filterCondStatus =
                    WebParameterUtils.validIntDataParameter("condStatus",
                            req.getParameter("condStatus"),
                            false,
                            TBaseConstants.META_VALUE_UNDEFINED,
                            0);
            if (filterCondStatus != TBaseConstants.META_VALUE_UNDEFINED
                    && filterCondStatus != curFilterCondEntity.getControlStatus()) {
                foundChange = true;
                newFilterCondEntity.setControlStatus(filterCondStatus);
            }
            String strNewFilterConds =
                    WebParameterUtils.checkAndGetFilterConds(req.getParameter("filterConds"), false, sBuilder);
            if (TStringUtils.isNotBlank(strNewFilterConds)) {
                if (!curFilterCondEntity.getAttributes().equals(strNewFilterConds)) {
                    foundChange = true;
                    newFilterCondEntity.setAttributes(strNewFilterConds);
                }
            }
            if (foundChange) {
                try {
                    brokerConfManager.confModGroupFilterCondConfig(newFilterCondEntity);
                } catch (Throwable ee) {
                    //
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Modify group filter condition info in batch
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminBatchModGroupFilterCondInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String modifyUser =
                    WebParameterUtils.validStringParameter("modifyUser",
                            req.getParameter("modifyUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date modifyDate =
                    WebParameterUtils.validDateParameter("modifyDate",
                            req.getParameter("modifyDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            List<Map<String, String>> jsonArray =
                    WebParameterUtils.checkAndGetJsonArray("filterCondJsonSet",
                            req.getParameter("filterCondJsonSet"),
                            TBaseConstants.META_VALUE_UNDEFINED, true);
            if ((jsonArray == null) || (jsonArray.isEmpty())) {
                throw new Exception("Null value of filterCondJsonSet, please set the value first!");
            }
            Set<String> batchRecords = new HashSet<>();
            List<BdbGroupFilterCondEntity> modifyFilterCondEntities = new ArrayList<>();
            for (int j = 0; j < jsonArray.size(); j++) {
                Map<String, String> groupObject = jsonArray.get(j);
                try {
                    String groupName =
                        WebParameterUtils.validGroupParameter("groupName",
                            groupObject.get("groupName"),
                            TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                            true, "");
                    String topicName =
                            WebParameterUtils.validStringParameter("topicName",
                                    groupObject.get("topicName"),
                                    TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                                    true, "");
                    BdbGroupFilterCondEntity curFilterCondEntity =
                            brokerConfManager.getBdbAllowedGroupFilterConds(topicName, groupName);
                    if (curFilterCondEntity == null) {
                        throw new Exception(sBuilder
                                .append("Not found group filter condition configure record by topicName=")
                                .append(topicName)
                                .append(", groupName=")
                                .append(groupName).toString());
                    }
                    String recordKey = sBuilder.append(groupName)
                            .append("-").append(topicName).toString();
                    sBuilder.delete(0, sBuilder.length());
                    if (batchRecords.contains(recordKey)) {
                        continue;
                    }
                    boolean foundChange = false;
                    BdbGroupFilterCondEntity newFilterCondEntity =
                            new BdbGroupFilterCondEntity(curFilterCondEntity.getTopicName(),
                                    curFilterCondEntity.getConsumerGroupName(),
                                    curFilterCondEntity.getControlStatus(),
                                    curFilterCondEntity.getAttributes(),
                                    modifyUser, modifyDate);
                    int filterCondStatus =
                            WebParameterUtils.validIntDataParameter("condStatus",
                                    groupObject.get("condStatus"),
                                    false, TBaseConstants.META_VALUE_UNDEFINED,
                                    0);
                    if (filterCondStatus != TBaseConstants.META_VALUE_UNDEFINED
                            && filterCondStatus != curFilterCondEntity.getControlStatus()) {
                        foundChange = true;
                        newFilterCondEntity.setControlStatus(filterCondStatus);
                    }
                    String strNewFilterConds =
                            WebParameterUtils.checkAndGetFilterConds(
                                    (String) groupObject.get("filterConds"),
                                    false, sBuilder);
                    if (TStringUtils.isNotBlank(strNewFilterConds)) {
                        if (!curFilterCondEntity.getAttributes().equals(strNewFilterConds)) {
                            foundChange = true;
                            newFilterCondEntity.setAttributes(strNewFilterConds);
                        }
                    }
                    if (!foundChange) {
                        continue;
                    }
                    batchRecords.add(recordKey);
                    modifyFilterCondEntities.add(newFilterCondEntity);
                } catch (Exception ee) {
                    sBuilder.delete(0, sBuilder.length());
                    throw new Exception(sBuilder.append("Process data exception, data is :")
                            .append(groupObject.toString())
                            .append(", exception is : ")
                            .append(ee.getMessage()).toString());
                }
            }
            for (BdbGroupFilterCondEntity tmpFilterCondEntity : modifyFilterCondEntities) {
                try {
                    brokerConfManager.confModGroupFilterCondConfig(tmpFilterCondEntity);
                } catch (Throwable ee) {
                    //
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Delete group filter condition info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminDeleteGroupFilterCondInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            Set<String> batchOpTopicNames =
                    WebParameterUtils.getBatchTopicNames(req.getParameter("topicName"),
                            true, false, null, sBuilder);
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            false, false, null, sBuilder);
            if (batchOpGroupNames.isEmpty()) {
                for (String tmpTopicName : batchOpTopicNames) {
                    BdbGroupFilterCondEntity webFilterCondEntity =
                            new BdbGroupFilterCondEntity();
                    webFilterCondEntity.setTopicName(tmpTopicName);
                    List<BdbGroupFilterCondEntity> webFilterCondEntities =
                            brokerConfManager.confGetBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                    if (!webFilterCondEntities.isEmpty()) {
                        webFilterCondEntity.setCreateUser("System");
                        brokerConfManager.confDelBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                    }
                }
            } else {
                for (String tmpTopicName : batchOpTopicNames) {
                    for (String tmpGroupName : batchOpGroupNames) {
                        BdbGroupFilterCondEntity webFilterCondEntity =
                                new BdbGroupFilterCondEntity();
                        webFilterCondEntity.setTopicName(tmpTopicName);
                        webFilterCondEntity.setConsumerGroupName(tmpGroupName);
                        List<BdbGroupFilterCondEntity> webFilterCondEntities =
                                brokerConfManager.confGetBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                        if (!webFilterCondEntities.isEmpty()) {
                            webFilterCondEntity.setCreateUser("System");
                            brokerConfManager.confDelBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                        }
                    }
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Re-balance group allocation info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminRebalanceGroupAllocateInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String groupName =
                WebParameterUtils.validGroupParameter("groupName",
                    req.getParameter("groupName"),
                    TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                    true, "");
            String modifyUser =
                    WebParameterUtils.validStringParameter("modifyUser",
                            req.getParameter("modifyUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date modifyDate =
                    WebParameterUtils.validDateParameter("modifyDate",
                            req.getParameter("modifyDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            int reJoinWait =
                    WebParameterUtils.validIntDataParameter("reJoinWait",
                            req.getParameter("reJoinWait"),
                            false, 0, 0);
            Set<String> batchOpConsumerIds = new HashSet<>();
            String inputConsumerId = req.getParameter("consumerId");
            if (TStringUtils.isNotBlank(inputConsumerId)) {
                inputConsumerId = inputConsumerId.trim();
                String[] strInputConsumerIds =
                        inputConsumerId.split(TokenConstants.ARRAY_SEP);
                for (int i = 0; i < strInputConsumerIds.length; i++) {
                    if (TStringUtils.isBlank(strInputConsumerIds[i])) {
                        continue;
                    }
                    String consumerId = strInputConsumerIds[i].trim();
                    if (consumerId.length() > TServerConstants.CFG_CONSUMER_CLIENTID_MAX_LENGTH) {
                        throw new Exception(sBuilder.append("The max length of ")
                                .append(consumerId)
                                .append(" in consumerId parameter over ")
                                .append(TServerConstants.CFG_CONSUMER_CLIENTID_MAX_LENGTH)
                                .append(" characters").toString());
                    }
                    if (!consumerId.matches(TBaseConstants.META_TMP_CONSUMERID_VALUE)) {
                        throw new Exception(sBuilder.append("The value of ").append(consumerId)
                                .append("in consumerId parameter must begin with a letter, ")
                                .append("can only contain characters,numbers,dot,scores,and underscores").toString());
                    }
                    if (!batchOpConsumerIds.contains(consumerId)) {
                        batchOpConsumerIds.add(consumerId);
                    }
                }
            }
            if (batchOpConsumerIds.isEmpty()) {
                throw new Exception("Null value of required consumerId parameter");
            }
            ConsumerInfoHolder consumerInfoHolder =
                    master.getConsumerHolder();
            ConsumerBandInfo consumerBandInfo =
                    consumerInfoHolder.getConsumerBandInfo(groupName);
            if (consumerBandInfo == null) {
                return sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"The group(")
                        .append(groupName).append(") not online! \"}");
            } else {
                Map<String, NodeRebInfo> nodeRebInfoMap = consumerBandInfo.getRebalanceMap();
                for (String consumerId : batchOpConsumerIds) {
                    if (nodeRebInfoMap.containsKey(consumerId)) {
                        return sBuilder
                                .append("{\"result\":false,\"errCode\":400,\"errMsg\":\"Duplicated set for consumerId(")
                                .append(consumerId).append(") in group(")
                                .append(groupName).append(")! \"}");
                    }
                }
                logger.info(sBuilder.append("[Re-balance] Add rebalance consumer: group=")
                        .append(groupName).append(", consumerIds=")
                        .append(batchOpConsumerIds.toString())
                        .append(", reJoinWait=").append(reJoinWait)
                        .append(", creator=").append(modifyUser).toString());
                sBuilder.delete(0, sBuilder.length());
                consumerInfoHolder.addRebConsumerInfo(groupName, batchOpConsumerIds, reJoinWait);
                sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
            }
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Query group filter condition info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminQueryGroupFilterCondInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        BdbGroupFilterCondEntity webGroupFilterCondEntity =
                new BdbGroupFilterCondEntity();
        try {
            webGroupFilterCondEntity
                    .setTopicName(WebParameterUtils.validStringParameter("topicName",
                            req.getParameter("topicName"),
                            TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                            false, null));
            webGroupFilterCondEntity
                .setConsumerGroupName(WebParameterUtils.validGroupParameter("groupName",
                    req.getParameter("groupName"),
                    TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                    false, null));
            webGroupFilterCondEntity
                    .setCreateUser(WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            false, null));
            webGroupFilterCondEntity
                    .setControlStatus(WebParameterUtils.validIntDataParameter("condStatus",
                            req.getParameter("condStatus"),
                            false,
                            TBaseConstants.META_VALUE_UNDEFINED,
                            0));
            Set<String> filterCondSet =
                    WebParameterUtils.checkAndGetFilterCondSet(req.getParameter("filterConds"), true, false, sBuilder);
            List<BdbGroupFilterCondEntity> webGroupCondEntities =
                    brokerConfManager.confGetBdbAllowedGroupFilterCondSet(webGroupFilterCondEntity);
            SimpleDateFormat formatter =
                    new SimpleDateFormat(TBaseConstants.META_TMP_DATE_VALUE);
            int j = 0;
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\",\"data\":[");
            for (BdbGroupFilterCondEntity entity : webGroupCondEntities) {
                if (!filterCondSet.isEmpty()) {
                    String filterItems = entity.getAttributes();
                    if (filterItems.length() == 2
                            && filterItems.equals(TServerConstants.TOKEN_BLANK_FILTER_CONDITION)) {
                        continue;
                    } else {
                        boolean allInc = true;
                        for (String filterCond : filterCondSet) {
                            if (!filterItems.contains(filterCond)) {
                                allInc = false;
                                break;
                            }
                        }
                        if (!allInc) {
                            continue;
                        }
                    }
                }
                if (j++ > 0) {
                    sBuilder.append(",");
                }
                sBuilder.append("{\"topicName\":\"").append(entity.getTopicName())
                        .append("\",\"groupName\":\"").append(entity.getConsumerGroupName())
                        .append("\",\"condStatus\":").append(entity.getControlStatus());
                if (entity.getAttributes().length() <= 2) {
                    sBuilder.append(",\"filterConds\":\"\"");
                } else {
                    sBuilder.append(",\"filterConds\":\"")
                            .append(entity.getAttributes())
                            .append("\"");
                }
                sBuilder.append(",\"createUser\":\"").append(entity.getCreateUser())
                        .append("\",\"createDate\":\"").append(formatter.format(entity.getCreateDate()))
                        .append("\"}");
            }
            sBuilder.append("],\"count\":").append(j).append("}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\",\"count\":0,\"data\":[]}");
        }
        return sBuilder;
    }

    /**
     * Add authorized consumer group info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminAddConsumerGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            Set<String> configuredTopicSet =
                    brokerConfManager.getTotalConfiguredTopicNames();
            Set<String> batchOpTopicNames =
                    WebParameterUtils.getBatchTopicNames(req.getParameter("topicName"),
                            true, true, configuredTopicSet, sBuilder);
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            true, false, null, sBuilder);
            for (String tmpTopicName : batchOpTopicNames) {
                BdbTopicAuthControlEntity topicAuthControlEntity =
                        brokerConfManager.getBdbEnableAuthControlByTopicName(tmpTopicName);
                if (topicAuthControlEntity == null) {
                    try {
                        brokerConfManager.confSetBdbTopicAuthControl(
                                new BdbTopicAuthControlEntity(tmpTopicName,
                                        false, createUser, createDate));
                    } catch (Exception ee) {
                        //
                    }
                }
                for (String tmpGroupName : batchOpGroupNames) {
                    BdbConsumerGroupEntity webConsumerGroupEntity =
                            new BdbConsumerGroupEntity(tmpTopicName,
                                    tmpGroupName, createUser, createDate);
                    brokerConfManager.confAddAllowedConsumerGroup(webConsumerGroupEntity);
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Add authorized consumer group info in batch
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminBatchAddConsumerGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            List<Map<String, String>> jsonArray =
                    WebParameterUtils.checkAndGetJsonArray("groupNameJsonSet",
                            req.getParameter("groupNameJsonSet"),
                            TBaseConstants.META_VALUE_UNDEFINED, true);
            if ((jsonArray == null) || (jsonArray.isEmpty())) {
                throw new Exception("Null value of groupNameJsonSet, please set the value first!");
            }
            Set<String> configuredTopicSet = brokerConfManager.getTotalConfiguredTopicNames();
            HashMap<String, BdbConsumerGroupEntity> inGroupAuthConfEntityMap =
                    new HashMap<>();
            for (int j = 0; j < jsonArray.size(); j++) {
                Map<String, String> groupObject = jsonArray.get(j);
                try {
                    String groupName =
                        WebParameterUtils.validGroupParameter("groupName",
                            groupObject.get("groupName"),
                            TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                            true, "");
                    String groupTopicName =
                            WebParameterUtils.validStringParameter("topicName",
                                    groupObject.get("topicName"),
                                    TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                                    true, "");
                    String groupCreateUser =
                            WebParameterUtils.validStringParameter("createUser",
                                    groupObject.get("createUser"),
                                    TBaseConstants.META_MAX_USERNAME_LENGTH,
                                    false, null);
                    Date groupCreateDate =
                            WebParameterUtils.validDateParameter("createDate",
                                    groupObject.get("createDate"),
                                    TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                                    false, null);
                    if ((TStringUtils.isBlank(groupCreateUser))
                            || (groupCreateDate == null)) {
                        groupCreateUser = createUser;
                        groupCreateDate = createDate;
                    }
                    if (!configuredTopicSet.contains(groupTopicName)) {
                        throw new Exception(sBuilder.append("Topic ").append(groupTopicName)
                                .append(" not configure in master configure, please configure first!").toString());
                    }
                    String recordKey = sBuilder.append(groupName)
                            .append("-")
                            .append(groupTopicName).toString();
                    sBuilder.delete(0, sBuilder.length());
                    inGroupAuthConfEntityMap.put(recordKey,
                            new BdbConsumerGroupEntity(groupTopicName,
                                    groupName, groupCreateUser, groupCreateDate));
                } catch (Exception ee) {
                    sBuilder.delete(0, sBuilder.length());
                    throw new Exception(sBuilder.append("Process data exception, data is :")
                            .append(groupObject.toString()).append(", exception is : ")
                            .append(ee.getMessage()).toString());
                }

            }
            if (inGroupAuthConfEntityMap.isEmpty()) {
                throw new Exception("Not found record in groupNameJsonSet parameter");
            }
            for (BdbConsumerGroupEntity tmpGroupEntity : inGroupAuthConfEntityMap.values()) {
                BdbTopicAuthControlEntity topicAuthControlEntity =
                        brokerConfManager.getBdbEnableAuthControlByTopicName(tmpGroupEntity.getGroupTopicName());
                if (topicAuthControlEntity == null) {
                    try {
                        brokerConfManager.confSetBdbTopicAuthControl(
                                new BdbTopicAuthControlEntity(tmpGroupEntity.getGroupTopicName(),
                                        false, createUser, createDate));
                    } catch (Exception ee) {
                        //
                    }
                }
                brokerConfManager.confAddAllowedConsumerGroup(tmpGroupEntity);
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Query allowed(authorized?) consumer group info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminQueryConsumerGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        BdbConsumerGroupEntity webConsumerGroupEntity =
                new BdbConsumerGroupEntity();
        try {
            webConsumerGroupEntity
                    .setGroupTopicName(WebParameterUtils.validStringParameter("topicName",
                            req.getParameter("topicName"),
                            TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                            false, null));
            webConsumerGroupEntity
                .setConsumerGroupName(WebParameterUtils.validGroupParameter(
                    "groupName",
                    req.getParameter("groupName"),
                    TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                    false, null));
            webConsumerGroupEntity
                    .setRecordCreateUser(WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            false, null));
            List<BdbConsumerGroupEntity> webConsumerGroupEntities =
                    brokerConfManager.confGetBdbAllowedConsumerGroupSet(webConsumerGroupEntity);
            SimpleDateFormat formatter =
                    new SimpleDateFormat(TBaseConstants.META_TMP_DATE_VALUE);
            int j = 0;
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\",\"count\":")
                    .append(webConsumerGroupEntities.size()).append(",\"data\":[");
            for (BdbConsumerGroupEntity entity : webConsumerGroupEntities) {
                if (j++ > 0) {
                    sBuilder.append(",");
                }
                sBuilder.append("{\"topicName\":\"").append(entity.getGroupTopicName())
                        .append("\",\"groupName\":\"").append(entity.getConsumerGroupName())
                        .append("\",\"createUser\":\"").append(entity.getRecordCreateUser())
                        .append("\",\"createDate\":\"").append(formatter.format(entity.getRecordCreateDate()))
                        .append("\"}");
            }
            sBuilder.append("]}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\",\"count\":0,\"data\":[]}");
        }
        return sBuilder;
    }

    /**
     * Delete allowed(authorized) consumer group info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminDeleteConsumerGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            Set<String> batchOpTopicNames =
                    WebParameterUtils.getBatchTopicNames(req.getParameter("topicName"),
                            true, false, null, sBuilder);
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            false, false, null, sBuilder);
            if (batchOpGroupNames.isEmpty()) {
                for (String tmpTopicName : batchOpTopicNames) {
                    BdbGroupFilterCondEntity webFilterCondEntity =
                            new BdbGroupFilterCondEntity();
                    webFilterCondEntity.setTopicName(tmpTopicName);
                    List<BdbGroupFilterCondEntity> webFilterCondEntities =
                            brokerConfManager.confGetBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                    if (!webFilterCondEntities.isEmpty()) {
                        webFilterCondEntity.setCreateUser("System");
                        brokerConfManager.confDelBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                    }
                    BdbConsumerGroupEntity webConsumerGroupEntity =
                            new BdbConsumerGroupEntity();
                    webConsumerGroupEntity.setGroupTopicName(tmpTopicName);
                    brokerConfManager.confDelBdbAllowedConsumerGroupSet(webConsumerGroupEntity);
                }
            } else {
                for (String tmpTopicName : batchOpTopicNames) {
                    for (String tmpGroupName : batchOpGroupNames) {
                        BdbGroupFilterCondEntity webFilterCondEntity =
                                new BdbGroupFilterCondEntity();
                        webFilterCondEntity.setTopicName(tmpTopicName);
                        webFilterCondEntity.setConsumerGroupName(tmpGroupName);
                        List<BdbGroupFilterCondEntity> webFilterCondEntities =
                                brokerConfManager.confGetBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                        if (!webFilterCondEntities.isEmpty()) {
                            webFilterCondEntity.setCreateUser("System");
                            brokerConfManager.confDelBdbAllowedGroupFilterCondSet(webFilterCondEntity);
                        }
                        BdbConsumerGroupEntity webConsumerGroupEntity =
                                new BdbConsumerGroupEntity();
                        webConsumerGroupEntity.setGroupTopicName(tmpTopicName);
                        webConsumerGroupEntity.setConsumerGroupName(tmpGroupName);
                        brokerConfManager.confDelBdbAllowedConsumerGroupSet(webConsumerGroupEntity);
                    }
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Add black consumer group info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminAddBlackGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            Set<String> batchOpTopicNames =
                    WebParameterUtils.getBatchTopicNames(req.getParameter("topicName"),
                            true, true, brokerConfManager.getTotalConfiguredTopicNames(), sBuilder);
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            true, false, null, sBuilder);
            for (String tmpGroupName : batchOpGroupNames) {
                for (String tmpTopicName : batchOpTopicNames) {
                    BdbBlackGroupEntity webBlackGroupEntity =
                            new BdbBlackGroupEntity(tmpTopicName,
                                    tmpGroupName, createUser, createDate);
                    brokerConfManager.confAddBdbBlackConsumerGroup(webBlackGroupEntity);
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Add black consumer group info in batch
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminBatchAddBlackGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            List<Map<String, String>> jsonArray =
                    WebParameterUtils.checkAndGetJsonArray("groupNameJsonSet",
                            req.getParameter("groupNameJsonSet"),
                            TBaseConstants.META_VALUE_UNDEFINED, true);
            if ((jsonArray == null) || (jsonArray.isEmpty())) {
                throw new Exception("Null value of groupNameJsonSet, please set the value first!");
            }
            Set<String> configuredTopicSet = brokerConfManager.getTotalConfiguredTopicNames();
            HashMap<String, BdbBlackGroupEntity> inBlackGroupEntityMap = new HashMap<>();
            for (int j = 0; j < jsonArray.size(); j++) {
                Map<String, String> groupObject = jsonArray.get(j);
                try {
                    String groupName =
                        WebParameterUtils.validGroupParameter("groupName",
                            groupObject.get("groupName"),
                            TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                            true, "");
                    String groupTopicName =
                            WebParameterUtils.validStringParameter("topicName",
                                    groupObject.get("topicName"),
                                    TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                                    true, "");
                    String groupCreateUser =
                            WebParameterUtils.validStringParameter("createUser",
                                    groupObject.get("createUser"),
                                    TBaseConstants.META_MAX_USERNAME_LENGTH,
                                    false, null);
                    Date groupCreateDate =
                            WebParameterUtils.validDateParameter("createDate",
                                    groupObject.get("createDate"),
                                    TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                                    false, null);
                    if ((TStringUtils.isBlank(groupCreateUser))
                            || (groupCreateDate == null)) {
                        groupCreateUser = createUser;
                        groupCreateDate = createDate;
                    }
                    if (!configuredTopicSet.contains(groupTopicName)) {
                        throw new Exception(sBuilder.append("Topic ").append(groupTopicName)
                                .append(" not configure in master configure, please configure first!").toString());
                    }
                    String recordKey = sBuilder.append(groupName)
                            .append("-").append(groupTopicName).toString();
                    sBuilder.delete(0, sBuilder.length());
                    inBlackGroupEntityMap.put(recordKey,
                            new BdbBlackGroupEntity(groupTopicName,
                                    groupName, groupCreateUser, groupCreateDate));
                } catch (Exception ee) {
                    sBuilder.delete(0, sBuilder.length());
                    throw new Exception(sBuilder.append("Process data exception, data is :")
                            .append(groupObject.toString())
                            .append(", exception is : ")
                            .append(ee.getMessage()).toString());
                }
            }
            if (inBlackGroupEntityMap.isEmpty()) {
                throw new Exception("Not found record in groupNameJsonSet parameter");
            }
            for (BdbBlackGroupEntity tmpBlackGroupEntity
                    : inBlackGroupEntityMap.values()) {
                brokerConfManager.confAddBdbBlackConsumerGroup(tmpBlackGroupEntity);
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Query black consumer group info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminQueryBlackGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        BdbBlackGroupEntity webBlackGroupEntity =
                new BdbBlackGroupEntity();
        try {
            webBlackGroupEntity
                    .setTopicName(WebParameterUtils.validStringParameter("topicName",
                            req.getParameter("topicName"),
                            TBaseConstants.META_MAX_TOPICNAME_LENGTH,
                            false, null));
            webBlackGroupEntity
                .setBlackGroupName(WebParameterUtils.validGroupParameter("groupName",
                    req.getParameter("groupName"),
                    TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                    false, null));
            webBlackGroupEntity
                    .setCreateUser(WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            false, null));
            List<BdbBlackGroupEntity> webBlackGroupEntities =
                    brokerConfManager.confGetBdbBlackConsumerGroupSet(webBlackGroupEntity);
            SimpleDateFormat formatter =
                    new SimpleDateFormat(TBaseConstants.META_TMP_DATE_VALUE);
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\",\"count\":")
                    .append(webBlackGroupEntities.size()).append(",\"data\":[");
            int j = 0;
            for (BdbBlackGroupEntity entity : webBlackGroupEntities) {
                if (j++ > 0) {
                    sBuilder.append(",");
                }
                sBuilder.append("{\"topicName\":\"").append(entity.getTopicName())
                        .append("\",\"groupName\":\"").append(entity.getBlackGroupName())
                        .append("\",\"createUser\":\"").append(entity.getCreateUser())
                        .append("\",\"createDate\":\"").append(formatter.format(entity.getCreateDate()))
                        .append("\"}");
            }
            sBuilder.append("]}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\",\"count\":0,\"data\":[]}");
        }
        return sBuilder;
    }

    /**
     * Delete black consumer group info
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminDeleteBlackGroupInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            true, false, null, sBuilder);
            Set<String> batchOpTopicNames =
                    WebParameterUtils.getBatchTopicNames(req.getParameter("topicName"),
                            false, false, null, sBuilder);
            if (batchOpTopicNames.isEmpty()) {
                for (String tmpGroupName : batchOpGroupNames) {
                    BdbBlackGroupEntity webBlackGroupEntity =
                            new BdbBlackGroupEntity();
                    webBlackGroupEntity.setBlackGroupName(tmpGroupName);
                    brokerConfManager.confDeleteBdbBlackConsumerGroupSet(webBlackGroupEntity);
                }
            } else {
                for (String tmpGroupName : batchOpGroupNames) {
                    for (String tmpTopicName : batchOpTopicNames) {
                        BdbBlackGroupEntity webBlackGroupEntity =
                                new BdbBlackGroupEntity();
                        webBlackGroupEntity.setBlackGroupName(tmpGroupName);
                        webBlackGroupEntity.setTopicName(tmpTopicName);
                        brokerConfManager.confDeleteBdbBlackConsumerGroupSet(webBlackGroupEntity);
                    }
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Add consumer group setting
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminAddConsumeGroupSettingInfo(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            int enableBind =
                    WebParameterUtils.validIntDataParameter("enableBind",
                            req.getParameter("enableBind"),
                            false, 0, 0);
            int allowedBClientRate =
                    WebParameterUtils.validIntDataParameter("allowedBClientRate",
                            req.getParameter("allowedBClientRate"),
                            false, 0, 0);
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            true, false, null, sBuilder);
            for (String tmpGroupName : batchOpGroupNames) {
                BdbConsumeGroupSettingEntity webConsumeGroupSettingEntity =
                        new BdbConsumeGroupSettingEntity(tmpGroupName,
                                enableBind, allowedBClientRate, "", createUser, createDate);
                brokerConfManager.confAddBdbConsumeGroupSetting(webConsumeGroupSettingEntity);
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Add consumer group setting in batch
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminBatchAddConsumeGroupSetting(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String createUser =
                    WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date createDate =
                    WebParameterUtils.validDateParameter("createDate",
                            req.getParameter("createDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            int enableBind =
                    WebParameterUtils.validIntDataParameter("enableBind",
                            req.getParameter("enableBind"),
                            false, 0, 0);
            int allowedBClientRate =
                    WebParameterUtils.validIntDataParameter("allowedBClientRate",
                            req.getParameter("allowedBClientRate"),
                            false, 0, 0);
            List<Map<String, String>> groupNameJsonArray =
                    WebParameterUtils.checkAndGetJsonArray("groupNameJsonSet",
                            req.getParameter("groupNameJsonSet"),
                            TBaseConstants.META_VALUE_UNDEFINED, true);
            if ((groupNameJsonArray == null) || (groupNameJsonArray.isEmpty())) {
                throw new Exception("Null value of groupNameJsonSet, please set the value first!");
            }
            HashMap<String, BdbConsumeGroupSettingEntity> inOffsetRstGroupEntityMap =
                    new HashMap<>();
            for (int j = 0; j < groupNameJsonArray.size(); j++) {
                Map<String, String> groupObject = groupNameJsonArray.get(j);
                try {
                    String groupName =
                        WebParameterUtils.validGroupParameter("groupName",
                            groupObject.get("groupName"),
                            TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                            true, "");
                    String groupCreateUser =
                            WebParameterUtils.validStringParameter("createUser",
                                    groupObject.get("createUser"),
                                    TBaseConstants.META_MAX_USERNAME_LENGTH,
                                    false, createUser);
                    Date groupCreateDate =
                            WebParameterUtils.validDateParameter("createDate",
                                    groupObject.get("createDate"),
                                    TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                                    false, createDate);
                    int groupEnableBind =
                            WebParameterUtils.validIntDataParameter("enableBind",
                                    groupObject.get("enableBind"),
                                    false, enableBind, 0);
                    int groupAllowedBClientRate =
                            WebParameterUtils.validIntDataParameter("allowedBClientRate",
                                    groupObject.get("allowedBClientRate"),
                                    false, allowedBClientRate, 0);
                    inOffsetRstGroupEntityMap.put(groupName,
                            new BdbConsumeGroupSettingEntity(groupName,
                                    groupEnableBind, groupAllowedBClientRate,
                                    "", groupCreateUser, groupCreateDate));
                } catch (Exception ee) {
                    throw new Exception(sBuilder.append("Process data exception, data is :")
                            .append(groupObject.toString())
                            .append(", exception is : ")
                            .append(ee.getMessage()).toString());
                }
            }
            if (inOffsetRstGroupEntityMap.isEmpty()) {
                throw new Exception("Not found record in groupNameJsonSet parameter");
            }
            for (BdbConsumeGroupSettingEntity tmpGroupEntity
                    : inOffsetRstGroupEntityMap.values()) {
                brokerConfManager.confAddBdbConsumeGroupSetting(tmpGroupEntity);
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Query consumer group setting
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminQueryConsumeGroupSetting(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        BdbConsumeGroupSettingEntity queryEntity =
                new BdbConsumeGroupSettingEntity();
        try {
            queryEntity
                .setConsumeGroupName(WebParameterUtils.validGroupParameter("groupName",
                    req.getParameter("groupName"),
                    TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                    false, null));
            queryEntity
                    .setCreateUser(WebParameterUtils.validStringParameter("createUser",
                            req.getParameter("createUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            false, null));
            queryEntity
                    .setEnableBind(WebParameterUtils.validIntDataParameter("enableBind",
                            req.getParameter("enableBind"),
                            false, -2, 0));
            queryEntity
                    .setAllowedBrokerClientRate(WebParameterUtils.validIntDataParameter("allowedBClientRate",
                            req.getParameter("allowedBClientRate"),
                            false, -2, 0));
            List<BdbConsumeGroupSettingEntity> resultEntities =
                    brokerConfManager.confGetBdbConsumeGroupSetting(queryEntity);
            SimpleDateFormat formatter =
                    new SimpleDateFormat(TBaseConstants.META_TMP_DATE_VALUE);
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\",\"count\":")
                    .append(resultEntities.size()).append(",\"data\":[");
            int j = 0;
            for (BdbConsumeGroupSettingEntity entity : resultEntities) {
                if (j++ > 0) {
                    sBuilder.append(",");
                }
                sBuilder.append("{\"groupName\":\"").append(entity.getConsumeGroupName())
                        .append("\",\"enableBind\":").append(entity.getEnableBind())
                        .append(",\"allowedBClientRate\":").append(entity.getAllowedBrokerClientRate())
                        .append(",\"attributes\":\"").append(entity.getAttributes())
                        .append("\",\"lastBindUsedDate\":\"").append(entity.getLastBindUsedDate())
                        .append("\",\"createUser\":\"").append(entity.getCreateUser())
                        .append("\",\"createDate\":\"").append(formatter.format(entity.getCreateDate()))
                        .append("\"}");
            }
            sBuilder.append("]}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\",\"count\":0,\"data\":[]}");
        }
        return sBuilder;
    }

    /**
     * Update consumer group setting
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminUpdConsumeGroupSetting(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            String modifyUser =
                    WebParameterUtils.validStringParameter("modifyUser",
                            req.getParameter("modifyUser"),
                            TBaseConstants.META_MAX_USERNAME_LENGTH,
                            true, "");
            Date modifyDate =
                    WebParameterUtils.validDateParameter("modifyDate",
                            req.getParameter("modifyDate"),
                            TBaseConstants.META_MAX_DATEVALUE_LENGTH,
                            false, new Date());
            int enableBind =
                    WebParameterUtils.validIntDataParameter("enableBind",
                            req.getParameter("enableBind"),
                            false, -2, 0);
            int allowedBClientRate =
                    WebParameterUtils.validIntDataParameter("allowedBClientRate",
                            req.getParameter("allowedBClientRate"),
                            false, -2, 0);
            if (enableBind == -2
                    && allowedBClientRate == -2) {
                throw new Exception("Not require update content in request parameter!");
            }
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            true, false, null, sBuilder);
            for (String tmpGroupName : batchOpGroupNames) {
                try {
                    boolean isChanged = false;
                    BdbConsumeGroupSettingEntity oldEntity =
                            brokerConfManager.getBdbConsumeGroupSetting(tmpGroupName);
                    if (oldEntity == null) {
                        continue;
                    }
                    BdbConsumeGroupSettingEntity newEntity =
                            new BdbConsumeGroupSettingEntity(oldEntity);
                    if (enableBind != -2) {
                        if (newEntity.getEnableBind() != enableBind) {
                            isChanged = true;
                            newEntity.setEnableBind(enableBind);
                        }
                    }
                    if (allowedBClientRate != -2) {
                        if (allowedBClientRate != newEntity.getAllowedBrokerClientRate()) {
                            isChanged = true;
                            newEntity.setAllowedBrokerClientRate(allowedBClientRate);
                        }
                    }
                    if (isChanged) {
                        brokerConfManager.confUpdBdbConsumeGroupSetting(newEntity);
                    }
                } catch (Throwable e) {
                    //
                }
            }
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

    /**
     * Delete consumer group setting
     *
     * @param req
     * @return
     * @throws Exception
     */
    public StringBuilder adminDeleteConsumeGroupSetting(HttpServletRequest req) throws Exception {
        StringBuilder sBuilder = new StringBuilder(512);
        try {
            WebParameterUtils.reqAuthorizeCheck(master, brokerConfManager,
                    req.getParameter("confModAuthToken"));
            Set<String> batchOpGroupNames =
                    WebParameterUtils.getBatchGroupNames(req.getParameter("groupName"),
                            true, false, null, sBuilder);
            brokerConfManager.confDeleteBdbConsumeGroupSetting(batchOpGroupNames, sBuilder);
            sBuilder.append("{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\"}");
        } catch (Exception e) {
            sBuilder.delete(0, sBuilder.length());
            sBuilder.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"")
                    .append(e.getMessage()).append("\"}");
        }
        return sBuilder;
    }

}
