package org.apache.tubemq.manager.controller.group.request;

import java.util.Date;
import lombok.Data;
import org.apache.tubemq.manager.controller.node.request.BaseReq;

@Data
public class ResetTimeOffsetReq extends BaseReq {
    private Date resetDate;
    private String groupName;
    private String topicName;
    // determine whether get consume offset or produce offset
    private Boolean useConsumeOffset;
}
