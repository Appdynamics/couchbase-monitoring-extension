/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics.buckets;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.yml.YmlReader;
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
import java.util.concurrent.CountDownLatch;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class OtherBucketMetricsTest {

    MonitorContextConfiguration configuration = mock(MonitorContextConfiguration.class);
    MonitorContext context = mock(MonitorContext.class);
    MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    MonitorExecutorService executorService = mock(MonitorExecutorService.class);
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    BasicHttpEntity entity;
    Map<String, ?> conf;

    @Before
    public void init() throws IOException{
        conf = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        entity = new BasicHttpEntity();
        entity.setContent(new FileInputStream("src/test/resources/json/buckets/IndividualBucket.json"));
    }

    @Test
    public void overallBucketMetricsTest() throws IOException{
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        CountDownLatch latch = new CountDownLatch(1);

        when(configuration.getContext()).thenReturn(context);
        when(context.getHttpClient()).thenReturn(httpClient);
        when(context.getExecutorService()).thenReturn(executorService);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);

        Map<String, ?> metricsMap =  (Map<String, ?>)conf.get("metrics");
        Map<String, ?> bucketMap = (Map<String, ?>) metricsMap.get("buckets");
        OtherBucketMetrics otherBucketMetrics = new OtherBucketMetrics(configuration, metricWriteHelper, "cluster1", "", "localhost:8090", bucketMap, latch);
        otherBucketMetrics.run();

        verify(metricWriteHelper, times(1)).transformAndPrintMetrics(listCaptor.capture());
        List<Metric> resultList = listCaptor.getValue();
        for(Metric metric : resultList){
            if(metric.getMetricName().equalsIgnoreCase("bytes_read")){
                Assert.assertTrue(metric.getMetricValue().equalsIgnoreCase("157.7350859453994"));
            }
            if(metric.getMetricName().equalsIgnoreCase("bytes_written")){
                Assert.assertTrue(metric.getMetricValue().equalsIgnoreCase("377695.652173913"));
            }
        }
    }
}
