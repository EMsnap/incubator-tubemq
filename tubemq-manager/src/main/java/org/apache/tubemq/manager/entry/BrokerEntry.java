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


package org.apache.tubemq.manager.entry;


import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "broker", uniqueConstraints=
@UniqueConstraint(columnNames={"brokerIp", "clusterId"}))
@Data
@EntityListeners(AuditingEntityListener.class)
public class BrokerEntry {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Null
    private Long id;
    @Min(0)
    private Long brokerId;
    @NotEmpty
    private String brokerIp;
    @Null
    private Long version;
    @CreatedDate
    private Date createDate;
    @LastModifiedDate
    private Date modifyTime;
    @Size(max = 64)
    private String createUser;
    private Long regionId;
    @NotNull
    private Long clusterId;
    @NotNull
    @Min(0)
    private Integer brokerPort;
    private String deleteWhen;
    @Min(1)
    private Integer numPartitions;
    @Min(0)
    private Integer unflushThreshold;
    @Min(1)
    private Integer unflushInterval;
    @Min(0)
    private Integer unflushDataHold;
    private boolean acceptPublish;
    private boolean acceptSubscribe;
    private Integer brokerTLSPort;
    private Integer numTopicStores;
    private Integer memCacheMsgCntInK;
    private Integer memCacheMsgSizeInMB;
    private Integer memCacheFlushIntegervl;
    private String deletePolicy;
}
