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
package io.shulie.tesla.gravitee.plugins.duboo;

import com.alibaba.fastjson.JSONObject;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.handlers.api.context.ApiTemplateVariableProvider;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.handler.context.ReactableExecutionContext;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: DubboPolicy
 * @author: wangjian
 * @Date: 2020/3/9 21:09
 * @Description:
 */
@SuppressWarnings("unused")
public class DubboPolicy {

    private DubboPolicyConfiguration configuration;

    public DubboPolicy(DubboPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {
        Api api = ((ApiTemplateVariableProvider) ((ArrayList) ((ReactableExecutionContext) executionContext).getProviders()).get(2)).getApi();
        Map<String, Path> paths = api.getPaths();
        for (Map.Entry<String, Path> stringPathEntry : paths.entrySet()) {
            Path value = stringPathEntry.getValue();
            List<Rule> rules = value.getRules();
            rules.forEach(item -> {
                String configuration = item.getPolicy().getConfiguration();
                this.configuration = JSONObject.parseObject(configuration, DubboPolicyConfiguration.class);
            });
        }
        executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, new DubboInvoker(configuration));

        // Finally continue chaining
        policyChain.doNext(request, response);
    }
}
