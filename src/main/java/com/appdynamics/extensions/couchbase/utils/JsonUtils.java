/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.utils;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.MetricPathUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;

/**
 * Created by venkata.konala on 10/5/17.
 */
public class JsonUtils {

    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(JsonUtils.class);

    public static List<Metric> getClusterMetrics(String metricPath, Map<String, ?> clusterMap, JsonNode clusterJsonNode, Set<String> sectionSet) {
        List<Metric> metricList = Lists.newArrayList();
        for (String sectionName : sectionSet) {
            List<Map<String, ?>> sectionMetricsList = (List<Map<String, ?>>) clusterMap.get(sectionName);
            if (sectionMetricsList != null) {
                metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + sectionName, sectionMetricsList, clusterJsonNode.get(sectionName) == null ? clusterJsonNode : clusterJsonNode.get(sectionName)));
            } else {
                logger.debug("The {} section does not have any metrics specified in config.yml", sectionName);
            }
        }
        return metricList;
    }

    public static List<Metric> getNodeOrBucketMetrics(String metricPath, Map<String, ?> nodeMap, JsonNode jsonNode, Set<String> sectionSet, Set<String> nodeSet) {
        List<Metric> metricList = Lists.newArrayList();
        for (String section : sectionSet) {
            List<Map<String, ?>> sectionMetricsList = (List<Map<String, ?>>) nodeMap.get(section);
            for (JsonNode node : jsonNode) {
                String nodeOrBucketname = node.get("hostname") == null ? (node.get("name") == null ? null : node.get("name").asText()) : node.get("hostname").asText();
                nodeSet.add(nodeOrBucketname);
                if (sectionMetricsList != null) {
                    String path = MetricPathUtils.buildMetricPath(metricPath,nodeOrBucketname);
                    metricList.addAll(getMetrics(path + METRIC_SEPARATOR + section, sectionMetricsList, node.get(section) == null ? node : node.get(section)));
                } else {
                    logger.debug("The {} section does not have any metrics specified in config.yml", section);
                }
            }
        }
        return metricList;
    }

    public static List<Metric> getMetrics(String metricPath, List<Map<String, ?>> metricsList, JsonNode jsonNode) {
        AssertUtils.assertNotNull(metricsList, "The metricsList passed is either null or empty");
        AssertUtils.assertNotNull(jsonNode, "The jsonNode passed is either null or empty");
        List<Metric> metricList = Lists.newArrayList();
        for (Map<String, ?> metric : metricsList) {
            String metricName = metric.entrySet().iterator().next().getKey();
            Map<String, ?> metricProperties = (Map<String, ?>) metric.entrySet().iterator().next().getValue();
            JsonNode jsonValue = jsonNode.get(metricName);
            if (jsonValue != null && jsonValue.isValueNode()) {
                Metric individualMetric;
                if (metricProperties != null) {
                    individualMetric = new Metric(metricName, jsonValue.asText(), metricPath + METRIC_SEPARATOR + metricName, metricProperties);
                } else {
                    individualMetric = new Metric(metricName, jsonValue.asText(), metricPath + METRIC_SEPARATOR + metricName);
                }
                metricList.add(individualMetric);
            } else {
                logger.debug("Value for {} does not exist", metricName);
            }
        }
        return metricList;
    }

    public static List<Metric> getMetricsFromArray(String metricPath, String bucketName, List<Map<String, ?>> metricsList, JsonNode jsonNode) {
        AssertUtils.assertNotNull(metricsList, "The metricsList passed is either null or empty");
        AssertUtils.assertNotNull(jsonNode, "The jsonNode passed is either null or empty");
        List<Metric> metricList = Lists.newArrayList();
        for (Map<String, ?> metric : metricsList) {
            String metricName = metric.entrySet().iterator().next().getKey();
            Map<String, ?> metricProperties = (Map<String, ?>) metric.entrySet().iterator().next().getValue();
            JsonNode jsonValue = jsonNode.get(metricName);
            if (jsonValue != null && jsonValue.isArray()) {
                JsonNode lastValueNode = jsonValue.get(jsonValue.size() - 1);
                Metric individualMetric;
                String path = MetricPathUtils.buildMetricPath(metricPath,bucketName);
                if (metricProperties != null) {
                    individualMetric = new Metric(metricName, lastValueNode.asText(), path + METRIC_SEPARATOR + metricName, metricProperties);
                } else {
                    individualMetric = new Metric(metricName, lastValueNode.asText(), path + METRIC_SEPARATOR + metricName);
                }
                metricList.add(individualMetric);
            } else {
                logger.debug("Value for {} does not exist", metricName);
            }
        }
        return metricList;
    }
}
