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

package link.thingscloud.vertx.remoting.config;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public abstract class RemotingConfig {

    private int publicExecutorThreads = 4;
    private int asyncHandlerExecutorThreads = Runtime.getRuntime().availableProcessors();

    public abstract int getOnewayInvokeSemaphore();

    public abstract int getAsyncInvokeSemaphore();

    public int getPublicExecutorThreads() {
        return publicExecutorThreads;
    }

    public RemotingConfig setPublicExecutorThreads(int publicExecutorThreads) {
        this.publicExecutorThreads = publicExecutorThreads;
        return this;
    }

    public int getAsyncHandlerExecutorThreads() {
        return asyncHandlerExecutorThreads;
    }

    public RemotingConfig setAsyncHandlerExecutorThreads(int asyncHandlerExecutorThreads) {
        this.asyncHandlerExecutorThreads = asyncHandlerExecutorThreads;
        return this;
    }
}
