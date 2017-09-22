package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.BUCKETS_ENDPOINT;

/**
 * Created by venkata.konala on 9/20/17.
 */
class IndividualBucketMetrics implements Runnable {

    private MonitorConfiguration configuration;
    private CloseableHttpClient httpClient;
    private String serverURL;
    private CountDownLatch latch;

    IndividualBucketMetrics(MonitorConfiguration configuration, CloseableHttpClient httpClient, String serverURL, CountDownLatch latch){
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.serverURL = serverURL;
        this.latch = latch;
    }

    public void run() {
        List<Metric> individualBucketMetricsList = gatherIndividualBucketMetrics();
        configuration.getMetricWriter().transformAndPrintNodeLevelMetrics(individualBucketMetricsList);
        latch.countDown();
    }

    private List<Metric> gatherIndividualBucketMetrics(){
        List<Metric> individualBucketMetricsList = Lists.newArrayList();

        Map<String, ?> metricsMap = (Map<String, ?>)configuration.getConfigYml().get("metrics");
        Map<String, ?> bucketMap = (Map<String, ?>)metricsMap.get("bucket");
        List<Map<String, ?>> otherBucketMetricsList = (List<Map<String, ?>>)bucketMap.get("otherStats");

        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL, JsonNode.class);
        JsonNode opJsonNode = rootJsonNode.get("op");
        JsonNode sampleJsonNode = opJsonNode.get("samples");
        //#TODO take care of the metricPath
        individualBucketMetricsList.addAll(getIndividualBucketMetrics("", otherBucketMetricsList, sampleJsonNode));
        return individualBucketMetricsList;
    }

    private List<Metric> getIndividualBucketMetrics(String metricPath, List<Map<String, ?>> metricsList, JsonNode jsonNode){
        List<Metric> metricList = Lists.newArrayList();
        for(Map<String, ?> metric : metricsList){
            String metricName = metric.entrySet().iterator().next().getKey();
            Map<String, ?> metricProperties = (Map<String, ?>)metric.entrySet().iterator().next().getValue();
            JsonNode jsonValue = jsonNode.get(metricName);
            if(!jsonValue.isNull() && jsonValue.isArray()){
                JsonNode lastValueNode = jsonValue.get(jsonValue.size() - 1);
                Metric individualMetric = new Metric(metricName, lastValueNode.getTextValue(), metricPath, metricProperties);
                metricList.add(individualMetric);
            }
        }
        return metricList;
    }
}
