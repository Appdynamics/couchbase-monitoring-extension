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

    public BucketMetrics(MonitorConfiguration configuration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch, BucketMetricsProcessor bucketMetricsProcessor){
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.bucketMap = (Map<String, ?>) metricsMap.get("buckets");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
        this.executorService = this.configuration.getExecutorService();
        this.bucketsSet = Sets.newHashSet();
        this.bucketMetricsProcessor = bucketMetricsProcessor;
    }

    public void run(){
        try {
            if (bucketMap != null && bucketMap.get("include") != null && bucketMap.get("include").toString().equalsIgnoreCase("true")) {
                List<Metric> bucketMetrics = bucketMetricsProcessor.gatherBucketMetrics(configuration, httpClient, clusterName, serverURL, bucketMap, bucketsSet);
                metricWriteHelper.transformAndPrintMetrics(bucketMetrics);
                bucketMetricsProcessor.getIndividualBucketMetrics(configuration, metricWriteHelper, clusterName, serverURL, bucketMap, bucketsSet);
            } else {
                logger.debug("The metrics in 'buckets' section are not processed either because it is not present (or) 'include' parameter is either null or false");
            }
        }
        catch(Exception e){

        }
        finally {
            countDownLatch.countDown();
        }
    }
}
