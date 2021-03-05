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

package org.apache.tubemq.manager.controller.node.request;

import com.google.common.collect.Lists;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.apache.tubemq.manager.entry.BrokerEntry;
import org.apache.tubemq.manager.service.tube.BrokerConf;

import java.util.List;
import org.springframework.validation.annotation.Validated;

import static org.apache.tubemq.manager.service.TubeMQHttpConst.*;

@Data
@Validated
public class AddBrokersReq extends BaseReq{


    private String confModAuthToken;

    private String createUser;

    @NotNull(message = "brokerJsonSet can't be null")
    @Valid
    private List<BrokerConf> brokerJsonSet;

    public static AddBrokersReq getAddBrokerReq(Long clusterId) {
        AddBrokersReq req = new AddBrokersReq();
        req.setClusterId(clusterId);
        req.setConfModAuthToken(DEFAULT_CONF_MOD_AUTH_TOKEN);
        req.setMethod(BATCH_ADD_BROKER);
        req.setType(OP_MODIFY);
        req.setCreateUser(WEB_API);
        return req;
    }


    public static AddBrokersReq getBatchAddBrokerReq
        (Long clusterId, List<BrokerEntry> brokerEntries) {
        AddBrokersReq req = AddBrokersReq.getAddBrokerReq(clusterId);
        // generate add brokers req using given target broker ips
        List<BrokerConf> brokerConfs = Lists.newArrayList();
        brokerEntries.forEach(
            brokerEntry -> {
                BrokerConf brokerConf = new BrokerConf(brokerEntry);
                brokerConfs.add(brokerConf);
            }
        );
        req.setBrokerJsonSet(brokerConfs);
        return req;
    }


    public static AddBrokersReq getBatchAddBrokersReq(List<String> targetIps, Long clusterId, BrokerConf sourceBrokerConf) {
        AddBrokersReq addBrokersReq = AddBrokersReq.getAddBrokerReq(clusterId);
        // generate add brokers req using given target broker ips
        List<BrokerConf> brokerConfs = Lists.newArrayList();
        targetIps.forEach(ip -> {
            BrokerConf brokerConf = new BrokerConf(sourceBrokerConf);
            brokerConf.setBrokerIp(ip);
            brokerConf.setBrokerId(0L);
            brokerConfs.add(brokerConf);
        });
        addBrokersReq.setBrokerJsonSet(brokerConfs);
        return addBrokersReq;
    }

}
