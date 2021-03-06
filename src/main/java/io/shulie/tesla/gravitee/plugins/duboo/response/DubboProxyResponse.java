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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.shulie.tesla.gravitee.plugins.duboo.utils.StringUtils;

/**
 * @ClassName: DubboProxyResponse
 * @author: wangjian
 * @Date: 2020/3/9 20:16
 * @Description:
 */
public class DubboProxyResponse implements ProxyResponse {

    protected Handler<Buffer> bodyHandler;
    protected Handler<Void> endHandler;
    private final HttpHeaders headers = new HttpHeaders();

    protected String asyncResult;
    private Buffer buffer;

    public DubboProxyResponse(String asyncResult) {
        this.asyncResult = asyncResult;
        init();
    }

    protected void init() {
        buffer = Buffer.buffer(asyncResult);
        headers.set(HttpHeaders.CONTENT_LENGTH, Integer.toString(buffer.length()));
        headers.set(HttpHeaders.CONTENT_TYPE, getContentType(asyncResult));
    }

    protected static String getContentType(String content) {
        if (StringUtils.isJSON(content)) {
            return MediaType.APPLICATION_JSON;
        }
        if (StringUtils.isXML(content)) {
            return MediaType.TEXT_XML;
        }
        return MediaType.TEXT_PLAIN;
    }
    @Override
    public int status() {
        return 200;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        this.bodyHandler = bodyHandler;
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    @Override
    public ReadStream<Buffer> resume() {
        if (buffer != null) {
            bodyHandler.handle(buffer);
        }

        endHandler.handle(null);
        return this;
    }
}
