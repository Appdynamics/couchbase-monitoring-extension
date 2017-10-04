package com.appdynamics.extensions.couchbase.metrics.buckets;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Sets;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class BucketMetrics implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BucketMetrics.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> bucketMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private MonitorExecutorService executorService;
    private Set<String> bucketsSet;
    private BucketMetricsProcessor bucketMetricsProcessor;

    public BucketMetrics(MonitorConfiguration configuration, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch, BucketMetricsProcessor bucketMetricsProcessor){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.bucketMap = (Map<String, ?>) metricsMap.get("buckets");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
        this.metricWriteHelper = this.configuration.getMetricWriter();
        this.executorService = this.configuration.getExecutorService();
        this.bucketsSet = Sets.newHashSet();
        this.bucketMetricsProcessor = bucketMetricsProcessor;
    }

    public void run(){
        if(bucketMap != null && bucketMap.get("include") != null && bucketMap.get("include").toString().equalsIgnoreCase("true")) {
            List<Metric> bucketMetrics = bucketMetricsProcessor.gatherBucketMetrics(configuration, httpClient, clusterName, serverURL, bucketMap, bucketsSet);
            metricWriteHelper.transformAndPrintNodeLevelMetrics(bucketMetrics);
            bucketMetricsProcessor.getIndividualBucketMetrics(configuration, clusterName, serverURL, bucketMap, bucketsSet);
        }
        else{
            logger.debug("The metrics in 'buckets' section are not processed either because it is not present (or) 'include' parameter is either null or false");
        }
        countDownLatch.countDown();
    }

    /*private List<Metric> gatherBucketMetrics(){
        List<Metric> bucketMetrics = Lists.newArrayList();
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + BUCKETS_ENDPOINT, JsonNode.class);
        Set<String> sectionSet = Sets.newHashSet();
        sectionSet.add("quota");
        sectionSet.add("basicStats");
        bucketMetrics.addAll(getNodeOrBucketMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "buckets", bucketMap, rootJsonNode, sectionSet, bucketsSet));
        return bucketMetrics;
    }

    private void getIndividualBucketMetrics(){
        CountDownLatch latch = new CountDownLatch(bucketsSet.size());
        for(String bucket : bucketsSet){
            OtherBucketMetrics otherBucketMetricsTask = new OtherBucketMetrics(configuration, clusterName, bucket, String.format(serverURL + INDIVIDUAL_BUCKET_ENDPOINT, bucket), bucketMap, latch);
            executorService.submit("Extra Bucket task " + bucket, otherBucketMetricsTask);
        }
        try{
            latch.await();
            logger.debug("Finished all the individual bucket json tasks");
        }
        catch(InterruptedException ie){
             logger.debug(ie.getMessage());
        }
    }*/
}
