package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.MonitorExecutorService;
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

import static com.appdynamics.extensions.couchbase.utils.Constants.*;

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

    public BucketMetrics(MonitorConfiguration configuration, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.bucketMap = (Map<String, ?>) metricsMap.get("buckets");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
        this.metricWriteHelper = this.configuration.getMetricWriter();
        this.executorService = this.configuration.getExecutorService();
        this.bucketsSet = Sets.newHashSet();
    }

    public void run(){
        List<Metric> bucketMetrics = gatherBucketMetrics();
        metricWriteHelper.transformAndPrintNodeLevelMetrics(bucketMetrics);
        getIndividualBucketMetrics();
        countDownLatch.countDown();
    }

    private List<Metric> gatherBucketMetrics(){
        List<Metric> bucketMetrics = Lists.newArrayList();
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + BUCKETS_ENDPOINT, JsonNode.class);
        Set<String> sectionSet = Sets.newHashSet();
        sectionSet.add("quota");
        sectionSet.add("basicStats");
        //#TODO take care of the metricPath
        bucketMetrics.addAll(getNodeOrBucketMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "buckets", bucketMap, rootJsonNode, sectionSet, bucketsSet));
        return bucketMetrics;
    }

    private void getIndividualBucketMetrics(){
        CountDownLatch latch = new CountDownLatch(bucketsSet.size());
        for(String bucket : bucketsSet){
            IndividualBucketMetrics individualBucketMetricsTask = new IndividualBucketMetrics(configuration, clusterName, bucket, String.format(serverURL + INDIVIDUAL_BUCKET_ENDPOINT, bucket), bucketMap, latch);
            executorService.submit("Extra Bucket task " + bucket, individualBucketMetricsTask);
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
