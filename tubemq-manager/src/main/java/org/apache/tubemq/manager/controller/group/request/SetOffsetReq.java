package org.apache.tubemq.manager.controller.group.request;

import static org.apache.tubemq.manager.service.TubeMQHttpConst.ADMIN_SET_OFFSET;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.OP_MODIFY;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import org.apache.tubemq.manager.controller.group.result.OffsetTimeQueryRes.GroupOffsetItem.TopicOffsetItem;
import org.apache.tubemq.manager.controller.node.request.BaseReq;

@Data
public class SetOffsetReq extends BaseReq {

    private String groupName;
    private String brokerIp;
    private String brokerWebPort;
    private String modifyUser;
    private Boolean manualSet;
    private String offsetJsonInfo;

    public SetOffsetReq(String groupName, String brokerIp, String brokerWebPort,
        String modifyUser, Boolean manualSet, List<TopicOffsetItem> offsetItems, boolean isConsume) {
        this.groupName = groupName;
        this.brokerIp = brokerIp;
        this.brokerWebPort = brokerWebPort;
        this.modifyUser = modifyUser;
        this.manualSet = manualSet;
        this.setMethod(ADMIN_SET_OFFSET);
        this.setType(OP_MODIFY);
        setOffsetJsonInfo(offsetItems, isConsume);
    }


    public void setOffsetJsonInfo(List<TopicOffsetItem> offsetItems, boolean isConsume) {
        StringBuilder sBuilder = new StringBuilder(1024);
        AtomicInteger partCnt = new AtomicInteger();
        if (offsetItems.isEmpty()) {
            return;
        }
        sBuilder.append("{");
        offsetItems.forEach(topicOffsetItem -> {
            if (partCnt.getAndIncrement() > 0) {
                sBuilder.append(",");
            }
            String key = topicOffsetItem.getBrokerId() + ":" +
                topicOffsetItem.getTopicName() + ":" + topicOffsetItem.getPartitionId();
            sBuilder.append("\"").append(key).append("\":");
            if (isConsume) {
                sBuilder.append(topicOffsetItem.getConsumeOffset());
            } else {
                sBuilder.append(topicOffsetItem.getProduceOffset());
            }
        });
        sBuilder.append("}");
        this.offsetJsonInfo = sBuilder.toString();
    }

}
