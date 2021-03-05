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

package org.apache.tubemq.manager.service.tube;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.tubemq.manager.entry.BrokerEntry;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrokerConf {

    @NotNull
    private String brokerIp;
    private Integer brokerPort;
    private Long brokerId;
    private String deleteWhen;
    private Integer numPartitions;
    private Integer unflushThreshold;
    private Integer unflushInterval;
    private Integer unflushDataHold;
    private boolean acceptPublish;
    private boolean acceptSubscribe;
    private String createUser;
    private Integer brokerTLSPort;
    private Integer numTopicStores;
    private Integer memCacheMsgCntInK;
    private Integer memCacheMsgSizeInMB;
    private Integer memCacheFlushIntegervl;
    private String deletePolicy;

    public BrokerConf(BrokerConf other) {
        this.brokerIp = other.brokerIp;
        this.brokerPort = other.brokerPort;
        this.brokerId = other.brokerId;
        this.deleteWhen = other.deleteWhen;
        this.numPartitions = other.numPartitions;
        this.unflushThreshold = other.unflushThreshold;
        this.unflushInterval = other.unflushInterval;
        this.unflushDataHold = other.unflushDataHold;
        this.acceptPublish = other.acceptPublish;
        this.acceptSubscribe = other.acceptSubscribe;
        this.createUser = other.createUser;
        this.brokerTLSPort = other.brokerTLSPort;
        this.numTopicStores = other.numTopicStores;
        this.memCacheMsgCntInK = other.memCacheMsgCntInK;
        this.memCacheMsgSizeInMB = other.memCacheMsgSizeInMB;
        this.memCacheFlushIntegervl = other.memCacheFlushIntegervl;
        this.deletePolicy = other.deletePolicy;
    }


    public BrokerConf(BrokerEntry brokerEntry) {
        this.brokerIp = brokerEntry.getBrokerIp();
        this.brokerPort = brokerEntry.getBrokerPort();
        this.brokerId = brokerEntry.getBrokerId();
        this.deleteWhen = brokerEntry.getDeleteWhen();
        this.numPartitions = brokerEntry.getNumPartitions();
        this.unflushThreshold = brokerEntry.getUnflushThreshold();
        this.unflushInterval = brokerEntry.getUnflushInterval();
        this.unflushDataHold = brokerEntry.getUnflushDataHold();
        this.acceptPublish = brokerEntry.isAcceptPublish();
        this.acceptSubscribe = brokerEntry.isAcceptSubscribe();
        this.createUser = brokerEntry.getCreateUser();
        this.brokerTLSPort = brokerEntry.getBrokerTLSPort();
        this.numTopicStores = brokerEntry.getNumTopicStores();
        this.memCacheMsgCntInK = brokerEntry.getMemCacheMsgCntInK();
        this.memCacheMsgSizeInMB = brokerEntry.getMemCacheMsgSizeInMB();
        this.memCacheFlushIntegervl = brokerEntry.getMemCacheFlushIntegervl();
        this.deletePolicy = brokerEntry.getDeletePolicy();
    }


}


