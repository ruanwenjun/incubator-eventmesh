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

package org.apache.eventmesh.common.config;

import org.apache.eventmesh.common.EventMeshRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class YamlConfigurationReader {

    private Logger logger = LoggerFactory.getLogger(YamlConfigurationReader.class);

    private Map<String, Object> configProperties;

    public YamlConfigurationReader(String yamlFilePath) throws IOException {
        loadAllConfig(yamlFilePath);
    }

    /**
     * get int property
     *
     * @param configKey    config key
     * @param defaultValue default value
     * @return if configKey is not exist, return the default value
     */
    public int getInt(String configKey, int defaultValue) {
        Object configValue = getProperty(configKey);
        if (configValue == null) {
            return defaultValue;
        }
        if (!(configValue instanceof Integer)) {
            throw new EventMeshRuntimeException(
                    String.format("configKey: %s, configValue: %s is invalid", configKey, configValue));
        }
        return (int) configValue;
    }

    /**
     * get boolean property
     *
     * @param configKey    config key
     * @param defaultValue default value
     * @return if config key is not exist, return the default value
     */
    public boolean getBool(String configKey, boolean defaultValue) {
        Object configValue = getProperty(configKey);
        if (configValue == null) {
            return defaultValue;
        }
        if (!(configValue instanceof Boolean)) {
            throw new EventMeshRuntimeException(
                    String.format("configKey: %s, configValue: %s is invalid", configKey, configValue)
            );
        }
        return Boolean.parseBoolean(configValue.toString());
    }

    /**
     * get string property
     *
     * @param configKey    config key
     * @param defaultValue default value
     * @return if configKey is not exist, return the default value
     */
    public String getString(String configKey, String defaultValue) {
        Object configValue = getProperty(configKey);
        if (configValue == null) {
            return defaultValue;
        }
        return configValue.toString();
    }

    /**
     * get string property
     *
     * @param configKey    config key
     * @param defaultValue default value
     * @return if configKey is not exist, return the default value
     */
    public List<String> getList(String configKey, List<String> defaultValue) {
        Object configValue = getProperty(configKey);
        if (!(configValue instanceof List)) {
            return defaultValue;
        }
        return (List<String>) configValue;
    }

    private Object getProperty(String configKey) {
        Map<String, Object> config = configProperties;
        String[] subKeys = configKey.split("\\.");
        for (int i = 0; i < subKeys.length - 1; i++) {
            if (config == null) {
                break;
            }
            Object subConfig = config.get(subKeys[i]);
            if (!(subConfig instanceof Map) && i != subKeys.length - 1) {
                config = null;
            } else {
                config = (Map<String, Object>) config.get(subKeys[i]);
            }
        }
        if (config == null) {
            return null;
        }
        return config.get(subKeys[subKeys.length - 1]);
    }

    private void loadAllConfig(String yamlFile) throws IOException {
        configProperties = loadConfigFromConf(yamlFile);
        if (configProperties == null) {
            configProperties = loadConfigFromClassPath(yamlFile);
        }
        if (configProperties == null) {
            throw new EventMeshRuntimeException(String.format("Yaml config file: %s is empty", yamlFile));
        }
    }

    private Map<String, Object> loadConfigFromClassPath(String yamlFile) throws IOException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(yamlFile);
        if (resource == null) {
            return null;
        }
        try (InputStreamReader inputStreamReader = new InputStreamReader(resource.openStream())) {
            Yaml yaml = new Yaml();
            return yaml.load(inputStreamReader);
        }
    }

    private Map<String, Object> loadConfigFromConf(String fileName) throws IOException {
        String confPath = System.getProperty("confPath", System.getenv("confPath"));
        String yamlConfigFilePath = confPath + File.separator + fileName;
        File file = new File(yamlConfigFilePath);
        if (!file.exists()) {
            return null;
        }
        try (FileReader fileReader = new FileReader(yamlConfigFilePath)) {
            Yaml yaml = new Yaml();
            return yaml.load(fileReader);
        }
    }

}