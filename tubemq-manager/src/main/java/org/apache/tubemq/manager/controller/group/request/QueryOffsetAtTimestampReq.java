package org.apache.tubemq.manager.controller.group.request;

import static org.apache.tubemq.manager.service.TubeMQHttpConst.ADMIN_OFFSET_AT_TIMESTAMP;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.OP_QUERY;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.tubemq.manager.controller.node.request.BaseReq;

@Data
public class QueryOffsetAtTimestampReq extends BaseReq {

    @NonNull
    private String brokerWebPort;
    @NonNull
    private String brokerIp;
    @NonNull
    private String filterConds;

    public QueryOffsetAtTimestampReq(String brokerWebPort, String brokerIp, String requestTime) {
        this.brokerWebPort = brokerWebPort;
        this.brokerIp = brokerIp;
        this.filterConds = requestTime;
        this.setMethod(ADMIN_OFFSET_AT_TIMESTAMP);
        this.setType(OP_QUERY);
    }
}
