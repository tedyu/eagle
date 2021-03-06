/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.jpm.util.resourcefetch.ha;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.eagle.jpm.util.Constants;
import org.apache.eagle.jpm.util.resourcefetch.connection.InputStreamUtils;
import org.apache.eagle.jpm.util.resourcefetch.url.RmActiveTestURLBuilderImpl;
import org.apache.eagle.jpm.util.resourcefetch.url.ServiceURLBuilder;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class HAURLSelectorImpl implements HAURLSelector {

    private final String[] urls;
    private volatile String selectedUrl;
    private final ServiceURLBuilder builder;

    private volatile boolean reselectInProgress;
    private final Constants.CompressionType compressionType;
    private static final long MAX_RETRY_TIME = 2;
    private static final Logger LOG = LoggerFactory.getLogger(HAURLSelectorImpl.class);
    private static ObjectMapper OBJ_MAPPER = new ObjectMapper();

    public HAURLSelectorImpl(String[] urls, Constants.CompressionType compressionType) {
        this.urls = urls;
        this.compressionType = compressionType;
        this.builder = new RmActiveTestURLBuilderImpl();
    }

    public void checkUrl() throws IOException {
        if (!checkUrl(builder.build(getSelectedUrl()))) {
            reSelectUrl();
        }
    }

    public boolean checkUrl(String urlString) {
        InputStream is = null;
        try {
            LOG.info("checking resource manager HA by {}", urlString);
            is = InputStreamUtils.getInputStream(urlString, null, compressionType);
        } catch (Exception ex) {
            LOG.info("fail to get inputStream from {}", urlString);
            return false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOG.warn("{}", e);
                }
            }
        }
        return true;
    }

    @Override
    public String getSelectedUrl() {
        if (selectedUrl == null) {
            selectedUrl = urls[0];
        }
        return selectedUrl;
    }

    private void reSelectUrl() throws IOException {
        if (reselectInProgress) {
            return;
        }
        synchronized (this) {
            if (reselectInProgress) {
                return;
            }
            reselectInProgress = true;
            try {
                LOG.info("Going to reselect url");
                for (int i = 0; i < urls.length; i++) {
                    String urlToCheck = urls[i];
                    for (int time = 0; time < MAX_RETRY_TIME; time++) {
                        if (checkUrl(builder.build(urlToCheck))) {
                            selectedUrl = urls[i];
                            LOG.info("Successfully switch to new url : " + selectedUrl);
                            return;
                        }
                        LOG.info("try {} failed for {} times, sleep 5 seconds before try again. ", urlToCheck, time + 1);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            LOG.warn("{}", ex);
                        }
                    }
                }
                throw new IOException("No alive url found: " + StringUtils.join(";", Arrays.asList(this.urls)));
            } finally {
                reselectInProgress = false;
            }
        }
    }
}