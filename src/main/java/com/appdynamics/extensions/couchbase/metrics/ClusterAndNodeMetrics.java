/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.CLUSTER_NODES_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.JsonUtils.getClusterMetrics;
import static com.appdynamics.extensions.couchbase.utils.JsonUtils.getNodeOrBucketMetrics;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class ClusterAndNodeMetrics implements Runnable {

    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(ClusterAndNodeMetrics.class);
    private MonitorContextConfiguration contextConfiguration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> clusterMap;
    private Map<String, ?> nodeMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private Set<String> nodesSet;

    public ClusterAndNodeMetrics(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch) {
        this.contextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.clusterMap = (Map<String, ?>) metricsMap.get("cluster");
        this.nodeMap = (Map<String, ?>) metricsMap.get("nodes");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.contextConfiguration.getContext().getHttpClient();
        nodesSet = Sets.newHashSet();
    }

    public void run() {
        //Since the main task is waiting on this subtask to finish, make sure that the latch is counted down no matter what.
        //If there is an error in the subtask and it ends without counting down the latch, the main task will wait for the
        //latch forever. This will stall the DerivedMetricsCalculation and also the main task will wait in the scheduler queue
        //forever creating inconsistencies. So make sure the latch is counted down by using finally block.
        try {
            List<Metric> clusterAndNodeMetrics = gatherClusterAndNodeMetrics();
            metricWriteHelper.transformAndPrintMetrics(clusterAndNodeMetrics);
        } catch (Exception e) {
            logger.error("Caught an exception while fetching cluster and node metrics : ", e);
        } finally {
            countDownLatch.countDown();
        }
    }

    private List<Metric> gatherClusterAndNodeMetrics() {
        List<Metric> clusterAndNodeMetrics = Lists.newArrayList();
        JsonNode clusterJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + CLUSTER_NODES_ENDPOINT, JsonNode.class);
        JsonNode nodesJsonNode = clusterJsonNode.get("nodes");
        JsonNode storageNode = clusterJsonNode.get("storageTotals");

        if (clusterMap != null && clusterMap.get("include") != null && clusterMap.get("include").toString().equalsIgnoreCase("true")) {

            Set<String> clusterSectionSet = Sets.newHashSet();
            clusterSectionSet.add("counters");
            clusterSectionSet.add("others");
            clusterAndNodeMetrics.addAll(getClusterMetrics(contextConfiguration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "cluster", clusterMap, clusterJsonNode, clusterSectionSet));

            Set<String> storageSectionSet = Sets.newHashSet();
            storageSectionSet.add("ram");
            storageSectionSet.add("hdd");
            clusterAndNodeMetrics.addAll(getClusterMetrics(contextConfiguration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "cluster", clusterMap, storageNode, storageSectionSet));
        } else {
            logger.debug("The metrics in 'cluster' section are not processed either because it is not present (or)'include' parameter is either null or false");
        }

        if (nodeMap != null && nodeMap.get("include") != null && nodeMap.get("include").toString().equalsIgnoreCase("true")) {

            Set<String> nodeSectionSet = Sets.newHashSet();
            nodeSectionSet.add("systemStats");
            nodeSectionSet.add("interestingStats");
            nodeSectionSet.add("otherStats");
            clusterAndNodeMetrics.addAll(getNodeOrBucketMetrics(contextConfiguration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "nodes", nodeMap, nodesJsonNode, nodeSectionSet, nodesSet));
        } else {
            logger.debug("The metrics in 'node' section are not processed either because it is not present (or) 'include' parameter is either null or false");
        }

        return clusterAndNodeMetrics;
    }
}
