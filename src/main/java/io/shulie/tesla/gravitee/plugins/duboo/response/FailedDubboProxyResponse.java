/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.shulie.tesla.gravitee.plugins.duboo.response;

/**
 * @ClassName: FailedDubboProxyResponse
 * @author: wangjian
 * @Date: 2020/3/9 20:21
 * @Description:
 */
public class FailedDubboProxyResponse extends DubboProxyResponse {
    public static final String FAILED_RESPONSE_STRING_PREFIX = "{\"result\":";
    public static final String FAILED_RESPONSE_STRING_SUFFIX = "}";

    public FailedDubboProxyResponse(String msg) {
        super(FAILED_RESPONSE_STRING_PREFIX + msg + FAILED_RESPONSE_STRING_SUFFIX);
    }

    @Override
    public int status() {
        return 500;
    }
}
