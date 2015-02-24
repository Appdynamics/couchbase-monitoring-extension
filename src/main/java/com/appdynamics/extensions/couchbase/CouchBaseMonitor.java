/** 
 * Copyright 2013 AppDynamics 
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.couchbase;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.appdynamics.extensions.crypto.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class CouchBaseMonitor extends AManagedMonitor {

	private static final String BUCKET_STATS_PREFIX = "Bucket Stats|";
	private static final String NODE_STATS_PREFIX = "Node Stats|";
	private static final String CLUSTER_STATS_PREFIX = "Cluster Stats|";
	public static final String METRIC_SEPARATOR = "|";
	private static final String DISABLED_METRICS_PATH = "disabled-metrics-path";
	private static final String DISABLED_METRICS_XML = "monitors/CouchBaseMonitor/DisabledMetrics.xml";
	private static String METRIC_PREFIX_VALUE = "Custom Metrics|Couchbase|";
	private static final Logger logger = LoggerFactory.getLogger("com.singularity.extensions.CouchBaseMonitor");
	private Set<String> disabledMetrics = new HashSet<String>();
	private boolean isInitialized = false;

	public CouchBaseMonitor() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);
	}

	public static final Map<String, String> DEFAULT_ARGS = new HashMap<String, String>() {
		{
			put(TaskInputArgs.METRIC_PREFIX, METRIC_PREFIX_VALUE);
			put(DISABLED_METRICS_PATH, DISABLED_METRICS_XML);
		}
	};

	/**
	 * Main execution method that uploads the metrics to the AppDynamics
	 * Controller
	 * 
	 * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map,
	 *      com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
		try {
			logger.info("Exceuting CouchBaseMonitor...");
			taskArguments = ArgumentsValidator.validateArguments(taskArguments, DEFAULT_ARGS);
			initialize(taskArguments);
			logger.debug("The task arguments are {} ", taskArguments);
            if(logger.isDebugEnabled()){
                logger.debug("The decrypted password is " + CryptoUtil.getPassword(taskArguments));
            }
			SimpleHttpClient httpClient = SimpleHttpClient.builder(taskArguments).build();

			CouchBaseWrapper couchBaseWrapper = new CouchBaseWrapper();
			Map<String, Map<String, Double>> clusterNodeMetrics = couchBaseWrapper.gatherClusterNodeMetrics(httpClient);
			printClusterNodeMetrics(clusterNodeMetrics);
			Map<String, Map<String, Double>> bucketMetrics = couchBaseWrapper.gatherBucketMetrics(httpClient);
			printBucketMetrics(bucketMetrics);
			
			if (bucketMetrics != null && !bucketMetrics.isEmpty()) {
				Map<String, Map<String, Double>> bucketReplicationMetrics = couchBaseWrapper.gatherBucketReplicationMetrics(
						bucketMetrics.keySet(), httpClient);
				printBucketMetrics(bucketReplicationMetrics);
			}

			logger.info("Printed metrics successfully");
			return new TaskOutput("Task successfully...");
		} catch (Exception e) {
			logger.error("Exception: ", e);
		}
		return new TaskOutput("Task failed with errors");
	}

	private void printClusterNodeMetrics(Map<String, Map<String, Double>> nodeMetrics) throws Exception {
		for (Entry<String, Map<String, Double>> entry : nodeMetrics.entrySet()) {
			String key = entry.getKey();
			Map<String, Double> metrics = entry.getValue();
			if (CouchBaseWrapper.CLUSTER_STATS_KEY.equals(key)) {
				printMetricsHelper(CLUSTER_STATS_PREFIX, metrics);
			} else {
				printMetricsHelper(NODE_STATS_PREFIX + key + METRIC_SEPARATOR, metrics);
			}
		}
	}

	private void printBucketMetrics(Map<String, Map<String, Double>> nodeMetrics) throws Exception {
		for (Entry<String, Map<String, Double>> entry : nodeMetrics.entrySet()) {
			String bucketName = entry.getKey();
			Map<String, Double> metrics = entry.getValue();
			printMetricsHelper(BUCKET_STATS_PREFIX + bucketName + METRIC_SEPARATOR, metrics);
		}
	}

	/**
	 * Initializes the list of disabled metrics by reading the configuration
	 * file specified in monitor.xml
	 * 
	 * @param taskArguments
	 * @throws Exception
	 */
	private void initialize(Map<String, String> taskArguments) throws Exception {
		if (!isInitialized) {
			String fileName = getConfigFilename(taskArguments.get(DISABLED_METRICS_PATH));
			populateDisabledMetrics(fileName);
			isInitialized = true;
			logger.info("Got list of disabled metrics from config file: " + taskArguments.get(DISABLED_METRICS_PATH));
		}
		METRIC_PREFIX_VALUE = taskArguments.get(TaskInputArgs.METRIC_PREFIX) + METRIC_SEPARATOR;
	}

	/**
	 * Returns the metric to the AppDynamics Controller.
	 * 
	 * @param metricName
	 *            Name of the Metric
	 * @param metricValue
	 *            Value of the Metric
	 * @param aggregation
	 *            Average OR Observation OR Sum
	 * @param timeRollup
	 *            Average OR Current OR Sum
	 * @param cluster
	 *            Collective OR Individual
	 */
	private void printMetric(String metricName, Double metricValue, String aggregation, String timeRollup, String cluster) throws Exception {
		MetricWriter metricWriter = super.getMetricWriter(METRIC_PREFIX_VALUE + metricName, aggregation, timeRollup, cluster);
		if (metricValue != null) {
			metricWriter.printMetric(String.valueOf((long) metricValue.doubleValue()));
		}
	}

	/**
	 * Print helper function. Concerned only with printing the metric map
	 * 
	 * @param metricPrefix
	 *            Prefix identifying the metric to be a cluster, node, or bucket
	 *            metric
	 * @param metricsMap
	 *            Map containing metrics
	 */
	private void printMetricsHelper(String metricPrefix, Map<String, Double> metricsMap) throws Exception {
		for (Map.Entry<String, Double> entry : metricsMap.entrySet()) {
			String metricName = entry.getKey();
			Double metric = entry.getValue();
			if (!disabledMetrics.contains(metricName)) {
				printMetric(metricPrefix + metricName, metric, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
						MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
			}
		}
	}

	/**
	 * Reads the config file in the conf/ directory and retrieves the list of
	 * disabled metrics
	 * 
	 * @param filePath
	 *            Path to the configuration file
	 */
	private void populateDisabledMetrics(String filePath) throws Exception {
		BufferedInputStream configFile = null;
		try {
			configFile = new BufferedInputStream(new FileInputStream(filePath));
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(configFile);

			Element disabledMetricsElement = (Element) doc.getElementsByTagName("DisabledMetrics").item(0);
			NodeList disabledMetricList = disabledMetricsElement.getElementsByTagName("Metric");

			for (int i = 0; i < disabledMetricList.getLength(); i++) {
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
			try {
				if (configFile != null) {
					configFile.close();
				}
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
	}

	private String getConfigFilename(String filename) {
		if (filename == null) {
			return "";
		}
		// for absolute paths
		if (new File(filename).exists()) {
			return filename;
		}
		// for relative paths
		File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
		String configFileName = "";
		if (!Strings.isNullOrEmpty(filename)) {
			configFileName = jarPath + File.separator + filename;
		}
		return configFileName;
	}

	private static String getImplementationVersion() {
		return CouchBaseMonitor.class.getPackage().getImplementationTitle();
	}
}
