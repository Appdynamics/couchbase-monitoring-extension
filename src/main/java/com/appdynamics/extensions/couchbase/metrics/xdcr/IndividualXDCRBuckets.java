/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics.xdcr;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.AssertUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;

/**
 * Created by venkata.konala on 10/3/17.
 */
public class IndividualXDCRBuckets implements Runnable {

    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(IndividualXDCRBuckets.class);
    private MonitorContextConfiguration contextConfiguration;
    private String clusterName;
    private String serverURl;
    private JsonNode xdcrBucketNode;
    private List<Map<String, ?>> xdcrMetricsList;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private MonitorExecutorService executorService;

    IndividualXDCRBuckets(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, JsonNode xdcrBucketNode, List<Map<String, ?>> xdcrMetricsList, CountDownLatch countDownLatch) {
        this.contextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.serverURl = serverURL;
        this.xdcrBucketNode = xdcrBucketNode;
        AssertUtils.assertNotNull(xdcrMetricsList, "The stats section under xdcr group is either null or empty");
        this.xdcrMetricsList = xdcrMetricsList;
        this.countDownLatch = countDownLatch;
        this.httpClient = this.contextConfiguration.getContext().getHttpClient();
        this.executorService = this.contextConfiguration.getContext().getExecutorService();
    }

    public void run() {
        try {
            gatherAndPrintBucketXDCRMetrics();
        } catch (Exception e) {
            logger.debug("Exception while gathering metrics for Individual XDCR bucket : ", e);

        } finally {
            countDownLatch.countDown();
        }
    }

    private void gatherAndPrintBucketXDCRMetrics() {
        String id = xdcrBucketNode.get("id").asText();
        String idSplit[] = id.split("/");
        String remote_uuid = idSplit[0];
        String bucketName = idSplit[1];
        String destinationName = idSplit[2];

        List<Metric> xdcrStatusMetrics = Lists.newArrayList();
        String status = xdcrBucketNode.get("status").textValue();
        Metric statusMetric = new Metric("status", status.equalsIgnoreCase("running") ? "1" : "0", contextConfiguration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "xdcr" + METRIC_SEPARATOR + bucketName + METRIC_SEPARATOR + "status");
        logger.debug(String.format("The status metric for bucket {%s} is retrieved as {%s}", bucketName, status));
        xdcrStatusMetrics.add(statusMetric);

        CountDownLatch latch = new CountDownLatch(xdcrMetricsList.size());
        for (Map<String, ?> metric : xdcrMetricsList) {
            IndividualXDCRMetric individualXDCRMetricTask = new IndividualXDCRMetric(contextConfiguration, metricWriteHelper, clusterName, serverURl, metric, remote_uuid, bucketName, destinationName, latch);
            executorService.submit(id + " " + metric.entrySet().iterator().next().getKey(), individualXDCRMetricTask);
        }
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            logger.debug(String.format("The latch for Individual XDCR metrics in the bucket %s is interrupted", bucketName));
        } finally {
            logger.debug("The status metric is sent for printing");
            metricWriteHelper.transformAndPrintMetrics(xdcrStatusMetrics);
        }
    }
}
