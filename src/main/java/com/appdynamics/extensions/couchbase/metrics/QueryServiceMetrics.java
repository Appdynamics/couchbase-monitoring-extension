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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.*;

/**
 * Created by venkata.konala on 9/20/17.
 */
public class QueryServiceMetrics implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(QueryServiceMetrics.class);
    private MonitorConfiguration configuration;
    private String clusterName;
    private String serverURL;
    private Map<String, ?> queryMap;
    private CountDownLatch countDownLatch;
    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;

    public QueryServiceMetrics(MonitorConfiguration configuration, String clusterName, String serverURL, Map<String, ?> metricsMap, CountDownLatch countDownLatch){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.queryMap = (Map<String, ?>) metricsMap.get("query");;
        this.countDownLatch = countDownLatch;
        this.httpClient = this.configuration.getHttpClient();
        this.metricWriteHelper = this.configuration.getMetricWriter();
    }

    public void run() {
        if(queryMap != null && queryMap.get("include") != null && queryMap.get("include").toString().equalsIgnoreCase("true")) {
            List<Metric> queryMetricsList = gatherQueryMetrics();
            metricWriteHelper.transformAndPrintNodeLevelMetrics(queryMetricsList);
        }
        else{
            logger.debug("The metrics in 'query' section are not processed either because it is not present (or) 'include' parameter is either null or false");
        }
        countDownLatch.countDown();
    }

    private List<Metric> gatherQueryMetrics(){
        List<Metric> queryMetricsList = Lists.newArrayList();
        Map<String, ?> credentialsMap = (Map<String, ?>) queryMap.get("credentials");
        List<Map<String, ?>> vitalsList = (List<Map<String, ?>>)queryMap.get("systemVitals");
        if(credentialsMap != null  && credentialsMap.get("host") != null && credentialsMap.get("port") != null) {
            String url = String.format(serverURL + QUERY_SERVICE_URL, credentialsMap.get("host").toString(), Integer.parseInt(credentialsMap.get("port").toString()));
            JsonNode rootJsonNode = HttpClientUtils.getResponseAsJson(httpClient, url, JsonNode.class);
            queryMetricsList.addAll(getMetrics(configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR +"query", vitalsList, rootJsonNode));
        }
        else{
            logger.debug("Credentials for getting query metrics are not specified under the 'query' section");
        }
        return queryMetricsList;
    }
}
