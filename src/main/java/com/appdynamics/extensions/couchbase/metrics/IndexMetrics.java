package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.couchbase.metrics.buckets.BucketMetrics;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.*;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class IndexMetrics implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BucketMetrics.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> indexMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;

    public IndexMetrics(MonitorConfiguration configuration, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.indexMap = (Map<String, ?>) metricsMap.get("index");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
        this.metricWriteHelper = this.configuration.getMetricWriter();
    }

    public void run(){
        if(indexMap != null && indexMap.get("include") != null && indexMap.get("include").toString().equalsIgnoreCase("true")) {
            List<Metric> indexMetrics = gatherIndexMetrics();
            metricWriteHelper.transformAndPrintNodeLevelMetrics(indexMetrics);
        }
        else{
            logger.debug("The metrics in 'index' section are not processed either because it is not present (or) 'include' parameter is either null or false");
        }
        countDownLatch.countDown();
    }

    private List<Metric> gatherIndexMetrics(){
        List<Metric> bucketMetrics = Lists.newArrayList();
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + INDEX_ENDPOINT, JsonNode.class);
        List<Map<String, ?>> metricsList = (List<Map<String, ?>>)indexMap.get("stats");
        bucketMetrics.addAll(getMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "index", metricsList, rootJsonNode));
        return bucketMetrics;
    }
}
