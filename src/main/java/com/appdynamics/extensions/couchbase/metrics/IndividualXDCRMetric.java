package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.couchbase.utils.Constants.INDIVIDUAL_BUCKET_WITHOUT_ZOOM_ENDPPOINT;
import static com.appdynamics.extensions.couchbase.utils.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.couchbase.utils.Constants.XDCR_ENDPOINT;

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

    IndividualXDCRMetric(MonitorConfiguration configuration, String clusterName, String serverURL, Map<String, ?> metric, String remote_uuid, String bucketName, String destinationName, CountDownLatch latch){
        this.configuration = configuration;
        this.clusterName = clusterName;
        this.serverURL = serverURL;
        this.metric = metric;
        this.remote_uuid = remote_uuid;
        this.bucketName = bucketName;
        this.destinationName = destinationName;
        this.latch = latch;
        this.httpClient = this.configuration.getHttpClient();
        this.metricWriteHelper = this.configuration.getMetricWriter();
        this.xdcrMetricList = Lists.newArrayList();
    }

    public void run(){
        gatherAndPrintXDCRMetric();
        metricWriteHelper.transformAndPrintNodeLevelMetrics(xdcrMetricList);
        latch.countDown();
    }

    private void gatherAndPrintXDCRMetric(){
        try {
            String metricName = metric.entrySet().iterator().next().getKey();
            Map<String, ?> metricProperties = (Map<String, ?>) metric.entrySet().iterator().next().getValue();
            String xdcrEndPoint = URLEncoder.encode(String.format(XDCR_ENDPOINT + "/" + metricName, remote_uuid, bucketName, destinationName), "UTF-8");
            String bucketEndPoint = String.format(INDIVIDUAL_BUCKET_WITHOUT_ZOOM_ENDPPOINT, bucketName);
            JsonNode xdcrNode = HttpClientUtils.getResponseAsJson(httpClient, serverURL + bucketEndPoint + xdcrEndPoint + "?zoom=day", JsonNode.class);
            if(xdcrNode != null){
                JsonNode nodeStats = xdcrNode.get("nodeStats");
                if(nodeStats != null){
                    for(JsonNode ip : nodeStats){
                        JsonNode jsonValue = nodeStats.get(ip.asText());
                        if(jsonValue.isArray()){
                            JsonNode lastJasonValue = jsonValue.get(jsonValue.size() - 1);
                            Metric individualMetric;
                            if(metricProperties != null){
                                individualMetric = new Metric(metricName, lastJasonValue.asText(), configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "xdcr" + METRIC_SEPARATOR + bucketName + METRIC_SEPARATOR + ip.asText() + METRIC_SEPARATOR + metricName, metricProperties);
                            }
                            else{
                                individualMetric = new Metric(metricName, lastJasonValue.asText(), configuration.getMetricPrefix() + METRIC_SEPARATOR + clusterName + METRIC_SEPARATOR + "xdcr" + METRIC_SEPARATOR + bucketName + METRIC_SEPARATOR + ip.asText() + METRIC_SEPARATOR + metricName);
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
