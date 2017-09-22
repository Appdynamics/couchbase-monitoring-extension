package com.appdynamics.extensions.couchbase;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.couchbase.metrics.BucketMetrics;
import com.appdynamics.extensions.couchbase.metrics.ClusterAndNodeMetrics;
import com.appdynamics.extensions.couchbase.metrics.QueryServiceMetrics;
import com.appdynamics.extensions.http.UrlBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by venkata.konala on 9/18/17.
 */
class CouchBaseMonitorTask implements Runnable  {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ClusterAndNodeMetrics.class);
    private MonitorConfiguration configuration;
    private String serverURL;
    private String serverName;
    private CloseableHttpClient httpClient;

    CouchBaseMonitorTask(MonitorConfiguration configuration, Map<String, String> server){
        this.configuration = configuration;
        this.serverURL = UrlBuilder.fromYmlServerConfig(server).build();
        this.serverName = server.get("name");
        this.httpClient = configuration.getHttpClient();
    }

    public void run(){
        populateAndPrintMetrics();
    }

    void populateAndPrintMetrics(){
        CountDownLatch countDownLatch = new CountDownLatch(3);

        ClusterAndNodeMetrics clusterAndNodeMetricsTask = new ClusterAndNodeMetrics(configuration, httpClient, serverURL, countDownLatch);
        configuration.getExecutorService().submit(serverName + " Cluster and Node Metrics", clusterAndNodeMetricsTask);
        BucketMetrics bucketMetricsTask = new BucketMetrics(configuration, httpClient, serverURL, countDownLatch);
        configuration.getExecutorService().submit(serverName + " Bucket Metrics", bucketMetricsTask);
        QueryServiceMetrics queryServiceMetricsTask = new QueryServiceMetrics(configuration, httpClient, serverURL, countDownLatch);
        configuration.getExecutorService().submit(serverName + "Query service metrics", queryServiceMetricsTask);
        try{
            countDownLatch.await();
        }
        catch (InterruptedException ie){
            logger.debug(ie.getMessage());
        }
    }
}
