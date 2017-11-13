package com.appdynamics.extensions.couchbase;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.couchbase.metrics.ClusterAndNodeMetrics;
import com.appdynamics.extensions.couchbase.metrics.IndexMetrics;
import com.appdynamics.extensions.couchbase.metrics.QueryServiceMetrics;
import com.appdynamics.extensions.couchbase.metrics.buckets.BucketMetrics;
import com.appdynamics.extensions.couchbase.metrics.buckets.BucketMetricsProcessor;
import com.appdynamics.extensions.couchbase.metrics.xdcr.XDCRMetrics;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.util.AssertUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by venkata.konala on 9/18/17.
 */

/*
  The ExtensionMonitorTask(namely "task") needs to implement the interface
  AMonitorTaskRunnable instead of Runnable. This would make the need for overriding
  onTaskComplete() method which will be called once the run() method execution is done.
  This onTaskComplete() method emphasizes the need to print metrics like
  "METRICS_COLLECTION_STATUS" or do any other task complete work.
 */
public class CouchBaseMonitorTask implements AMonitorTaskRunnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ClusterAndNodeMetrics.class);
    private MonitorConfiguration configuration;
    private MetricWriteHelper metricWriteHelper;
    private Map<String, String> server;
    private String serverURL;
    private String clusterName;
    private Map<String, ?> configMap;
    private Map<String, ?> metricsMap;

    public CouchBaseMonitorTask(MonitorConfiguration configuration, MetricWriteHelper metricWriteHelper, Map<String, String> server){
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.serverURL = UrlBuilder.fromYmlServerConfig(server).build();
        this.clusterName = server.get("name");
        AssertUtils.assertNotNull(clusterName, "Name of the cluster should not be null");
        configMap = configuration.getConfigYml();
        metricsMap = (Map<String, ?>) configMap.get("metrics");
        AssertUtils.assertNotNull(metricsMap, "The 'metrics' section in config.yml is either null or empty");
    }

    public void run(){
        populateAndPrintMetrics();
    }

    void populateAndPrintMetrics(){
        CountDownLatch countDownLatch = new CountDownLatch(5);
        BucketMetricsProcessor bucketMetricsProcessor = new BucketMetricsProcessor();

        ClusterAndNodeMetrics clusterAndNodeMetricsTask = new ClusterAndNodeMetrics(configuration, metricWriteHelper, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : Cluster and Node metrics", clusterAndNodeMetricsTask);

        BucketMetrics bucketMetricsTask = new BucketMetrics(configuration, metricWriteHelper, clusterName, serverURL, metricsMap, countDownLatch, bucketMetricsProcessor);
        configuration.getExecutorService().submit(clusterName + " : Bucket metrics", bucketMetricsTask);

        CloseableHttpClient queryClient;
        try{
            queryClient = createClient();
        }
        catch(Exception e){
            queryClient = null;
        }
        QueryServiceMetrics queryServiceMetricsTask = new QueryServiceMetrics(configuration, metricWriteHelper, queryClient, server, clusterName, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : Query service metrics", queryServiceMetricsTask);

        XDCRMetrics xdcrMetricsTask = new XDCRMetrics(configuration, metricWriteHelper, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : xdcr metrics", xdcrMetricsTask);

        IndexMetrics indexMetricsTask = new IndexMetrics(configuration, metricWriteHelper, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : Index metrics", indexMetricsTask);

        //This task needs to wait for all the "subtasks" (in this case there are 5 subtasks) to finish. As the subtasks are responsible for printing the metrics, the main task have to
        // wait for all the subtasks finish. So latches are used to ensure this consistency is achieved.
        try{
            countDownLatch.await();
        }
        catch (InterruptedException ie){
            logger.error(ie.getMessage());
        }
    }

    private CloseableHttpClient createClient(){
        AssertUtils.assertNotNull(server, "The credentials in server section is either null or empty");
        AssertUtils.assertNotNull(server.get("host"), "host has not been provided in server section");
        AssertUtils.assertNotNull(server.get("queryPort"), "queryPort has not been provided in query section");
        HttpHost httpHost = new HttpHost(server.get("host"), Integer.parseInt(server.get("queryPort")), "http");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(httpHost.getHostName(), httpHost.getPort()), new UsernamePasswordCredentials(server.get("username"), server.get("password")));
        CloseableHttpClient closeableHttpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
        return closeableHttpClient;
    }

    public void onTaskComplete() {

    }
}
