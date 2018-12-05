/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics.xdcr;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.*;

/**
 * Created by venkata.konala on 10/3/17.
 */
public class IndividualXDCRMetric implements Runnable{

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IndividualXDCRMetric.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> metric;
    private String remote_uuid;
    private String bucketName;
    private String destinationName;
    private CountDownLatch latch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private List<Metric> xdcrMetricList;
    private String metricName;

    IndividualXDCRMetric(MonitorConfiguration configuration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, Map<String, ?> metric, String remote_uuid, String bucketName, String destinationName, CountDownLatch latch){
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.metric = metric;
        this.remote_uuid = remote_uuid;
        this.bucketName = bucketName;
        this.destinationName = destinationName;
        this.latch = latch;
        this.httpClient = this.configuration.getHttpClient();
        this.xdcrMetricList = Lists.newArrayList();
    }

    public void run(){
        try {
            metricName = metric.entrySet().iterator().next().getKey();
            gatherAndPrintXDCRMetric();
            metricWriteHelper.transformAndPrintMetrics(xdcrMetricList);
        } catch (Exception e) {
            logger.error(String.format("Caught an exception while gathering XDCR metric : %s for bucket : %s", metricName, bucketName));
        } finally {
            latch.countDown();
        }
    }

    private void gatherAndPrintXDCRMetric(){
        try {
            Map<String, ?> metricProperties = (Map<String, ?>) metric.entrySet().iterator().next().getValue();
            String xdcrEndPoint = URLEncoder.encode(String.format(XDCR_ENDPOINT + "/" + metricName, remote_uuid, bucketName, destinationName), "UTF-8");
            String bucketEndPoint = String.format(INDIVIDUAL_BUCKET_WITHOUT_ZOOM_ENDPPOINT, bucketName);
            JsonNode xdcrNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + bucketEndPoint + xdcrEndPoint + "?zoom=day", JsonNode.class);
            if(xdcrNode != null){
                JsonNode nodeStats = xdcrNode.get("nodeStats");
                //int size = nodeStats.size();
                //Iterator<JsonNode> sample =  nodeStats.getElements();
                if(nodeStats != null){
                    for(JsonNode jsonValue : nodeStats){
                        if(jsonValue.isArray()){
                            JsonNode lastJasonValue = jsonValue.get(jsonValue.size() - 1);
                            AssertUtils.assertNotNull(lastJasonValue, "Value null or empty");
                            Metric individualMetric;
                            if(metricProperties != null){
                                individualMetric = new Metric(metricName, lastJasonValue.asText(), configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "xdcr" + METRIC_SEPARATOR + bucketName  + METRIC_SEPARATOR + metricName, metricProperties);
                            }
                            else{
                                individualMetric = new Metric(metricName, lastJasonValue.asText(), configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "xdcr" + METRIC_SEPARATOR + bucketName  + METRIC_SEPARATOR + metricName);
                            }
                            xdcrMetricList.add(individualMetric);
                        }
                    }
                }
            }
        }
        catch (UnsupportedEncodingException ue) {
            logger.debug("Unable to encode the url {}" + XDCR_ENDPOINT);
        }
    }
}
