package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.BUCKETS_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.INDIVIDUAL_BUCKET_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class BucketMetrics implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BucketMetrics.class);
    private MonitorConfiguration configuration;
    private CloseableHttpClient httpClient;
    private String serverURL;
    private CountDownLatch countDownLatch;
    private Set<String> bucketsSet;

    public BucketMetrics(MonitorConfiguration configuration, CloseableHttpClient httpClient, String serverURl, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.serverURL = serverURl;
        this.countDownLatch = countDownLatch;
        this.bucketsSet = Sets.newHashSet();
    }

    public void run(){
        List<Metric> bucketMetrics = gatherBucketMetrics();
        configuration.getMetricWriter().transformAndPrintNodeLevelMetrics(bucketMetrics);
        getIndividualBucketMetrics();
        countDownLatch.countDown();
    }

    private List<Metric> gatherBucketMetrics(){
        List<Metric> bucketMetrics = Lists.newArrayList();
        Map<String, ?> metricsMap = (Map<String, ?>)configuration.getConfigYml().get("metrics");
        Map<String, ?> bucketMap = (Map<String, ?>)metricsMap.get("bucket");
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + BUCKETS_ENDPOINT, JsonNode.class);
        //#TODO take care of the metricPath
        bucketMetrics.addAll(getBucketMetrics("", bucketMap, rootJsonNode));
        return bucketMetrics;
    }

    private List<Metric> getBucketMetrics(String metricPath, Map<String, ?> nodeMap, JsonNode jsonNode){
        List<Map<String, ?>> quotaList = (List<Map<String, ?>>)nodeMap.get("quota");
        List<Map<String, ?>> basicStatsList = (List<Map<String, ?>>)nodeMap.get("basicStats");
        List<Metric> metricList = Lists.newArrayList();
        for(JsonNode node : jsonNode){
            String bucketName = node.get("name").getTextValue();
            bucketsSet.add(bucketName);
            metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "Buckets" + bucketName + "quota",quotaList, node.get("quota")));
            metricList.addAll(getMetrics(metricPath + METRIC_SEPARATOR + "Buckets" + bucketName + "basicStats", basicStatsList, node.get("basicStats")));
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

    private void getIndividualBucketMetrics(){
        CountDownLatch latch = new CountDownLatch(bucketsSet.size());
        for(String bucket : bucketsSet){
            IndividualBucketMetrics individualBucketMetricsTask = new IndividualBucketMetrics(configuration, httpClient, String.format(serverURL + INDIVIDUAL_BUCKET_ENDPOINT, bucket), latch);
            configuration.getExecutorService().submit("Extra Bucket task" + bucket, individualBucketMetricsTask);
        }
        try{
            latch.await();
            logger.debug("Finished all the individual bucket metrics tasks");
        }
        catch(InterruptedException ie){
             logger.debug(ie.getMessage());
        }
    }
}
