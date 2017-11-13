package com.appdynamics.extensions.couchbase.metrics.buckets;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.MonitorExecutorService;
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
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class BucketMetricsTest {

    MonitorConfiguration configuration = mock(MonitorConfiguration.class);
    MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    MonitorExecutorService executorService = mock(MonitorExecutorService.class);
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    BucketMetricsProcessor bucketMetricsProcessor = spy(BucketMetricsProcessor.class);
    BasicHttpEntity entity;
    Map<String, ?> conf;

    @Before
    public void init() throws IOException{
        conf = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        entity = new BasicHttpEntity();
        entity.setContent(new FileInputStream("src/test/resources/json/buckets/Bucket.json"));
    }

    @Test
    public void overallBucketMetricsTest() throws IOException{
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);

        when(configuration.getHttpClient()).thenReturn(httpClient);
        //when(configuration.getMetricWriter()).thenReturn(metricWriteHelper);
        when(configuration.getExecutorService()).thenReturn(executorService);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);
        //when(configuration.getMetricWriter()).thenReturn(metricWriteHelper);
        doNothing().when(bucketMetricsProcessor).getIndividualBucketMetrics(eq(configuration), eq(metricWriteHelper), eq("cluster1"), eq("localhost:8090"), anyMap(), anySet());

        Map<String, ?> metricsMap =  (Map<String, ?>)conf.get("metrics");
        CountDownLatch latch = new CountDownLatch(1);
        BucketMetrics bucketMetrics = new BucketMetrics(configuration, metricWriteHelper, "cluster1", "localhost:8090",  metricsMap, latch, bucketMetricsProcessor);
        bucketMetrics.run();

        verify(metricWriteHelper, times(1)).transformAndPrintMetrics(listCaptor.capture());
        Set<String> metricSet = Sets.newHashSet();
        metricSet.add("ram");
        metricSet.add("rawRAM");
        metricSet.add("quotaPercentUsed");
        metricSet.add("opsPerSec");
        metricSet.add("diskFetches");
        metricSet.add("itemCount");
        metricSet.add("diskUsed");
        metricSet.add("dataUsed");
        metricSet.add("memUsed");
        List<Metric> resultList = listCaptor.getValue();
        for(Metric metric : resultList){
            Assert.assertTrue(metricSet.contains(metric.getMetricName()));
        }
        Assert.assertTrue(resultList.size() == 18);
    }
}
