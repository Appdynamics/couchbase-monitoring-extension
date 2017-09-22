package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.BUCKETS_ENDPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.QUERY_SERVICE_URL;

/**
 * Created by venkata.konala on 9/20/17.
 */
public class QueryServiceMetrics implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(QueryServiceMetrics.class);
    private MonitorConfiguration configuration;
    private CloseableHttpClient httpClient;
    private String serverURL;
    private CountDownLatch countDownLatch;

    public QueryServiceMetrics(MonitorConfiguration configuration, CloseableHttpClient httpClient, String serverURL, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.serverURL = serverURL;
        this.countDownLatch = countDownLatch;
    }

    public void run() {
        List<Metric> queryMetricsList = gatherQueryMetrics();
        configuration.getMetricWriter().transformAndPrintNodeLevelMetrics(queryMetricsList);
        countDownLatch.countDown();
    }

    private List<Metric> gatherQueryMetrics(){
        List<Metric> queryMetricsList = Lists.newArrayList();
        Map<String, ?> metricsMap = (Map<String, ?>)configuration.getConfigYml().get("metrics");
        Map<String, ?> queryMap = (Map<String, ?>)metricsMap.get("query");
        List<Map<String, ?>> vitalsList = (List<Map<String, ?>>)queryMap.get("systemVitals");
        JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, String.format(serverURL + QUERY_SERVICE_URL, queryMap.get("host").toString(), Integer.parseInt(queryMap.get("port").toString())), JsonNode.class);
        //#TODO take care of the metricPath
        queryMetricsList.addAll(getMetrics("", vitalsList, rootJsonNode));
        return queryMetricsList;
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
}
