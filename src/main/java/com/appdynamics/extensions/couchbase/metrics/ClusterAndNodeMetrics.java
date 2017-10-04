package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.*;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class ClusterAndNodeMetrics implements Runnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ClusterAndNodeMetrics.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> clusterMap;
    private Map<String, ?> nodeMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private Set<String> nodesSet;

    public ClusterAndNodeMetrics(MonitorConfiguration configuration, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.clusterMap = (Map<String, ?>)metricsMap.get("cluster");
        this.nodeMap = (Map<String, ?>)metricsMap.get("nodes");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
        this.metricWriteHelper = this.configuration.getMetricWriter();
        nodesSet = Sets.newHashSet();
    }

    public void run(){
       List<Metric> clusterAndNodeMetrics = gatherClusterAndNodeMetrics();
       metricWriteHelper.transformAndPrintNodeLevelMetrics(clusterAndNodeMetrics);
       countDownLatch.countDown();
    }

    private List<Metric> gatherClusterAndNodeMetrics(){
        List<Metric> clusterAndNodeMetrics = Lists.newArrayList();
        JsonNode clusterJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + CLUSTER_NODES_ENDPOINT, JsonNode.class);
        JsonNode nodesJsonNode = clusterJsonNode.get("nodes");
        JsonNode storageNode = clusterJsonNode.get("storageTotals");

        if(clusterMap != null && clusterMap.get("include") != null && clusterMap.get("include").toString().equalsIgnoreCase("true")) {

            Set<String> clusterSectionSet = Sets.newHashSet();
            clusterSectionSet.add("counters");
            clusterSectionSet.add("others");
            clusterAndNodeMetrics.addAll(getClusterMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "cluster", clusterMap, clusterJsonNode, clusterSectionSet));

            Set<String> storageSectionSet = Sets.newHashSet();
            storageSectionSet.add("ram");
            storageSectionSet.add("hdd");
            clusterAndNodeMetrics.addAll(getClusterMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "cluster", clusterMap, storageNode, storageSectionSet));
        }
        else{
            logger.debug("The metrics in 'cluster' section are not processed either because it is not present (or)'include' parameter is either null or false");
        }

        if(nodeMap != null && nodeMap.get("include") != null && nodeMap.get("include").toString().equalsIgnoreCase("true")) {

            Set<String> nodeSectionSet = Sets.newHashSet();
            nodeSectionSet.add("systemStats");
            nodeSectionSet.add("interestingStats");
            nodeSectionSet.add("otherStats");
            clusterAndNodeMetrics.addAll(getNodeOrBucketMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "nodes", nodeMap, nodesJsonNode, nodeSectionSet, nodesSet));
        }
        else{
            logger.debug("The metrics in 'node' section are not processed either because it is not present (or) 'include' parameter is either null or false");
        }

        return clusterAndNodeMetrics;
    }
}
