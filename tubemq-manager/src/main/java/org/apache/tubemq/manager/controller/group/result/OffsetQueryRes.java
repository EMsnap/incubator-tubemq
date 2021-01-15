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

package org.apache.tubemq.manager.controller.group.result;

import java.util.List;
import lombok.Data;

@Data
public class OffsetQueryRes {
    private boolean result;
    private int errCode;
    private String errMsg;
    private List<GroupOffsetRes> dataSet;
    private int totalCnt;

    @Data
    private static class GroupOffsetRes {
        private String groupName;
        private List<TopicOffsetRes> subInfo;
        private int topicCount;

        @Data
        private static class TopicOffsetRes {
            private String topicName;
            private List<OffsetPartitionRes> offsets;
            private int partCount;

            @Data
            private static class OffsetPartitionRes {
                private int partitionId;
                private long curOffset;
                private int flightOffset;
                private int curDataOffset;
                private int offsetLag;
                private int dataLag;
                private int offsetMax;
                private int dataMax;
            }
        }
    }

}
