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
import org.apache.tubemq.manager.controller.group.request.DeleteOffsetReq;
import org.apache.tubemq.manager.controller.group.request.QueryOffsetReq;
import org.apache.tubemq.manager.controller.node.request.AddTopicReq;
import org.apache.tubemq.manager.controller.node.request.CloneOffsetReq;
import org.apache.tubemq.manager.controller.topic.request.RebalanceGroupReq;
import org.apache.tubemq.manager.entry.MasterEntry;
import org.apache.tubemq.manager.service.tube.TubeHttpGroupDetailInfo;
import org.apache.tubemq.manager.service.tube.TubeHttpTopicInfoList;

public interface TopicService {

    /**
     * get consumer group run info
     * @param masterEntry
     * @param group
     * @return
     */
    TubeHttpGroupDetailInfo requestGroupRunInfo(MasterEntry masterEntry, String group);

    /**
     * clone offset to other groups
     * @param req
     * @return
     */
    TubeMQResult cloneOffsetToOtherGroups(CloneOffsetReq req);

    /**
     * get topic config info
     * @param masterEntry
     * @param topic
     * @return
     */
    TubeHttpTopicInfoList requestTopicConfigInfo(MasterEntry masterEntry, String topic);

    /**
     * rebalance group
     * @param req
     * @return
     */
    TubeMQResult rebalanceGroup(RebalanceGroupReq req);

    /**
     * delete offset given topic and broker
     * @param req
     * @return
     */
    TubeMQResult deleteOffset(DeleteOffsetReq req);


    /**
     * query offset given topic and group name
     * @param req
     * @return
     */
    TubeMQResult queryOffset(QueryOffsetReq req);


    TubeMQResult addTopicsToBrokers(MasterEntry masterEntry, List<Long> brokerIds,
        List<AddTopicReq> addTopicReqs);

    TubeMQResult addTopicToBrokers(AddTopicReq req, MasterEntry masterEntry)
        throws Exception;
}
