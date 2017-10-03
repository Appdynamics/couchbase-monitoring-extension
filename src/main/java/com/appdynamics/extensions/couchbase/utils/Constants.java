package com.appdynamics.extensions.couchbase.utils;

import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.codehaus.jackson.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Constants {
    public static final String DEFAULT_METRIC_PREFIX = "Custom metrics|CouchBase|";
    public static final String METRIC_SEPARATOR  = "|";
    public static final String CLUSTER_NODES_ENDPOINT = "/pools/default";
    public static final String BUCKETS_ENDPOINT = "/pools/default/buckets";
    public static final String INDIVIDUAL_BUCKET_ENDPOINT = "/pools/default/buckets/%s/stats?zoom=day";
    public static final String QUERY_SERVICE_URL = "http://%s:%d/admin/vitals";
    public static final String TASKS_ENDPOINT = "/pools/default/tasks";
    public static final String INDIVIDUAL_BUCKET_WITHOUT_ZOOM_ENDPPOINT = "/pools/default/buckets/%s/stats/";
    public static final String XDCR_ENDPOINT = "replications/%s/%s/%s";
    public static final String INDEX_ENDPOINT = "/settings/indexes";

    public static List<Metric> getClusterMetrics(String metricPath, Map<String, ?> clusterMap, JsonNode clusterJsonNode, Set<String> sectionSet){
        List<Metric> metricList = Lists.newArrayList();
        for (String sectionName: sectionSet){
            List<Map<String, ?>> sectionMetricsList = (List<Map<String, ?>>)clusterMap.get(sectionName);
            if(sectionMetricsList != null) {
                metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + sectionName, sectionMetricsList, clusterJsonNode.get(sectionName) == null ? clusterJsonNode : clusterJsonNode.get(sectionName)));
            }
        }
        return metricList;
    }

    public static List<Metric> getNodeOrBucketMetrics(String metricPath, Map<String, ?> nodeMap, JsonNode jsonNode, Set<String> sectionSet, Set<String> nodeSet){
        List<Metric> metricList = Lists.newArrayList();
        for(String section : sectionSet){
            List<Map<String, ?>> systemStatsList = (List<Map<String, ?>>)nodeMap.get(section);
            for(JsonNode node : jsonNode){
                String nodeOrBucketname = node.get("hostname") == null ? (node.get("name") == null ? null  : node.get("name").asText()) : node.get("hostname").asText();
                nodeSet.add(nodeOrBucketname);
                if(systemStatsList != null) {
                    metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + nodeOrBucketname + METRIC_SEPARATOR + section, systemStatsList, node.get(section) == null ? node : node.get(section)));
                }
            }
        }
        return metricList;
    }

    public static List<Metric> getMetrics(String metricPath, List<Map<String, ?>> metricsList, JsonNode jsonNode){
        List<Metric> metricList = Lists.newArrayList();
        for(Map<String, ?> metric : metricsList){
            String metricName = metric.entrySet().iterator().next().getKey();
            Map<String, ?> metricProperties = (Map<String, ?>)metric.entrySet().iterator().next().getValue();
            JsonNode jsonValue = jsonNode.get(metricName);
            if(jsonNode.has(metricName) && !jsonValue.isNull() && jsonValue.isValueNode()){
                Metric individualMetric;
                if(metricProperties != null){
                    individualMetric = new Metric(metricName, jsonValue.asText(), metricPath + METRIC_SEPARATOR + metricName, metricProperties);
                }
                else{
                    individualMetric = new Metric(metricName, jsonValue.asText(), metricPath + METRIC_SEPARATOR + metricName);
                }
                metricList.add(individualMetric);
            }
        }
        return metricList;
    }

    public static List<Metric> getMetricsFromArray(String metricPath, List<Map<String, ?>> metricsList, JsonNode jsonNode){
        List<Metric> metricList = Lists.newArrayList();
        for(Map<String, ?> metric : metricsList){
            String metricName = metric.entrySet().iterator().next().getKey();
            Map<String, ?> metricProperties = (Map<String, ?>)metric.entrySet().iterator().next().getValue();
            JsonNode jsonValue = jsonNode.get(metricName);
            if(!jsonValue.isNull() && jsonValue.isArray()){
                JsonNode lastValueNode = jsonValue.get(jsonValue.size() - 1);
                Metric individualMetric;
                if(metricProperties != null){
                    individualMetric = new Metric(metricName, lastValueNode.asText(), metricPath + METRIC_SEPARATOR + metricName, metricProperties);
                }
                else{
                    individualMetric = new Metric(metricName, lastValueNode.asText(), metricPath + METRIC_SEPARATOR + metricName);
                }
                metricList.add(individualMetric);
            }
        }
        return metricList;
    }
}
