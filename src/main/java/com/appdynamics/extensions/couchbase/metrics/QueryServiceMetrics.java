/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.Constants.QUERY_SERVICE_URL;
import static com.appdynamics.extensions.couchbase.utils.JsonUtils.getMetrics;

/**
 * Created by venkata.konala on 9/20/17.
 */
public class QueryServiceMetrics implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(QueryServiceMetrics.class);
    private MonitorConfiguration configuration;
    private MetricWriteHelper metricWriteHelper;
    private CloseableHttpClient httpClient;
    private Map<String, String> server;
    private String clusterName;
    private Map<String, ?> queryMap;
    private CountDownLatch countDownLatch;


    public QueryServiceMetrics(MonitorConfiguration configuration, MetricWriteHelper metricWriteHelper, CloseableHttpClient httpClient, Map<String, String> server, String clusterName, Map<String, ?> metricsMap, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.httpClient = httpClient;
        this.server = server;
        this.clusterName = clusterName;
        this.queryMap = (Map<String, ?>) metricsMap.get("query");;
        this.countDownLatch = countDownLatch;
    }

    public void run(){
        try{
            if(queryMap != null && queryMap.get("include") != null && queryMap.get("include").toString().equalsIgnoreCase("true")) {
                List<Metric> queryMetricsList = gatherQueryMetrics();
                metricWriteHelper.transformAndPrintMetrics(queryMetricsList);
            }
            else{
                logger.debug("The metrics in 'query' section are not processed either because it is not present (or) 'include' parameter is either null or false");
            }
        }
        catch(Exception e){
            logger.error("Caught an exception while fetching query metrics : ", e);
        }
        finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException ie) {
                logger.error("Caught an exception while closing the query client : ", ie);
            }
            countDownLatch.countDown();
        }
    }

    private List<Metric> gatherQueryMetrics() throws IOException{
        List<Metric> queryMetricsList = Lists.newArrayList();
        try {
            List<Map<String, ?>> vitalsList = (List<Map<String, ?>>) queryMap.get("systemVitals");
            String url = String.format(QUERY_SERVICE_URL, server.get("host"), Integer.parseInt(server.get("queryPort")));
            JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, url, JsonNode.class);
            queryMetricsList.addAll(getMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "query", vitalsList, rootJsonNode));
            msReplace(queryMetricsList);
        }
        catch(Exception e){
            logger.error(e.getMessage());
        }
        return queryMetricsList;
    }



    private void msReplace(List<Metric> queryMetricList){
        for(Metric metric : queryMetricList){
            String metricValue = metric.getMetricValue();
            if(metricValue.contains("ms")){
                metric.setMetricValue(metricValue.replace("ms", ""));
            }
        }
    }
}
