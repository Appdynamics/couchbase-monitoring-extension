package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
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

import static com.appdynamics.extensions.couchbase.utils.Constants.INDEX_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.Constants.getMetrics;

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
        List<Metric> bucketMetrics = gatherBucketMetrics();
        metricWriteHelper.transformAndPrintNodeLevelMetrics(bucketMetrics);
        countDownLatch.countDown();
    }

    private List<Metric> gatherBucketMetrics(){
        List<Metric> bucketMetrics = Lists.newArrayList();
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + INDEX_ENDPOINT, JsonNode.class);
        List<Map<String, ?>> metricsList = (List<Map<String, ?>>)indexMap.get("stats");
        //#TODO take care of the metricPath
        bucketMetrics.addAll(getMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "index", metricsList, rootJsonNode));
        return bucketMetrics;
    }
}
