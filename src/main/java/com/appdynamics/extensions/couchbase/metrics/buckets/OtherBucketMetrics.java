/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics.buckets;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
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

import static com.appdynamics.extensions.couchbase.utils.Constants.INDIVIDUAL_BUCKET_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.JsonUtils.getMetricsFromArray;

/**
 * Created by venkata.konala on 9/20/17.
 */
class OtherBucketMetrics implements Runnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(OtherBucketMetrics.class);
    private MonitorContextConfiguration contextConfiguration;
    private String clusterName;
    private String bucketName;
    private String serverURL;
    private Map<String, ?> bucketMap;
    private CountDownLatch latch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;

    OtherBucketMetrics(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper, String clusterName, String bucketName, String serverURL, Map<String, ?> bucketMap, CountDownLatch latch) {
        this.contextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.bucketName = bucketName;
        this.serverURL = serverURL;
        this.bucketMap = bucketMap;
        this.latch = latch;
        this.httpClient = this.contextConfiguration.getContext().getHttpClient();
    }

    public void run() {
        try {
            List<Metric> individualBucketMetricsList = gatherIndividualBucketMetrics();
            metricWriteHelper.transformAndPrintMetrics(individualBucketMetricsList);
        } catch (Exception e) {
            logger.error("Something unforeseen happened", e);
        } finally {
            latch.countDown();
        }

    }

    private List<Metric> gatherIndividualBucketMetrics() {
        List<Metric> individualBucketMetricsList = Lists.newArrayList();
        List<Map<String, ?>> otherBucketMetricsList = (List<Map<String, ?>>) bucketMap.get("otherStats");
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, String.format(serverURL + INDIVIDUAL_BUCKET_ENDPOINT, bucketName), JsonNode.class);
        if (rootJsonNode != null) {
            JsonNode opJsonNode = rootJsonNode.get("op");
            if (opJsonNode != null) {
                JsonNode sampleJsonNode = opJsonNode.get("samples");
                individualBucketMetricsList.addAll(getMetricsFromArray(contextConfiguration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "buckets", bucketName, otherBucketMetricsList, sampleJsonNode));
            }
        }
        return individualBucketMetricsList;
    }
}
