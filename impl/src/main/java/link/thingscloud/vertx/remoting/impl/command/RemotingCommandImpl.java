/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package link.thingscloud.vertx.remoting.impl.command;

import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.api.command.TrafficType;
import org.apache.commons.lang3.builder.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public class RemotingCommandImpl implements RemotingCommand {
    @EqualsExclude
    public final static RequestIdGenerator REQUEST_ID_GENERATOR = RequestIdGenerator.inst;

    private int cmdCode;
    private int cmdVersion;
    private volatile int requestId = REQUEST_ID_GENERATOR.incrementAndGet();
    private TrafficType trafficType = TrafficType.REQUEST_SYNC;
    private int opCode = RemotingSysResponseCode.SUCCESS;
    private String remark = "";

    @ToStringExclude
    private Map<String, String> properties = new HashMap<>();

    @ToStringExclude
    private byte[] payload;

    @Override
    public int cmdCode() {
        return this.cmdCode;
    }

    @Override
    public RemotingCommandImpl cmdCode(int code) {
        this.cmdCode = code;
        return this;
    }

    @Override
    public int cmdVersion() {
        return this.cmdVersion;
    }

    @Override
    public RemotingCommandImpl cmdVersion(int version) {
        this.cmdVersion = version;
        return this;
    }

    @Override
    public int requestID() {
        return requestId;
    }

    @Override
    public RemotingCommandImpl requestID(int value) {
        this.requestId = value;
        return this;
    }

    @Override
    public TrafficType trafficType() {
        return this.trafficType;
    }

    @Override
    public RemotingCommandImpl trafficType(TrafficType value) {
        this.trafficType = value;
        return this;
    }

    @Override
    public int opCode() {
        return this.opCode;
    }

    @Override
    public RemotingCommandImpl opCode(int value) {
        this.opCode = value;
        return this;
    }

    @Override
    public String remark() {
        return this.remark;
    }

    @Override
    public RemotingCommandImpl remark(String value) {
        this.remark = value;
        return this;
    }

    @Override
    public Map<String, String> properties() {
        return Collections.unmodifiableMap(this.properties);
    }

    @Override
    public String property(String key) {
        return this.properties.get(key);
    }

    @Override
    public RemotingCommandImpl property(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    @Override
    public byte[] payload() {
        return this.payload;
    }

    @Override
    public RemotingCommandImpl payload(byte[] payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }

    public int getCmdCode() {
        return cmdCode;
    }

    public RemotingCommandImpl setCmdCode(int cmdCode) {
        this.cmdCode = cmdCode;
        return this;
    }

    public int getCmdVersion() {
        return cmdVersion;
    }

    public RemotingCommandImpl setCmdVersion(int cmdVersion) {
        this.cmdVersion = cmdVersion;
        return this;
    }

    public int getRequestId() {
        return requestId;
    }

    public RemotingCommandImpl setRequestId(int requestId) {
        this.requestId = requestId;
        return this;
    }

    public TrafficType getTrafficType() {
        return trafficType;
    }

    public RemotingCommandImpl setTrafficType(TrafficType trafficType) {
        this.trafficType = trafficType;
        return this;
    }

    public int getOpCode() {
        return opCode;
    }

    public RemotingCommandImpl setOpCode(int opCode) {
        this.opCode = opCode;
        return this;
    }

    public String getRemark() {
        return remark;
    }

    public RemotingCommandImpl setRemark(String remark) {
        this.remark = remark;
        return this;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public RemotingCommandImpl setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public byte[] getPayload() {
        return payload;
    }

    public RemotingCommandImpl setPayload(byte[] payload) {
        this.payload = payload;
        return this;
    }

}
