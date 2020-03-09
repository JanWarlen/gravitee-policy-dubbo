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
package io.shulie.tesla.gravitee.plugins.duboo.connection;

import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.utils.ReferenceConfigCache;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.handlers.api.context.ApiTemplateVariableProvider;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.handler.context.ReactableExecutionContext;
import io.shulie.tesla.gravitee.plugins.duboo.DubboPolicyConfiguration;
import io.shulie.tesla.gravitee.plugins.duboo.response.DubboProxyResponse;
import io.shulie.tesla.gravitee.plugins.duboo.response.FailedDubboProxyResponse;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

/**
 * @ClassName: DubboProxyConnection
 * @author: wangjian
 * @Date: 2020/3/9 19:47
 * @Description:
 */
public class DubboProxyConnection implements ProxyConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboProxyConnection.class);

    private Buffer content;

    private Handler<ProxyResponse> responseHandler;

    private DubboPolicyConfiguration configuration;

    private DubboConnectionManager manager;

    private String dubboUri;

    public DubboProxyConnection(ExecutionContext executionContext,
                                DubboPolicyConfiguration configuration,
                                DubboConnectionManager manager) {
        this.configuration = configuration;
        this.manager = manager;
        Api api = ((ApiTemplateVariableProvider) ((ArrayList) ((ReactableExecutionContext) executionContext).getProviders()).get(2)).getApi();
        Set<EndpointGroup> groups = api.getProxy().getGroups();
        groups.forEach(item -> {
            Set<Endpoint> endpoints = item.getEndpoints();
            endpoints.forEach(tmp -> {
                dubboUri = tmp.getTarget();
            });
        });
        Vertx vertx = executionContext.getComponent(Vertx.class);
        this.manager.addConfiguration(vertx, configuration);
    }

    @Override
    public WriteStream<Buffer> write(Buffer buffer) {
        if (content == null) {
            content = Buffer.buffer();
        }
        content.appendBuffer(buffer);
        return this;
    }

    @Override
    public void end() {
        try {
            URI uri = new URI(dubboUri);
            String params = uri.getQuery();
            Properties dubboParams = new Properties();
            dubboParams.load(new StringReader(params.replace("&", System.lineSeparator())));
            this.manager.requestDubbo(configuration, uri, dubboParams, res -> {

                ReferenceConfig<GenericService> result = res.result();
                // 获取缓存中的实例
                ReferenceConfigCache cache = ReferenceConfigCache.getCache();
                GenericService genericService = cache.get(result);
                Object dubboRes = genericService.$invoke(dubboParams.getProperty("methods"),
                        dubboParams.getProperty("paramTypes").split(";"),
                        JSONArray.parseArray(content.toString()).toArray());
                responseHandler.handle(new DubboProxyResponse(JSONObject.toJSONString(dubboRes)));
            });
        } catch (URISyntaxException e) {
            LOGGER.error("target url is illegal:", e);
            responseHandler.handle(new FailedDubboProxyResponse("target url is illegal"));
        } catch (IOException e) {
            LOGGER.error("target url param is illegal:", e);
            responseHandler.handle(new FailedDubboProxyResponse("target url param is illegal"));
        }
    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    private void sendSuccessfulResponse(String response) {
        responseHandler.handle(new DubboProxyResponse(response));

    }

    private void sendErrorResponse(String msg) {
        responseHandler.handle(new FailedDubboProxyResponse(msg));
    }
}
