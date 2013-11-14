package com.appdynamics.monitors.couchbase;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

import java.util.*;

public class CouchBaseMonitor extends AManagedMonitor{

    private static final String METRIC_PREFIX = "Custom Metrics|CouchBase|";

    public static void main(String[] args) throws Exception{
//        List<URI> baseList = new ArrayList<URI>();
//        baseList.add(new URI("http://127.0.0.1:8091/pools"));
//
//        CouchbaseClient couchbaseClient = new CouchbaseClient(baseList, "default", "");
//        Map stats = couchbaseClient.getStats();
//        printMap(stats);
//        couchbaseClient.shutdown();

        CouchBaseConfig config = new CouchBaseConfig();
        config.username="";
        config.password="";
        config.hostId="localhost";
        config.port="8091";
        CouchBaseWrapper wrapper = new CouchBaseWrapper(config);
        try {
            wrapper.gatherMetrics();

        }
        catch (Exception e) {
            throw e;
        }

	}

    /**
     * Writes the couchDB metrics to the controller
     * @param 	metricsMap		HashMap containing all the couchDB metrics
     */
    private void printMetrics(HashMap metricsMap) throws Exception {
        printMetrics("Cluster Stats|", "", (HashMap)metricsMap.get("ClusterStats"));
        printMetrics("Node Stats|", "NodeID|", (HashMap)metricsMap.get("NodeStats"));
        printMetrics("Bucket Stats|", "BucketID|", (HashMap)metricsMap.get("BucketStats"));
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     * @param 	metricName		Name of the Metric
     * @param 	metricValue		Value of the Metric
     * @param 	aggregation		Average OR Observation OR Sum
     * @param 	timeRollup		Average OR Current OR Sum
     * @param 	cluster			Collective OR Individual
     */
    private void printMetric(String metricName, Number metricValue, String aggregation, String timeRollup, String cluster) throws Exception
    {
        MetricWriter metricWriter = super.getMetricWriter(METRIC_PREFIX + metricName,
                aggregation,
                timeRollup,
                cluster
        );
        metricWriter.printMetric(String.valueOf((long) metricValue.doubleValue()));
    }

    @Override
    public TaskOutput execute(Map<String, String> stringStringMap, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

    }

    private void printMetricsHelper(String metricPrefix, Map metricsMap) throws Exception {
        HashMap<String, Number> metrics = (HashMap<String, Number>) metricsMap;
        Iterator iterator = metrics.keySet().iterator();
        while (iterator.hasNext()) {
            String metricName = iterator.next().toString();
            Number metric = metrics.get(metricName);
            printMetric(metricPrefix + metricName, metric,
                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        }
    }

    private void printMetrics(String metricPrefix, String id, HashMap metricsMap) throws Exception {
        if (id.equals("")) {
            printMetricsHelper(metricPrefix, metricsMap);
        }
        else {
            Iterator iterator = metricsMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, HashMap> mapEntry = (Map.Entry<String, HashMap>) iterator.next();
                String hostName = mapEntry.getKey();
                HashMap<String, Number> metricStats = mapEntry.getValue();
                printMetricsHelper(metricPrefix + id + hostName + "|", metricStats);
            }
        }
    }

//    private static void printMap(Map map) {
//        Iterator iterator = map.keySet().iterator();
//        while (iterator.hasNext()) {
//            Object hostKey = iterator.next();
//            Map metricMap = (Map) map.get(hostKey);
//            Iterator metricIterator = metricMap.keySet().iterator();
//            while (metricIterator.hasNext()) {
//                String metricName = metricIterator.next().toString();
//                String metricValue = (String) metricMap.get(metricName);
//                System.out.println(String.format("Key: %40s Value: %25s", metricName, metricValue));
//            }
//        }
//    }
}