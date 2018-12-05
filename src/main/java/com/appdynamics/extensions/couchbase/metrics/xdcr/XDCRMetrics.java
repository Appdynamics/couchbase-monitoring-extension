/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics.xdcr;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.google.common.collect.Sets;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.appdynamics.extensions.couchbase.utils.Constants.TASKS_ENDPOINT;


/**
 * Created by venkata.konala on 9/26/17.
 */
public class XDCRMetrics implements Runnable{

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(XDCRMetrics.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> xdcrMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private MonitorExecutorService executorService;
    private Set<JsonNode> xdcrBucketsSet;

    public XDCRMetrics(MonitorConfiguration configuration, MetricWriteHelper metricWriteHelper, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.xdcrMap = (Map<String, ?>) metricsMap.get("xdcr");
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
        this.executorService = this.configuration.getExecutorService();
        this.xdcrBucketsSet = Sets.newHashSet();
    }

    public void run() {
        try {
            if (xdcrMap != null && xdcrMap.get("include") != null && xdcrMap.get("include").toString().equalsIgnoreCase("true")) {
                gatherXDCRBucketsSet();
                List<Map<String, ?>> xdcrMetricsList = (List<Map<String, ?>>) xdcrMap.get("stats");
                if (xdcrBucketsSet.size() != 0) {
                    getIndividualBucketXDCRMetrics(xdcrMetricsList);
                }
            } else {
                logger.debug("The metrics in 'xdcr' section are not processed either because it is not present (or) 'include' parameter is either null or false");
            }
        } catch (Exception e) {
            logger.error("Caught an exception while fetching XDCR metrics : ", e);
        } finally {
            countDownLatch.countDown();
        }
    }

    private void gatherXDCRBucketsSet(){
        JsonNode taskNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + TASKS_ENDPOINT, JsonNode.class);
        for(JsonNode node : taskNode){
            if(node.get("type").asText().equals("xdcr")){
                xdcrBucketsSet.add(node);
            }
        }
    }

    private void getIndividualBucketXDCRMetrics(List<Map<String, ?>> xdcrMetricsList){
        CountDownLatch latch = new CountDownLatch(xdcrBucketsSet.size());
        for(JsonNode xdcrBucketNode : xdcrBucketsSet) {
            IndividualXDCRBuckets individualXDCRBucketsTask = new IndividualXDCRBuckets(configuration, metricWriteHelper, clusterName, serverURL, xdcrBucketNode, xdcrMetricsList, latch);
            executorService.submit(xdcrBucketNode.get("id").asText(), individualXDCRBucketsTask);
        }
        try {
            latch.await(60, TimeUnit.SECONDS);
        }
        catch(InterruptedException ie){
            logger.error("The latch for Individual bucket XDCR metrics are interrupted : ", ie);
        }
    }

}
