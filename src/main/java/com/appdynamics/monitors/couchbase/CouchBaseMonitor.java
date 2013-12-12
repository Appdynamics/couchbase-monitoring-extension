package com.appdynamics.monitors.couchbase;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class CouchBaseMonitor extends AManagedMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|CouchBase|";
    private static final Logger logger = Logger.getLogger(CouchBaseMonitor.class.getSimpleName());
    private HashSet<String> disabledMetrics = new HashSet<String>();
    private boolean isInitialized = false;

    public static void main(String[] args) throws Exception{
        Map<String, String> taskArguments = new HashMap<String, String>();
        taskArguments.put("host", "localhost");
        taskArguments.put("port", "8091");
        taskArguments.put("username", "");
        taskArguments.put("password", "");
        taskArguments.put("disabled-metrics-path", "conf/DisabledMetrics.xml");

        CouchBaseMonitor monitor = new CouchBaseMonitor();
        monitor.execute(taskArguments, null);
	}

    public CouchBaseMonitor() {
        logger.setLevel(Level.INFO);
    }

    /**
     * Initializes the list of disabled metrics by reading the configuration file specified in monitor.xml
     * @param taskArguments
     * @throws Exception
     */
    private void initialize(Map<String, String> taskArguments) throws Exception{
        if (!isInitialized) {
            populateDisabledMetrics(taskArguments.get("disabled-metrics-path"));
            isInitialized = true;
            logger.info("Got list of disabled metrics from config file: " + taskArguments.get("disabled-metrics-path"));
        }
    }

    /**
     * Writes the couchBase metrics to the controller
     * @param 	metricsMap		HashMap containing all the couchDB metrics
     */
    private void printMetrics(HashMap metricsMap) throws Exception {
        printMetrics("Cluster Stats|", "", (HashMap)metricsMap.get("ClusterStats"));
        printMetrics("Node Stats|", "NodeID|", (HashMap)metricsMap.get("NodeStats"));
        printMetrics("Bucket Stats|", "BucketID|", (HashMap)metricsMap.get("BucketStats"));

        // Another possible structure in the Metric Browser

        //printMetrics("Cluster Stats|", "", (HashMap)metricsMap.get("ClusterStats"));
        //printMetrics("Cluster Stats|Node Stats|", "NodeID|", (HashMap)metricsMap.get("NodeStats"));
        //printMetrics("Cluster Stats|Node Stats|Bucket Stats|", "BucketID|", (HashMap)metricsMap.get("BucketStats"));
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
            initialize(taskArguments);
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
            if (!disabledMetrics.contains(metricName)) {
                printMetric(metricPrefix + metricName, metric,
                        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
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

    /**
     * Reads the config file in the conf/ directory and retrieves the list of disabled metrics
     * @param filePath          Path to the configuration file
     */
    private void populateDisabledMetrics(String filePath) throws Exception{
        BufferedInputStream configFile = null;

        try {
            configFile = new BufferedInputStream(new FileInputStream(filePath));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);

            Element disabledMetricsElement = (Element)doc.getElementsByTagName("DisabledMetrics").item(0);
            NodeList disabledMetricList = disabledMetricsElement.getElementsByTagName("Metric");

            for (int i=0; i < disabledMetricList.getLength(); i++) {
                Node disabledMetric = disabledMetricList.item(i);
                disabledMetrics.add(disabledMetric.getAttributes().getNamedItem("name").getTextContent());
            }
        } catch (FileNotFoundException e) {
            logger.error("Config file not found");
            throw e;
        } catch (ParserConfigurationException e) {
            logger.error("Failed to instantiate new DocumentBuilder");
            throw e;
        } catch (SAXException e) {
            logger.error("Error parsing the config file");
            throw e;
        } catch (DOMException e) {
            logger.error("Could not parse metric name");
            throw e;
        } finally {
            configFile.close();
        }
    }

}