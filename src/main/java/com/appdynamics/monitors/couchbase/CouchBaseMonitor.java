package com.appdynamics.monitors.couchbase;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

public class CouchBaseMonitor extends AManagedMonitor{

    private static final String METRIC_PREFIX = "Custom Metrics|CouchBase|";
    private static final Logger logger = Logger.getLogger(CouchBaseMonitor.class.getSimpleName());

    public static void main(String[] args) throws Exception{
        Map<String, String> taskArguments = new HashMap<String, String>();
        taskArguments.put("host", "localhost");
        taskArguments.put("port", "8091");
        taskArguments.put("username", "");
        taskArguments.put("password", "");

        CouchBaseMonitor monitor = new CouchBaseMonitor();
        monitor.execute(taskArguments, null);
	}
    public CouchBaseMonitor() {
        logger.setLevel(Level.INFO);
    }

    /**
     * Writes the couchDB metrics to the controller
     * @param 	metricsMap		HashMap containing all the couchDB metrics
     */
    private void printMetrics(HashMap metricsMap) throws Exception {
        printMetrics("Cluster Stats|", "", (HashMap)metricsMap.get("ClusterStats"));
        printMetrics("Node Stats|", "NodeID|", (HashMap)metricsMap.get("NodeStats"));
        printMetrics("Bucket Stats|", "BucketID|", (HashMap)metricsMap.get("BucketStats"));

        // Another structure

        //printMetrics("Cluster Stats|", "", (HashMap)metricsMap.get("ClusterStats"));
        //printMetrics("Cluster Stats|Node Stats|", "NodeID|", (HashMap)metricsMap.get("NodeStats"));
        //printMetrics("ClusterStats|Node Stats|Bucket Stats|", "BucketID|", (HashMap)metricsMap.get("BucketStats"));
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

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        try {
            logger.info("Exceuting CouchBaseMonitor...");
            CouchBaseWrapper couchBaseWrapper = new CouchBaseWrapper(taskArguments);
            HashMap metrics = couchBaseWrapper.gatherMetrics();
            logger.info("Gathered metrics successfully. Size of metrics: " + metrics.size());
            printMetrics(metrics);
            logger.info("Printed metrics successfully");
            return new TaskOutput("Task successful...");
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
        return new TaskOutput("Task failed with errors");
    }

    /**
     * Print helper function. Concerned only with printing the metric map
     * @param 	metricPrefix	Prefix identifying the metric to be a cluster, node, or bucket metric
     * @param 	metricsMap		Map containing metrics
     */
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

    /**
     * Prints metrics for a cluster, nodes, or buckets
     * @param 	metricPrefix	Prefix identifying the metric to be a cluster, node, or bucket metric
     * @param   id              Identifies the node or bucket id
     * @param 	metricsMap		Map containing metrics
     */
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
}