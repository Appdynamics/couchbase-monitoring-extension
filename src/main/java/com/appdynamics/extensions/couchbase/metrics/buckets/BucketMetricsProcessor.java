package com.appdynamics.extensions.couchbase.metrics.buckets;

import com.appdynamics.extensions.MetricWriteHelper;
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
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.JsonUtils.getNodeOrBucketMetrics;

/**
 * Created by venkata.konala on 10/4/17.
 */
public class BucketMetricsProcessor {

     private static final Logger logger = LoggerFactory.getLogger(BucketMetrics.class);

     List<Metric> gatherBucketMetrics(MonitorConfiguration configuration, CloseableHttpClient httpClient, String clusterName, String serverURL, Map<String, ?> bucketMap, Set<String> bucketsSet){
        List<Metric> bucketMetrics = Lists.newArrayList();
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + BUCKETS_ENDPOINT, JsonNode.class);
        Set<String> sectionSet = Sets.newHashSet();
        sectionSet.add("quota");
        sectionSet.add("basicStats");
        bucketMetrics.addAll(getNodeOrBucketMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "buckets", bucketMap, rootJsonNode, sectionSet, bucketsSet));
        return bucketMetrics;
     }

     void getIndividualBucketMetrics(MonitorConfiguration configuration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, Map<String, ?> bucketMap, Set<String> bucketsSet){
        CountDownLatch latch = new CountDownLatch(bucketsSet.size());
        for(String bucket : bucketsSet){
            OtherBucketMetrics otherBucketMetricsTask = new OtherBucketMetrics(configuration, metricWriteHelper, clusterName, bucket, serverURL, bucketMap, latch);
            configuration.getExecutorService().submit("Individual Bucket task for : " + bucket, otherBucketMetricsTask);
        }
        try{
            latch.await();
            logger.debug("Finished all the individual bucket json tasks");
        }
        catch(InterruptedException ie){
            logger.error(ie.getMessage());
        }
     }
}
