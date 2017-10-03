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

import static com.appdynamics.extensions.couchbase.utils.Constants.INDIVIDUAL_BUCKET_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.Constants.getMetricsFromArray;

/**
 * Created by venkata.konala on 9/20/17.
 */
class IndividualBucketMetrics implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(IndividualBucketMetrics.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String bucketName;
    private String serverURL;
    private Map<String, ?> bucketMap;
    private CountDownLatch latch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;

    IndividualBucketMetrics(MonitorConfiguration configuration, String clusterName, String bucketName, String serverURL, Map<String, ?> bucketMap, CountDownLatch latch){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.bucketName = bucketName;
        this.serverURL = serverURL;
        this.bucketMap = bucketMap;
        this.latch = latch;
        this.httpClient = this.configuration.getHttpClient();
        this.metricWriteHelper = this.configuration.getMetricWriter();
    }

    public void run(){
        List<Metric> individualBucketMetricsList = gatherIndividualBucketMetrics();
        metricWriteHelper.transformAndPrintNodeLevelMetrics(individualBucketMetricsList);
        latch.countDown();
    }

    private List<Metric> gatherIndividualBucketMetrics(){
        List<Metric> individualBucketMetricsList = Lists.newArrayList();
        List<Map<String, ?>> otherBucketMetricsList = (List<Map<String, ?>>)bucketMap.get("otherStats");
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, String.format(serverURL + INDIVIDUAL_BUCKET_ENDPOINT, bucketName), JsonNode.class);
        if(rootJsonNode != null) {
            JsonNode opJsonNode = rootJsonNode.get("op");
            if (opJsonNode != null) {
                JsonNode sampleJsonNode = opJsonNode.get("samples");
                //#TODO take care of the metricPath
                individualBucketMetricsList.addAll(getMetricsFromArray(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + "buckets" + METRIC_SEPARATOR + bucketName, otherBucketMetricsList, sampleJsonNode));
            }
        }
        return individualBucketMetricsList;
    }
}
