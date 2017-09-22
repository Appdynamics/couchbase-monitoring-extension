package com.appdynamics.extensions.couchbase.utils;

public class Constants {
    public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|CouchBase|";
    public static final String METRIC_SEPARATOR  = "|";
    public static final String CLUSTER_NODES_ENDPOINT = "/pools/default";
    public static final String BUCKETS_ENDPOINT = "/pools/default/buckets";
    public static final String INDIVIDUAL_BUCKET_ENDPOINT = "/pools/default/buckets/%s/stats?zoom=day";
    public static final String QUERY_SERVICE_URL = "http://%s:%d/admin/vitals";
}
