package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.metal.MetalRadioButtonUI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;

/**
 * Created by venkata.konala on 10/3/17.
 */
public class IndividualXDCRBuckets implements Runnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IndividualXDCRBuckets.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String serverURl;
    private JsonNode xdcrBucketNode;
    private List<Map<String, ?>> xdcrMetricsList;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private MonitorExecutorService executorService;

    IndividualXDCRBuckets(MonitorConfiguration configuration, String clusterName, String serverURL, JsonNode xdcrBucketNode, List<Map<String, ?>> xdcrMetricsList, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.serverURl = serverURL;
        this.xdcrBucketNode = xdcrBucketNode;
        this.xdcrMetricsList = xdcrMetricsList;
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
    }

    public void run(){
        gatherAndPrintBucketXDCRMetrics();
        countDownLatch.countDown();
    }

    private void gatherAndPrintBucketXDCRMetrics(){


        String id = xdcrBucketNode.get("id").asText();
        String idSplit[] = id.split("/");
        String remote_uuid = idSplit[0];
        String bucketName = idSplit[1];
        String destinationName = idSplit[2];

        List<Metric> xdcrStatusMetrics = Lists.newArrayList();
        //#TODO take care of metric path
        Metric statusMetric = new Metric("status", xdcrBucketNode.get("status").getTextValue(), configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "xdcr" + METRIC_SEPARATOR + bucketName + METRIC_SEPARATOR + "status");
        xdcrStatusMetrics.add(statusMetric);

        CountDownLatch latch = new CountDownLatch(xdcrMetricsList.size());
        for (Map<String, ?> metric : xdcrMetricsList) {
            IndividualXDCRMetric individualXDCRMetricTask = new IndividualXDCRMetric(configuration, clusterName, serverURl, metric, remote_uuid, bucketName, destinationName,latch);
            executorService.submit(id + metric.entrySet().iterator().next().getKey(), individualXDCRMetricTask);
        }
        try {
            latch.await();
        }
        catch(InterruptedException ie){
            logger.debug(ie.getMessage());
        }
        finally {
            metricWriteHelper.transformAndPrintNodeLevelMetrics(xdcrStatusMetrics);
        }
    }
}
