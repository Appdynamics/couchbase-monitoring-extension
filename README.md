# CouchBase Monitoring Extension for AppDynamics

## Use Case
Couchbase Server is an open source, distributed (shared-nothing architecture) NoSQL document-oriented database that is optimized for interactive applications. This extension allows the user to connect to a specific cluster host and retrieve metrics about the cluster, all the nodes within the cluster and any buckets associated with the nodes.

## Prerequisites
1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2. The client created through the extension uses various REST endpoints provided by the CouchBase server to retrieve metrics.
Please make sure your user account has proper admin role to access all the [REST endpoints](https://docs.couchbase.com/server/current/rest-api/rest-endpoints-all.html). "Full" and "Cluster" level roles gives you access to all the REST endpoints.
3. The extension needs to be able to connect to the CouchBase server in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

## Installation

1. Run 'mvn clean install' from "CouchBaseMonitorRepo"
2. Unzip the `CouchBaseMonitor-<version>.zip` from `target` directory into the "<MachineAgent_Dir>/monitors" directory.
3. Edit the file config.yml located at <MachineAgent_Dir>/monitors/CouchBaseMonitor The metricPrefix of the extension has to be configured as specified [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
4. Restart the Machine Agent.

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.

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
More details around metric prefix can be found [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695).

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
          encryptedPassword: ""
     ```

  3. Configure the encryptionKey for encryptedPassword(only if password encryption required). See next section for encrypting password.

     For example,
     ```
     #Encryption key for Encrypted password.
     encryptionKey: "axcdde43535hdhdgfiniyy576"
     ```
  
  4. Configure the controllerInfo section (optional)<br/>
     For example,
     ```
     controllerInfo:
       username: "username" # Username used for controller login
       account: "customer1" # Accountname in the controller
       password: "password" #  Password for the above username 
       encryptedPassword: "" # Only required if encrypting the password, else leave it empty
     ```
   
     
  5. Configure the metrics section.

     For configuring the metrics, the following properties can be used:

     |     Property      |   Default value |         Possible values         |                                              Description                                                                                                |
     | :---------------- | :-------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------- |
     | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
     | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)    |
     | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)   |
     | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)|
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
     
     There are six categories of metrics i.e cluster, node , bucket, query, xdcr, index. To disable any of these sections, change the include parameter under the section to "false" as follows:
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

### Metric Category: cluster Metrics
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

### Metric Category: nodes Metrics
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

### Metric Category: buckets Metrics
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

### Metric Category: query Metrics
|           Metric Name           |                      Description                               |
|---------------------------------|----------------------------------------------------------------|
|request.completed.count        	|Number of requests completed
|request.active.count  			      |Number of active requests
|request.per.sec.1min         		|query throughput 1 minute
|request.per.sec.5min             |query throughput 5 minutes
|request.per.sec.15min            |query throughput 15 minutes
|request_time.mean                |Mean time to comlete a request
|request_time.median              |Median time to complete a request
|request_time.80percentile        |80th percentile query response time
|request_time.95percentile        |95th percentile query response time
|request_time.99percentile        |99th percentile query response time
|request.prepared.percent         |percentage of prepared requests


### Metric Category: xdcr Metrics
|Metric Name            	|Description|
|------------------------------	|-----------|
|bandwidth_usage        		|Bandwidth used during replication, measured in bytes per second
|changes_left  			|Number of updates still pending replication
|data_replicated         		|Size of data replicated in bytes
|docs_checked         			|Number of documents checked for changes
|docs_failed_cr_source	        	|Number of documents that have failed conflict resolution on the source cluster and not replicated to target cluster
|docs_filtered       			|Number of documents that have been filtered out and not replicated to target cluster
|docs_latency_wt       		|Weighted average latency for sending replicated changes to destination cluster
|docs_opt_repd       		|Number of docs sent optimistically
|docs_received_from_dcp       	|Number of documents received from DCP
|docs_rep_queue                 |Number of documents in replication queue
|docs_written                   |Number of documents written to the destination cluster via xdcr
|meta_latency_wt                |Weighted average time for requesting document metadata. xdcr uses this for conflict resolution prior to sending the document into the replication queue.
|num_checkpoints                |Number of checkpoints issued in replication queue.
|num_failedckpts                |Number of checkpoints failed during replication.
|rate_received_from_dcp         |Number of documents received from DCP per second.
|rate_replication               |Rate of documents being replicated, measured in documents per second.
|size_rep_queue                 |Size of replication queue in bytes.
|time_committing                |Seconds elapsed during replication.

### Metric Category: index Metrics
|Metric Name            	|Description|
|------------------------------	|-----------|
|memorySnapshotInterval        		|How often the indexer creates an in-memory snapshot for querying
|stableSnapshotInterval  			|How often the indexer creates a persistent snapshot of recovery
|maxRollbackPoints         		|Maximum number of rollback points

## Credentials Encryption ##

Please visit [Encryption Guidelines](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.
If you want to use password encryption, please send arguments as connectionProperties. You will have to fill in the encrypted Password and Encryption Key fields in the config but you will also have to give an empty "" value to the password field and the encrypted password will be automatically picked up.

## Extensions Workbench ##
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually
 deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting ##

Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. 
These are a set of common issues that customers might have faced during the installation of the extension. 

## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/couchbase-monitoring-extensio).

## Version

|            Name              |       Version      |      
|------------------------------|--------------------|
|Extension version             |2.0.4               |
|CouchBase version tested on   |6.5.1               |
|Last Update                   |18/02/2021          |
|Changelist                    |[Changelog](https://github.com/Appdynamics/couchbase-monitoring-extension/blob/master/CHANGELOG.md)|

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.
