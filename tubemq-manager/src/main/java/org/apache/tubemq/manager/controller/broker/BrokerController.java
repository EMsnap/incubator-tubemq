package org.apache.tubemq.manager.controller.broker;

import static org.apache.tubemq.manager.service.TubeMQHttpConst.ADD;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.AUTH_CONTROL;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.CLONE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.DELETE;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.MODIFY;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.NO_SUCH_CLUSTER;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.NO_SUCH_METHOD;
import static org.apache.tubemq.manager.service.TubeMQHttpConst.REMOVE;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.tubemq.manager.controller.TubeMQResult;
import org.apache.tubemq.manager.controller.node.request.BatchAddTopicReq;
import org.apache.tubemq.manager.controller.node.request.CloneTopicReq;
import org.apache.tubemq.manager.controller.topic.request.DeleteTopicReq;
import org.apache.tubemq.manager.controller.topic.request.ModifyTopicReq;
import org.apache.tubemq.manager.controller.topic.request.SetAuthControlReq;
import org.apache.tubemq.manager.entry.BrokerEntry;
import org.apache.tubemq.manager.entry.ClusterEntry;
import org.apache.tubemq.manager.service.ClusterServiceImpl;
import org.apache.tubemq.manager.service.interfaces.BrokerService;
import org.apache.tubemq.manager.service.interfaces.ClusterService;
import org.apache.tubemq.manager.service.interfaces.MasterService;
import org.apache.tubemq.manager.service.interfaces.NodeService;
import org.apache.tubemq.manager.service.interfaces.TopicService;
import org.apache.tubemq.manager.utils.ValidateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/broker")
@Slf4j
@Validated
public class BrokerController {

    @Autowired
    private NodeService nodeService;

    private Gson gson = new Gson();

    @Autowired
    private MasterService masterService;

    @Autowired
    private BrokerService brokerService;

    @Autowired
    private ClusterService clusterService;


    /**
     * broker method proxy
     * divides the operation on broker to different method
     */
    @RequestMapping(value = "")
    public @ResponseBody
    TubeMQResult topicMethodProxy(
        @RequestParam String method, @RequestBody String req) throws Exception {
        switch (method) {
            case ADD:
                return nodeService.batchAddTopic(gson.fromJson(req, BatchAddTopicReq.class));
            case CLONE:
                return nodeService.cloneTopicToBrokers(gson.fromJson(req, CloneTopicReq.class));
            case AUTH_CONTROL:
                return masterService.baseRequestMaster(gson.fromJson(req, SetAuthControlReq.class));
            case MODIFY:
                return masterService.baseRequestMaster(gson.fromJson(req, ModifyTopicReq.class));
            case DELETE:
            case REMOVE:
                return masterService.baseRequestMaster(gson.fromJson(req, DeleteTopicReq.class));
            default:
                return TubeMQResult.errorResult(NO_SUCH_METHOD);
        }
    }

    /**
     *
     * @param clusterId
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public TubeMQResult addBrokers(@RequestParam @NotNull Long clusterId,
        @RequestBody @Valid List<BrokerEntry> brokerEntryList) {
        ClusterEntry oneCluster = clusterService.getOneCluster(clusterId);
        if (ValidateUtils.isNull(oneCluster)) {
            return TubeMQResult.errorResult(NO_SUCH_CLUSTER);
        }
        return brokerService.batchAddNewBrokers(clusterId, brokerEntryList);
    }

}
