/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  * All Rights Reserved.
 *  * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.couchbase.utils;

public class Constants {

    public static final String DEFAULT_METRIC_PREFIX = "Custom json|CouchBase|";
    public static final String METRIC_SEPARATOR  = "|";
    public static final String CLUSTER_NODES_ENDPOINT = "/pools/default";
    public static final String BUCKETS_ENDPOINT = "/pools/default/buckets";
    public static final String INDIVIDUAL_BUCKET_ENDPOINT = "/pools/default/buckets/%s/stats?zoom=day";
    public static final String QUERY_SERVICE_URL = "http://%s:%d/admin/vitals";
    public static final String TASKS_ENDPOINT = "/pools/default/tasks";
    public static final String INDIVIDUAL_BUCKET_WITHOUT_ZOOM_ENDPPOINT = "/pools/default/buckets/%s/stats/";
    public static final String XDCR_ENDPOINT = "replications/%s/%s/%s";
    public static final String INDEX_ENDPOINT = "/settings/indexes";
}
