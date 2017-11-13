package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Sets;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;


public class IndexMetricsTest{

    MonitorConfiguration configuration = mock(MonitorConfiguration.class);
    MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    BasicHttpEntity entity;
    Map<String, ?> conf;
    Map<String, ?> confWithIncludeFalse;

    @Before
    public void init() throws IOException{
        conf = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        confWithIncludeFalse = YmlReader.readFromFile(new File("src/test/resources/conf/config_WithIncludeFalse.yml"));
        entity = new BasicHttpEntity();
        entity.setContent(new FileInputStream("src/test/resources/json/Index.json"));
    }

    @Test
    public void indexMetricsTest() throws IOException{
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        CountDownLatch latch = new CountDownLatch(1);

        when(configuration.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);

        Map<String, ?> metricsMap = (Map<String, ?>)conf.get("metrics");
        IndexMetrics indexMetrics = new IndexMetrics(configuration, metricWriteHelper, "cluster1", "localhost:8090", metricsMap, latch);
        indexMetrics.run();

        verify(metricWriteHelper, times(1)).transformAndPrintMetrics(pathCaptor.capture());
        List<Metric> resultList = pathCaptor.getValue();
        Set<String> metricNames = Sets.newHashSet();
        metricNames.add("memorySnapshotInterval");
        metricNames.add("stableSnapshotInterval");
        metricNames.add("maxRollbackPoints");
        for(Metric metric : resultList){
            Assert.assertTrue(metricNames.contains(metric.getMetricName()));
        }
        Assert.assertTrue(resultList.size() == 3);
    }

    @Test
    public void indexMetricsWithIncludeFalseTest() throws IOException{
        CountDownLatch latch = new CountDownLatch(1);

        when(configuration.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);

        Map<String, ?> metricsMap = (Map<String, ?>)confWithIncludeFalse.get("metrics");
        IndexMetrics indexMetrics2 = new IndexMetrics(configuration, metricWriteHelper, "cluster1", "localhost:8090", metricsMap, latch);
        indexMetrics2.run();

        verify(metricWriteHelper, times(0)).transformAndPrintMetrics(anyList());
    }
}