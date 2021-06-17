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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
class CodecHelperTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void encodeCommand() {
        RemotingCommandFactoryImpl factory = new RemotingCommandFactoryImpl();
        RemotingCommand request = factory.createRequest();
        request.cmdCode(1).opCode(10).remark("xxx").property("xyz", "abc");
        String s = CodecHelper.encodeCommand(request);
        System.out.println(CodecHelper.encodeCommand(request).hashCode());
        System.out.println(CodecHelper.decode(s).hashCode());
        System.out.println(CodecHelper.decode(s).hashCode());
        System.out.println(CodecHelper.decode(s).equals(CodecHelper.decode(s)));
    }

    @org.junit.jupiter.api.Test
    void decode() {
        RemotingCommand decode = CodecHelper.decode("{\"cmdCode\":1,\"cmdVersion\":0,\"requestId\":1,\"trafficType\":\"REQUEST_SYNC\",\"opCode\":10,\"remark\":\"xxx\",\"properties\":{},\"payload\":null}");
        System.out.println(decode);
        System.out.println(CodecHelper.decode("{\"cmdCode\":1,\"cmdVersion\":0,\"requestId\":1,\"trafficType\":\"REQUEST_SYNC\",\"opCode\":10,\"remark\":\"xxx\",\"properties\":{\"xyz\":\"abc\"},\"payload\":null}").hashCode());
        System.out.println(CodecHelper.decode("{\"cmdCode\":1,\"cmdVersion\":0,\"requestId\":1,\"trafficType\":\"REQUEST_SYNC\",\"opCode\":10,\"remark\":\"xxx\",\"properties\":{\"xyz\":\"abc\"},\"payload\":null}").hashCode());
    }

}