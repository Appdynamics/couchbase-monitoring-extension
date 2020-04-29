/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.couchbase.metrics.buckets.BucketMetrics;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.INDEX_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.JsonUtils.getMetrics;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class IndexMetrics implements Runnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(BucketMetrics.class);
    private MonitorContextConfiguration contextConfiguration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> indexMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;

    public IndexMetrics(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch) {
        this.contextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.indexMap = (Map<String, ?>) metricsMap.get("index");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.contextConfiguration.getContext().getHttpClient();
    }

    public void run() {
        try {
            if (indexMap != null && indexMap.get("include") != null && indexMap.get("include").toString().equalsIgnoreCase("true")) {
                List<Metric> indexMetrics = gatherIndexMetrics();
                metricWriteHelper.transformAndPrintMetrics(indexMetrics);
            } else {
                logger.debug("The metrics in 'index' section are not processed either because it is not present (or) 'include' parameter is either null or false");
            }
        } catch (Exception e) {
            logger.error("Caught an exception while gathering Index metrics : ", e);
        } finally {
            countDownLatch.countDown();
        }
    }

    private List<Metric> gatherIndexMetrics() {
        List<Metric> bucketMetrics = Lists.newArrayList();
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + INDEX_ENDPOINT, JsonNode.class);
        List<Map<String, ?>> metricsList = (List<Map<String, ?>>) indexMap.get("stats");
        bucketMetrics.addAll(getMetrics(contextConfiguration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "index", metricsList, rootJsonNode));
        return bucketMetrics;
    }
}
