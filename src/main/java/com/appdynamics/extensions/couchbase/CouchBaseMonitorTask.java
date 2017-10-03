package com.appdynamics.extensions.couchbase;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.couchbase.metrics.*;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by venkata.konala on 9/18/17.
 */
public class CouchBaseMonitorTask implements Runnable  {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ClusterAndNodeMetrics.class);
    private MonitorConfiguration configuration;
    private String serverURL;
    private String clusterName;
    private Map<String, ?> configMap;
    private Map<String, ?> metricsMap;

    public CouchBaseMonitorTask(MonitorConfiguration configuration, Map<String, String> server){
        this.configuration = configuration;
        this.serverURL = UrlBuilder.fromYmlServerConfig(server).build();
        this.clusterName = server.get("name");
        AssertUtils.assertNotNull(clusterName, "Name of the cluster should not be null");
        configMap = configuration.getConfigYml();
        metricsMap = (Map<String, ?>) configMap.get("metrics");
    }

    public void run(){
        populateAndPrintMetrics();
    }

    void populateAndPrintMetrics(){
        Map<String, ?> metricsMap = (Map<String, ?>)configMap.get("metrics");
        CountDownLatch countDownLatch = new CountDownLatch(5);

        ClusterAndNodeMetrics clusterAndNodeMetricsTask = new ClusterAndNodeMetrics(configuration, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : Cluster and Node metrics", clusterAndNodeMetricsTask);

        BucketMetrics bucketMetricsTask = new BucketMetrics(configuration, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : Bucket metrics", bucketMetricsTask);

        QueryServiceMetrics queryServiceMetricsTask = new QueryServiceMetrics(configuration, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : Query service metrics", queryServiceMetricsTask);

        XDCRMetrics xdcrMetricsTask = new XDCRMetrics(configuration, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : XDCR metrics", xdcrMetricsTask);

        IndexMetrics indexMetricsTask = new IndexMetrics(configuration, clusterName, serverURL, metricsMap, countDownLatch);
        configuration.getExecutorService().submit(clusterName + " : Index metrics", indexMetricsTask);

        try{
            countDownLatch.await();
        }
        catch (InterruptedException ie){
            logger.debug(ie.getMessage());
        }
    }
}
