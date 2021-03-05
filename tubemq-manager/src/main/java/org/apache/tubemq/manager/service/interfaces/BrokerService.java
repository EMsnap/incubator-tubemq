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

package org.apache.tubemq.manager.service.interfaces;

import java.util.List;
import org.apache.tubemq.manager.controller.TubeMQResult;
import org.apache.tubemq.manager.controller.node.request.CloneBrokersReq;
import org.apache.tubemq.manager.entry.BrokerEntry;

public interface BrokerService {

    TubeMQResult createBroker(long clusterId, BrokerEntry brokerEntry);

    /**
     * reset the brokers in a region to be default region
     * @param regionId
     * @param clusterId
     */
    void resetBrokerRegions(long regionId, long clusterId);

    /**
     * update brokers to be in a region
     * @param brokerIdList
     * @param regionId
     * @param clusterId
     */
    void updateBrokersRegion(List<Long> brokerIdList, Long regionId, Long clusterId);

    /**
     * check if all the brokers exsit in this cluster
     * @param brokerIdList
     * @param clusterId
     * @return
     */
    boolean checkIfBrokersAllExsit(List<Long> brokerIdList, long clusterId);

    /**
     * get all broker id list in a region
     * @param regionId
     * @param cluster
     * @return
     */
    List<Long> getBrokerIdListInRegion(long regionId, long cluster);

    TubeMQResult cloneBrokersWithTopic(CloneBrokersReq req) throws Exception;

    List<BrokerEntry> getOnlineBrokerConfigs(long clusterId);

    /**
     * batch add brokers to a cluster and send req to master
     * if add to db fail, fail
     * @param clusterId
     * @param brokerEntries
     * @return
     */
    TubeMQResult batchAddNewBrokers(long clusterId, List<BrokerEntry> brokerEntries);
}
