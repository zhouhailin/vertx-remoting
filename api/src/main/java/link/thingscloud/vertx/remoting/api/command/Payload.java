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

package link.thingscloud.vertx.remoting.api.command;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public class Payload extends HashMap<String, Object> {

    public static Payload of(String value) {
        Payload payload = new Payload();
        payload.put("body", value);
        return payload;
    }

    public static Payload of(Object value) {
        Payload payload = new Payload();
        JsonObject.mapFrom(value).forEach(entry -> payload.put(entry.getKey(), entry.getValue()));
        return payload;
    }

}
