package com.appdynamics.extensions.couchbase.metrics.xdcr;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.mockito.ArgumentCaptor;

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

public class IndividualXDCRBucketsTest {

    MonitorConfiguration configuration = mock(MonitorConfiguration.class);
    MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
    MonitorExecutorService executorService = mock(MonitorExecutorService.class);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    BasicHttpEntity entity;
    BasicHttpEntity entity1;
    Map<String, ?> conf;
    String serverURL;

    @Before
    public void init() throws IOException{
        conf = YmlReader.readFromFile(new File("src/test/resources/conf/config.yml"));
        entity = new BasicHttpEntity();
        entity.setContent(new FileInputStream("src/test/resources/json/xdcr/XDCRBucket.json"));
    }

    @Test
    public void xdcrMetricsTest() throws IOException{
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        CountDownLatch latch = new CountDownLatch(1);

        when(configuration.getHttpClient()).thenReturn(httpClient);
        when(configuration.getMetricWriter()).thenReturn(metricWriteHelper);
        when(configuration.getExecutorService()).thenReturn(executorService);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);

        List<Map<String, ?>> xdcrList = Lists.newArrayList();
        ObjectMapper mapper = new ObjectMapper();
        IndividualXDCRBuckets xdcrMetrics = new IndividualXDCRBuckets(configuration, "cluster1", "localhost:8090",  mapper.readValue(entity.getContent(), JsonNode.class),xdcrList, latch);
        xdcrMetrics.run();

        verify(metricWriteHelper, times(1)).transformAndPrintNodeLevelMetrics(pathCaptor.capture());
        List<Metric> resultList = pathCaptor.getValue();
        Set<String> metricNames = Sets.newHashSet();
        metricNames.add("status");
        for(Metric metric : resultList){
            Assert.assertTrue(metricNames.contains(metric.getMetricName()));
            Assert.assertTrue(metric.getMetricValue().equalsIgnoreCase("running"));
        }
        Assert.assertTrue(resultList.size() == 1);
    }
}