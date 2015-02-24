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

import static com.appdynamics.extensions.couchbase.CouchBaseMonitor.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class CouchBaseWrapper {
	private static final Logger logger = Logger.getLogger("com.singularity.extensions.CouchBaseWrapper");
	public static final String CLUSTER_STATS_KEY = "Cluster Stats";
	private static final String CLUSTER_NODE_URI = "/pools/default";
	private static final String BUCKET_URI = "/pools/default/buckets";
	private static final String BUCKET_REPLICATION_STATS_URI = BUCKET_URI + "/%s/stats";
	private static final String REPLICATIONS_KEY = "replications";
	private static final String REPLICATIONS_SEPARATOR = "/";

	/**
	 * Gathers Cluster and Node stats. Cluster stats as Map of CLUSTER_STATS_KEY
	 * and Map of MetricName, MetricValue and NodeStats as Map of NodeName, Map
	 * of MetricName, MetricValue
	 * 
	 * @param httpClient
	 * @return
	 */
	public Map<String, Map<String, Double>> gatherClusterNodeMetrics(SimpleHttpClient httpClient) {
		JsonElement clusterNodeResponse = getResponse(httpClient, CLUSTER_NODE_URI);
		Map<String, Map<String, Double>> clusterNodeMetrics = new HashMap<String, Map<String, Double>>();
		if (clusterNodeResponse != null && clusterNodeResponse.isJsonObject()) {
			JsonObject clusterNodeJsonObject = clusterNodeResponse.getAsJsonObject();
			JsonObject storageTotals = clusterNodeJsonObject.getAsJsonObject("storageTotals");
			clusterNodeMetrics = populateClusterMetrics(clusterNodeMetrics, storageTotals);

			JsonArray nodes = (JsonArray) clusterNodeJsonObject.get("nodes");
			clusterNodeMetrics.putAll(populateNodeMetrics(nodes));
		}
		return clusterNodeMetrics;
	}

	/**
	 * Gathers Bucket stats as Map of BucketName and Map of MetricName,
	 * MetricValue
	 * 
	 * @param httpClient
	 * @return Map<String, Map<String, Double>>
	 */
	public Map<String, Map<String, Double>> gatherBucketMetrics(SimpleHttpClient httpClient) {
		JsonElement bucketResponse = getResponse(httpClient, BUCKET_URI);
		Map<String, Map<String, Double>> bucketMetrics = new HashMap<String, Map<String, Double>>();
		if (bucketResponse != null && bucketResponse.isJsonArray()) {
			JsonArray bucketStats = bucketResponse.getAsJsonArray();
			bucketMetrics = populateBucketMetrics(bucketStats);
		}
		return bucketMetrics;
	}
	
	/**
	 * Gathers Bucket Replication stats from /pools/default/buckets/<bucket_name>/stats
	 * 
	 * Response contains all other stats but this only filters any stats that starts with 'replications', 
	 * e.g. replications/03cd3332434401c64594f47eeeabbb79/beer-sample/gamesim-sample/wtavg_meta_latency
	 * 
	 * @param buckets
	 * @param httpClient
	 * @return Map<String, Map<String, Double>>
	 */
	public Map<String, Map<String, Double>> gatherBucketReplicationMetrics(Set<String> buckets, SimpleHttpClient httpClient) {
		Map<String, Map<String, Double>> replicationMetrics = new HashMap<String, Map<String, Double>>();
		
		if (buckets != null) {
			for (String bucket : buckets) {
				JsonElement bucketResponse = getResponse(httpClient, 
						String.format(BUCKET_REPLICATION_STATS_URI, bucket));
				
				if (bucketResponse != null && bucketResponse.isJsonObject()) {
					JsonObject bucketStats = bucketResponse.getAsJsonObject();
					JsonObject op = bucketStats.getAsJsonObject("op");
					JsonObject samples = op.getAsJsonObject("samples");
					populateBucketReplicationMetrics(bucket, samples.entrySet().iterator(), replicationMetrics);
				}
			}
		}
		
		return replicationMetrics;
	}

	private void populateBucketReplicationMetrics(String bucketName, 
			Iterator<Entry<String, JsonElement>> iterator, 
			Map<String, Map<String, Double>> replicationMetrics) {
		
		while (iterator.hasNext()) {
			Entry<String, JsonElement> entry = iterator.next();
			String metricName = (String) entry.getKey();
			
			// sample format: replications/03cd3332434401c64594f47eeeabbb79/beer-sample/gamesim-sample/wtavg_meta_latency
			if (metricName.startsWith(REPLICATIONS_KEY)) {
				String[] metricNameParts = metricName.split(REPLICATIONS_SEPARATOR);
				
				if (metricNameParts != null && metricNameParts.length > 2) {
					// e.g. gamesim-sample
					String destinationBucket = metricNameParts[metricNameParts.length - 2];
					
					// e.g. wtavg_meta_latency
					String replicationMetricName = metricNameParts[metricNameParts.length - 1];
					
					// e.g. beer-sample|replications|gamesim-sample
					String metricPrefix = String.format("%s%s%s%s%s", bucketName,
							METRIC_SEPARATOR, REPLICATIONS_KEY, METRIC_SEPARATOR, destinationBucket);
					
					JsonArray replicationStats = (JsonArray) entry.getValue();
					
					if (!replicationMetrics.containsKey(metricPrefix)) {
						replicationMetrics.put(metricPrefix, new HashMap<String, Double>());
					}
						
					populateBucketReplicationMetricsMapHelper(replicationMetricName, replicationStats, 
							replicationMetrics.get(metricPrefix));
				}
			}
		}
	}

	private void populateBucketReplicationMetricsMapHelper(String replicationMetricName, 
			JsonArray replicationStats, Map<String, Double> replicationMetrics) {
		if (replicationStats != null) {
			Iterator<JsonElement> stats = replicationStats.iterator();
			while (stats.hasNext()) {
				try {
					Double value = stats.next().getAsDouble();
					replicationMetrics.put(replicationMetricName, value);
					break;
				} catch (ClassCastException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Incorrect value type retrieved for %s, ignoring...", 
								replicationMetricName), ex);
					}
				} catch (IllegalStateException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Incorrect value type retrieved for %s, ignoring...", 
								replicationMetricName), ex);
					}					
				}
			}
		}
		
	}

	/**
	 * Returns JsonElement after parsing HttpResponse from given uri
	 * 
	 * @param httpClient
	 * @param uri
	 * @return
	 */
	private JsonElement getResponse(SimpleHttpClient httpClient, String uri) {
		String response = getResponseString(httpClient, uri);
		JsonElement jsonElement = null;
		try {
			jsonElement = new JsonParser().parse(response);
		} catch (JsonParseException e) {
			logger.error("Response from " + uri + "is not a json");
		}
		return jsonElement;
	}

	/**
	 * Returns HttpResponse as string from given url
	 * 
	 * @param httpClient
	 * @param path
	 * @return
	 */
	private String getResponseString(SimpleHttpClient httpClient, String path) {
		Response response = null;
		String responseString = "";
		try {
			response = httpClient.target().path(path).get();
			responseString = response.string();
		} catch (Exception e) {
			logger.error("Exception in getting response from " + path, e);
			throw new RuntimeException(e);
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception ex) {
				// Ignore
			}
		}
		return responseString;
	}

	/**
	 * Populates the cluster metrics hashmap
	 * 
	 * @param nodeMetrics
	 * @param clusterStats
	 * @return
	 */
	private Map<String, Map<String, Double>> populateClusterMetrics(Map<String, Map<String, Double>> nodeMetrics, JsonObject clusterStats) {
		Map<String, Double> clusterMetrics = new HashMap<String, Double>();
		if (clusterStats != null) {
			JsonObject ramStats = clusterStats.getAsJsonObject("ram");
			Iterator<Entry<String, JsonElement>> iterator = ramStats.entrySet().iterator();
			populateMetricsMapHelper(iterator, clusterMetrics, "ram_");

			JsonObject hddStats = clusterStats.getAsJsonObject("hdd");
			iterator = hddStats.entrySet().iterator();
			populateMetricsMapHelper(iterator, clusterMetrics, "hdd_");
		}
		nodeMetrics.put(CLUSTER_STATS_KEY, clusterMetrics);
		return nodeMetrics;
	}

	/**
	 * Populates the node metrics hashmap
	 * 
	 * @param nodes
	 * @return
	 */
	private Map<String, Map<String, Double>> populateNodeMetrics(JsonArray nodes) {
		Map<String, Map<String, Double>> nodeMetrics = new HashMap<String, Map<String, Double>>();
		for (JsonElement node : nodes) {
			JsonObject nodeObject = node.getAsJsonObject();
			Map<String, Double> metrics = new HashMap<String, Double>();
			nodeMetrics.put(nodeObject.get("hostname").getAsString(), metrics);

			JsonObject interestingStats = nodeObject.getAsJsonObject("interestingStats");
			Iterator<Entry<String, JsonElement>> iterator = interestingStats.entrySet().iterator();
			populateMetricsMapHelper(iterator, metrics, "");

			JsonObject systemStats = nodeObject.getAsJsonObject("systemStats");
			iterator = systemStats.entrySet().iterator();
			populateMetricsMapHelper(iterator, metrics, "");

			iterator = nodeObject.entrySet().iterator();
			populateMetricsMapHelper(iterator, metrics, "");
		}
		return nodeMetrics;
	}

	/**
	 * Populates the bucket metrics hashmap
	 * 
	 * @param buckets
	 * @return
	 */
	private Map<String, Map<String, Double>> populateBucketMetrics(JsonArray buckets) {
		Map<String, Map<String, Double>> bucketMetrics = new HashMap<String, Map<String, Double>>();
		for (JsonElement bucket : buckets) {
			JsonObject bucketObject = bucket.getAsJsonObject();
			Map<String, Double> metrics = new HashMap<String, Double>();
			bucketMetrics.put(bucketObject.get("name").getAsString(), metrics);

			JsonObject interestingStats = bucketObject.getAsJsonObject("quota");
			Iterator<Entry<String, JsonElement>> iterator = interestingStats.entrySet().iterator();
			populateMetricsMapHelper(iterator, metrics, "");

			JsonObject systemStats = bucketObject.getAsJsonObject("basicStats");
			iterator = systemStats.entrySet().iterator();
			populateMetricsMapHelper(iterator, metrics, "");
		}
		return bucketMetrics;
	}

	/**
	 * Populates an empty map with values retrieved from the entry set of a Json
	 * Object
	 * 
	 * @param iterator
	 *            An entry set iterator for the json object
	 * @param metrics
	 *            Initially empty map that is populated based on the values
	 *            retrieved from entry set
	 * @param prefix
	 *            Optional prefix for the metric name to distinguish duplicate
	 *            metric names
	 */
	private void populateMetricsMapHelper(Iterator<Entry<String, JsonElement>> iterator, Map<String, Double> metrics, String prefix) {
		while (iterator.hasNext()) {
			Entry<String, JsonElement> entry = iterator.next();
			String metricName = (String) entry.getKey();
			JsonElement value = (JsonElement) entry.getValue();
			if (value instanceof JsonPrimitive && NumberUtils.isNumber(value.getAsString())) {
				Double val = value.getAsDouble();
				metrics.put(prefix + metricName, val);
			}
		}
	}
}