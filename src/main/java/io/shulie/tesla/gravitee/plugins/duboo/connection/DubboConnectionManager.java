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

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import io.netty.util.internal.StringUtil;
import io.shulie.tesla.gravitee.plugins.duboo.DubboPolicyConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @ClassName: DubboConnectionManager
 * @author: wangjian
 * @Date: 2020/3/9 19:49
 * @Description:
 */
public class DubboConnectionManager {


    private static ApplicationConfig applicationConfig;

    static {
        applicationConfig = new ApplicationConfig();
        applicationConfig.setName("gravitee.gateway");
        applicationConfig.setOwner("gravitee.gateway");
        applicationConfig.setOrganization("shulie");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboConnectionManager.class);

    private Map<DubboPolicyConfiguration, RegistryConfig> registryMap = new HashMap<>();

    private Map<String, ReferenceConfig<GenericService>> referenceConfigMap = new HashMap<>();

    private static final DubboConnectionManager instance = new DubboConnectionManager();

    private DubboConnectionManager() {
        LOGGER.info("DubboConnectionManager is created!");
    }

    public static DubboConnectionManager getInstance() {
        return instance;
    }

    public void addConfiguration(Vertx vertx, DubboPolicyConfiguration configuration) {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress(configuration.getAddress());
        registryConfig.setPort(configuration.getPort());
        registryConfig.setProtocol(configuration.getProtocol());
        registryConfig.setCheck(configuration.getCheck());
        registryConfig.setUsername(configuration.getUsername());
        registryConfig.setPassword(configuration.getPassword());
        registryConfig.setGroup(configuration.getGroup());
        registryConfig.setVersion(configuration.getVersion());
        registryMap.put(configuration, registryConfig);
    }

    public void requestDubbo(DubboPolicyConfiguration configuration,
                             URI dubboUri,
                             Properties dubboParams,
                             Handler<AsyncResult<ReferenceConfig<GenericService>>> connectionHandler) {
        if (referenceConfigMap.containsKey(dubboUri.getPath())) {
            connectionHandler.handle(Future.succeededFuture(referenceConfigMap.get(dubboUri.getPath())));
        } else {
            ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
            referenceConfig.setApplication(applicationConfig);
            referenceConfig.setGeneric(true);
            referenceConfig.setRegistry(registryMap.get(configuration));
            referenceConfig.setProtocol("dubbo");
            referenceConfig.setRetries(0);
            String anInterface = dubboUri.getPath().substring(1);
            referenceConfig.setInterface(anInterface);
            referenceConfig.setId(anInterface);
            String version = dubboParams.getProperty("version");
            if (!StringUtil.isNullOrEmpty(version)) {
                referenceConfig.setVersion(version);
            }
            String group = dubboParams.getProperty("group");
            if (!StringUtil.isNullOrEmpty(group)) {
                referenceConfig.setGroup(group);
            }
            referenceConfigMap.put(dubboUri.getPath(), referenceConfig);
            connectionHandler.handle(Future.succeededFuture(referenceConfig));
        }
    }
}
