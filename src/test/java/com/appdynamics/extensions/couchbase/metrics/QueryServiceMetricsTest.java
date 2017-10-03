package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.couchbase.CouchBaseMonitor;
import com.appdynamics.extensions.couchbase.CouchBaseMonitorTask;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QueryServiceMetrics.class, HttpClientUtils.class, CouchBaseMonitorTask.class})

public class QueryServiceMetricsTest{

    MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    BasicHttpEntity entity;
    Map<String, ?> conf;

    @Before
    public void init() throws IOException{
        conf = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        entity = new BasicHttpEntity();
        entity.setContent(new FileInputStream("src/test/resources/Query.json"));
    }

    @Test
    public void clusterAndNodeMetricsTest() throws IOException{
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        mockStatic(HttpClientUtils.class);
        mockStatic(CouchBaseMonitorTask.class);
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, ?> server = Maps.newHashMap();
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);
        ObjectMapper mapper = new ObjectMapper();
        when(HttpClientUtils.getResponseAsJson(eq(httpClient), anyString(), eq(JsonNode.class))).thenReturn(mapper.readValue(entity.getContent(), JsonNode.class));
        CouchBaseMonitorTask.metricsMap = (Map<String, ?>)conf.get("metrics");
        CouchBaseMonitorTask.httpClient = httpClient;
        CouchBaseMonitorTask.metricWriteHelper = metricWriteHelper;
        QueryServiceMetrics queryServiceMetrics = new QueryServiceMetrics(latch);
        queryServiceMetrics.run();
        verify(metricWriteHelper, times(1)).transformAndPrintNodeLevelMetrics(pathCaptor.capture());
        List<Metric> resultList = pathCaptor.getValue();
        Set<String> metricNames = Sets.newHashSet();
        metricNames.add("request.completed.count");
        metricNames.add("request.active.count");
        metricNames.add("request.per.sec.1min");
        metricNames.add("request.per.sec.5min");
        metricNames.add("request.per.sec.15min");
        metricNames.add("request_time.mean");
        metricNames.add("request_time.median");
        metricNames.add("request_time.80percentile");
        metricNames.add("request_time.95percentile");
        metricNames.add("request_time.99percentile");
        metricNames.add("request.prepared.percent");
        for(Metric metric : resultList){
            Assert.assertTrue(metricNames.contains(metric.getMetricName()));
        }
        Assert.assertTrue(resultList.size() == 11);
    }
}