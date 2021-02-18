/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.MetricPathUtils;
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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest(MetricPathUtils.class)
@PowerMockIgnore({ "javax.net.ssl.*" })
public class ClusterAndNodeMetricsTest{

    MonitorContextConfiguration contextConfiguration = mock(MonitorContextConfiguration.class);
    MonitorContext context = mock(MonitorContext.class);
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
        entity.setContent(new FileInputStream("src/test/resources/json/ClusterNode.json"));
        PowerMockito.mockStatic(MetricPathUtils.class);
    }

    @Test
    public void clusterAndNodeMetricsTest() throws IOException{
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        CountDownLatch latch = new CountDownLatch(1);

        when(contextConfiguration.getContext()).thenReturn(context);
        when(context.getHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);

        when(MetricPathUtils.buildMetricPath(Mockito.anyString(),Mockito.anyString())).thenReturn("Custom Metrics|CouchBase|Cluster1|nodes|172.17.0.2;8091");

        Map<String, ?> metricsMap = (Map<String, ?>)conf.get("metrics");
        ClusterAndNodeMetrics clusterAndNodeMetrics = new ClusterAndNodeMetrics(contextConfiguration, metricWriteHelper,"cluster1", "localhost:8090", metricsMap, latch);
        clusterAndNodeMetrics.run();

        verify(metricWriteHelper, times(1)).transformAndPrintMetrics(pathCaptor.capture());

        List<Metric> resultList = pathCaptor.getValue();
        Set<String> metricNames = Sets.newHashSet();
        metricNames.add("total");
        metricNames.add("quotaTotal");
        metricNames.add("used");
        metricNames.add("usedByData");
        metricNames.add("free");
        metricNames.add("quotaUsed");
        metricNames.add("quotaUsedPerNode");
        metricNames.add("quotaTotalPerNode");
        metricNames.add("rebalance_success");
        metricNames.add("rebalance_start");
        metricNames.add("rebalance_success");
        metricNames.add("rebalanceStatus");
        metricNames.add("cmd_get");
        metricNames.add("couch_docs_actual_disk_size");
        metricNames.add("couch_docs_data_size");
        metricNames.add("couch_spatial_data_size");
        metricNames.add("couch_spatial_disk_size");
        metricNames.add("couch_views_actual_disk_size");
        metricNames.add("couch_views_data_size");
        metricNames.add("curr_items");
        metricNames.add("curr_items_tot");
        metricNames.add("ep_bg_fetched");
        metricNames.add("get_hits");
        metricNames.add("mem_used");
        metricNames.add("ops");
        metricNames.add("vb_replica_curr_items");
        metricNames.add("cpu_utilization_rate");
        metricNames.add("swap_total");
        metricNames.add("swap_used");
        metricNames.add("mem_total");
        metricNames.add("mem_free");
        metricNames.add("clusterMembership");
        metricNames.add("status");

        for(Metric metric : resultList){
            Assert.assertTrue(metricNames.contains(metric.getMetricName()));
        }
    }
}
