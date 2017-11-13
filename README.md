# CouchBase Monitoring Extension for AppDynamics

## Use Case
Couchbase Server is an open source, distributed (shared-nothing architecture) NoSQL document-oriented database that is optimized for interactive applications. This extension allows the user to connect to a specific cluster host and retrieve metrics about the cluster, all the nodes within the cluster and any buckets associated with the nodes.

## Prerequisites
1. This extension works only with the standalone Java machine agent. The extension requires the machine agent to be up and running.
2. This extension creates a client to the CouchBase server that needs to be monitored. So the CouchBase server that has to be monitored, should be available for access from the machine that has the extension installed.

## Installation
1. Unzip the contents of "CouchBaseMonitor.zip" as "CouchBaseMonitor" and copy the "CouchBaseMonitor" directory to `<MACHINE_AGENT_HOME>/monitors/`

## Recommendations
It is recommended that a single CouchBase monitoring extension be used to monitor a single CouchBase cluster.

## Configuring the extension using config.yml
Configure the CouchBase monitoring extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/CouchBaseMonitor/`

  1. Configure the "tier" under which the metrics need to be reported. This can be done by changing the value of `<TIER NAME OR TIER ID>` in
     metricPrefix: "Server|Component:`<TIER NAME OR TIER ID>`|Custom Metrics|CouchBase".

     For example,
     ```
     metricPrefix: "Server|Component:Extensions tier|Custom Metrics|CouchBase"
     ```

  2. Configure the CouchBase cluster by specifying the name(required), host(required), port(required), queryPort(required) of  any node(server) in the CouchBase cluster, username(only if authentication enabled), password(only if authentication enabled), passwordEncrypted(only if password encryption required).

     For example,
     ```
      servers:
        - name: "Cluster1"
          host: "localhost"
          port: "8091"
          queryPort: "8093"
          username: "Administrator"
          password: "password1"
          passwordEncrypted: ""
     ```

  3. Configure the encyptionKey for passwordEncrypted(only if password encryption required).

     For example,
     ```
     #Encryption key for Encrypted password.
     encryptionKey: "axcdde43535hdhdgfiniyy576"
     ```
     
  4. Configure the metrics section.

     For configuring the metrics, the following properties can be used:

     |     Property      |   Default value |         Possible values         |                                              Description                                                                                                |
     | :---------------- | :-------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------- |
     | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
     | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
     | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
     | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
     | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
     | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:0, DOWN:1  |
     | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |

     For example,
     ```
     - bandwidth_usage:  #Bandwidth used during replication, measured in bytes per second.
         alias: "bandwidthUsed"
         multiplier: 1
         aggregationType: "SUM"
         timeRollUpType: "CURRENT"
         clusterRollUpType: "INDIVIDUAL"
         delta: true
     - status:
         alias: "status"
         convert:
           "healthy" : 1
           "warmup" : 2
     ```
     **All these metric properties are optional, and the default value shown in the table is applied to the metric(if a property has not been specified) by default.**
     
     There are five categories of metrics i.e cluster, node , bucket, query, xdcr. To disable any of these sections, change the include parameter under the section to "false" as follows:
     ```
     index:
        include: "false"
        stats:
            - memorySnapshotInterval:
                  alias: "memorySnapshotInterval"
            - stableSnapshotInterval:
                  alias: "stableSnapshotInterval"
            - maxRollbackPoints:
                  alias: "maxRollbackPoints"
     ```
     
## Metrics

### Metric Category: Cluster Metrics
|Metric Name            	|Description|
|------------------------------	|------------|
|ram_total        		|Total ram available to cluster (bytes)
|ram_quotaUsed        		|Ram quota used by the cluster (bytes)
|ram_usedByData         	|Ram used by the data in the cluster (bytes)
|ram_quotaTotal         	|Ram quota total for the cluster (bytes)
|ram_used	        	|Ram used by the cluster (bytes)
|hdd_total       		|Total harddrive space available to cluster (bytes)
|hdd_used       		|Harddrive space used by the cluster (bytes)
|hdd_usedByData       		|Harddrive use by the data in the cluster(bytes)
|hdd_quotaTotal       		|Harddrive quota total for the cluster (bytes)
|hdd_free       		|Free harddrive space in the cluster (bytes)

### Metric Category: Node Metrics
|Metric Name            	|Description|
|------------------------------	|-----------|
|memoryFree        		|Amount of memory free for the node (bytes)
|vb_replica_curr_items  	|Number of items/documents that are replicas 
|couch_docs_data_size         	|Data size of couch documents associated with a node (bytes)
|mem_total	        	|Total memory available to the node (bytes)
|mcdMemoryAllocated       	|Amount of memcached memory allocated (bytes)
|mcdMemoryReserved       	|Amount of memcached memory reserved (bytes)
|uptime       			|Time during which the node was in operation (sec)
|ep_bg_fetched       		|Number of disk fetches performed since server was started
|mem_used       		|Memory used by the node (bytes)
|memoryTotal        		|Total memory available to the node (bytes)
|get_hits  			|Number of get hits
|curr_items         		|Number of current items
|cmd_get	        	|Number of get commands
|couch_views_actual_disk_size   |Amount of disk space occupied by Couch views (bytes)
|swap_used       		|Amount of swap space used.(bytes)
|cpu_utilization_rate       	|The CPU utilization rate (%)
|couch_views_data_size       	|Size of object data for Couch views (bytes)
|curr_items_tot       		|Total number of items associated with node
|couch_docs_actual_disk_size    |Amount of disk space used by Couch docs.(bytes)
|swap_total       		|Total swap size allocated (bytes)
|ops       			|Number of operations performed on Couchbase

### Metric Category: Bucket Metrics
|Metric Name            	|Description|
|------------------------------	|-----------|
|opsPerSec        		|Number of operations per second
|rawRAM  			|Amount of raw RAM used by the bucket (bytes)
|diskFetches         		|Number of disk fetches
|ram         			|Amount of RAM used by the bucket (bytes)
|dataUsed	        	|Size of user data within buckets of the specified state that are resident in RAM.(%)
|memUsed       			|Amount of memory used by the bucket (bytes)
|itemCount       		|Number of items associated with the bucket
|diskUsed       		|Amount of disk used (bytes)
|quotaPercentUsed       	|Percentage of RAM used (for active objects) against the configure bucket size.(%)

## Version
2.0.0  -  Revamped the extension to support new extensions framework(2.0.0), Added 3 different categories of metrics(query, index and xdcr), Added extra metrics in cluster, node and bucket categories.

## Troubleshooting
Please follow the steps specified in the [TROUBLESHOOTING](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) document to debug problems faced while using the extension.

## Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/couchbase-monitoring-extension).

## Community
Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/extension/couchbase-monitoring-extension/) community.

## Support
For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).
