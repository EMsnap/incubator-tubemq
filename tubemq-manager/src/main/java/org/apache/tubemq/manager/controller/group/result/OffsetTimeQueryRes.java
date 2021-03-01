package org.apache.tubemq.manager.controller.group.result;

import static org.apache.tubemq.manager.service.TubeMQHttpConst.BLANK;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.tubemq.manager.controller.group.result.OffsetTimeQueryRes.GroupOffsetItem.TopicOffsetItem;

@Data
public class OffsetTimeQueryRes {

    private boolean result;
    private int errCode;
    private String errMsg;
    private List<String> messages;

    private static final int GROUP_NAME_INDEX = 0;
    private static final int TOPICNAME_INDEX = 0;
    private static final int BROKERID_INDEX = 1;
    private static final int PARTITION_INDEX = 2;
    private static final int PRODUCE_OFFSET_INDEX = 3;
    private static final int CONSUME_OFFSET_INDEX = 4;
    private static final int TOPIC_OFFSET_INFO_SIZE = 5;

    @Data
    public static class GroupOffsetItem {

        private String groupName;
        private List<TopicOffsetItem> topicOffsets;

        @Data
        public static class TopicOffsetItem {
            private String topicName;
            private String brokerId;
            private String partitionId;
            private String produceOffset;
            private String consumeOffset;
        }
    }

    /**
     * message format : group1 offsetTopic1 1 0 21896 -2 offsetTopic1 1 1 21896 -2 offsetTopic1 1 2 21896 -2
     * @return
     */
    public List<GroupOffsetItem> getGroupOffsets() {
        if (CollectionUtils.isEmpty(messages)) {
            return null;
        }
        List<GroupOffsetItem> groupOffsets = new ArrayList<>();
        messages.forEach(message -> {
            List<String> messageItems = new ArrayList<>(Arrays.asList(message.split(BLANK)));
            GroupOffsetItem groupOffsetItem = new GroupOffsetItem();
            groupOffsetItem.setGroupName(messageItems.get(GROUP_NAME_INDEX));
            // remove the group name
            messageItems.remove(GROUP_NAME_INDEX);
            List<TopicOffsetItem> topicOffsets = new ArrayList<>();
            List<List<String>> topicOffsetList = Lists.partition(messageItems,
                TOPIC_OFFSET_INFO_SIZE);
            topicOffsetList.forEach(topicOffset -> {
                    if (topicOffset.size() != TOPIC_OFFSET_INFO_SIZE) {
                        return;
                    }
                    TopicOffsetItem topicOffsetItem = new TopicOffsetItem();
                    topicOffsetItem.setTopicName(topicOffset.get(TOPICNAME_INDEX));
                    topicOffsetItem.setBrokerId(topicOffset.get(BROKERID_INDEX));
                    topicOffsetItem.setPartitionId(topicOffset.get(PARTITION_INDEX));
                    topicOffsetItem.setProduceOffset(topicOffset.get(PRODUCE_OFFSET_INDEX));
                    topicOffsetItem.setConsumeOffset(topicOffset.get(CONSUME_OFFSET_INDEX));
                    topicOffsets.add(topicOffsetItem);
                }
            );
            groupOffsetItem.setTopicOffsets(topicOffsets);
            groupOffsets.add(groupOffsetItem);
        });
        return groupOffsets;
    }
}
