package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import static com.appdynamics.extensions.couchbase.utils.Constants.CLUSTER_NODES_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class ClusterAndNodeMetrics implements Runnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ClusterAndNodeMetrics.class);
    private MonitorConfiguration configuration;
    private CloseableHttpClient httpClient;
    private String serverURL;
    private CountDownLatch countDownLatch;

    public ClusterAndNodeMetrics(MonitorConfiguration configuration, CloseableHttpClient httpClient, String serverURl, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.serverURL = serverURl;
        this.countDownLatch = countDownLatch;
    }

    public void run(){
       List<Metric> clusterAndNodeMetrics = gatherClusterAndNodeMetrics();
       configuration.getMetricWriter().transformAndPrintNodeLevelMetrics(clusterAndNodeMetrics);
       countDownLatch.countDown();
    }

    private List<Metric> gatherClusterAndNodeMetrics(){
        List<Metric> clusterAndNodeMetrics = Lists.newArrayList();
        Map<String, ?> metricsMap = (Map<String, ?>)configuration.getConfigYml().get("metrics");
        Map<String, ?> clusterMap = (Map<String, ?>)metricsMap.get("cluster");
        Map<String, ?> nodeMap = (Map<String, ?>)metricsMap.get("node");
        JsonNode clusterJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + CLUSTER_NODES_ENDPOINT, JsonNode.class);
        JsonNode nodesJsonNode = clusterJsonNode.get("nodes");
        //#TODO take care of the metricPath
        clusterAndNodeMetrics.addAll(getClusterMetrics("", clusterMap, clusterJsonNode));
        clusterAndNodeMetrics.addAll(getNodeMetrics("", nodeMap, nodesJsonNode));
        return clusterAndNodeMetrics;
    }

    private List<Metric> getClusterMetrics(String metricPath, Map<String, ?> clusterMap, JsonNode clusterJsonNode){
        JsonNode storageNode = clusterJsonNode.get("storageTotals");
        List<Map<String, ?>> ramMetricsList = (List<Map<String, ?>>)clusterMap.get("ram");
        List<Map<String, ?>> hddMetricsList = (List<Map<String, ?>>)clusterMap.get("hdd");
        List<Map<String, ?>> countersMetricsList = (List<Map<String, ?>>)clusterMap.get("counters");
        List<Map<String, ?>> otherMetricsList = (List<Map<String, ?>>)clusterMap.get("otherMetrics");
        List<Metric> metricList = Lists.newArrayList();
        metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "ram",ramMetricsList, storageNode.get("ram")));
        metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "hdd", hddMetricsList, storageNode.get("hdd")));
        metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "counters", countersMetricsList, clusterJsonNode.get("counters")));
        metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "otherMetrics", otherMetricsList, clusterJsonNode));

        return metricList;
    }

    private List<Metric> getNodeMetrics(String metricPath, Map<String, ?> nodeMap, JsonNode jsonNode){
        List<Map<String, ?>> systemStatsList = (List<Map<String, ?>>)nodeMap.get("systemStats");
        List<Map<String, ?>> interestingStatsList = (List<Map<String, ?>>)nodeMap.get("interestingStats");
        List<Map<String, ?>> otherStatsList = (List<Map<String, ?>>)nodeMap.get("otherStats");
        List<Metric> metricList = Lists.newArrayList();
        for(JsonNode node : jsonNode){
            String nodeName = node.get("hostname").getTextValue();
            metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "Nodes" + nodeName + "systemStats",systemStatsList, node.get("systemStats")));
            metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "Nodes" + nodeName + "interestingStats", interestingStatsList, node.get("interestingStats")));
            metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "Nodes" + nodeName + "otherStats", otherStatsList, node));

        }
        return metricList;
    }

    private List<Metric> getMetrics(String metricPath, List<Map<String, ?>> metricsList, JsonNode jsonNode){
        List<Metric> metricList = Lists.newArrayList();
        for(Map<String, ?> metric : metricsList){
            String metricName = metric.entrySet().iterator().next().getKey();
            Map<String, ?> metricProperties = (Map<String, ?>)metric.entrySet().iterator().next().getValue();
            JsonNode jsonValue = jsonNode.get(metricName);
            if(jsonNode.has(metricName) && !jsonValue.isNull() && jsonValue.isValueNode()){
                Metric individualMetric = new Metric(metricName, jsonValue.getTextValue(), metricPath, metricProperties);
                metricList.add(individualMetric);
            }
        }
        return metricList;
    }
}
