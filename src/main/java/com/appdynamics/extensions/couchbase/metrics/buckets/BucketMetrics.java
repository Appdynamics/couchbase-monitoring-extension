/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics.buckets;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Sets;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class BucketMetrics implements Runnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(BucketMetrics.class);
    private MonitorContextConfiguration contextConfiguration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> bucketMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private MonitorExecutorService executorService;
    private Set<String> bucketsSet;
    private BucketMetricsProcessor bucketMetricsProcessor;

    public BucketMetrics(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch, BucketMetricsProcessor bucketMetricsProcessor) {
        this.contextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.bucketMap = (Map<String, ?>) metricsMap.get("buckets");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.contextConfiguration.getContext().getHttpClient();
        this.executorService = this.contextConfiguration.getContext().getExecutorService();
        this.bucketsSet = Sets.newHashSet();
        this.bucketMetricsProcessor = bucketMetricsProcessor;
    }

    public void run() {
        try {
            if (bucketMap != null && bucketMap.get("include") != null && bucketMap.get("include").toString().equalsIgnoreCase("true")) {
                List<Metric> bucketMetrics = bucketMetricsProcessor.gatherBucketMetrics(contextConfiguration, httpClient, clusterName, serverURL, bucketMap, bucketsSet);
                metricWriteHelper.transformAndPrintMetrics(bucketMetrics);
                bucketMetricsProcessor.getIndividualBucketMetrics(contextConfiguration, metricWriteHelper, clusterName, serverURL, bucketMap, bucketsSet);
            } else {
                logger.debug("The metrics in 'buckets' section are not processed either because it is not present (or) 'include' parameter is either null or false");
            }
        } catch (Exception e) {
            logger.error("Caught an exception while fetching bucket metrics : ", e);
        } finally {
            countDownLatch.countDown();
        }
    }
}
