# CouchBase Monitoring Extension for AppDynamics

## Use Case
Couchbase Server is an open source, distributed (shared-nothing architecture) NoSQL document-oriented database that is optimized for interactive applications. This extension allows the user to connect to a specific cluster host and retrieve metrics about the cluster, all the nodes within the cluster and any buckets associated with the nodes.

## Prerequisites
1. This extension works only with the standalone Java machine agent. The extension requires the machine agent to be up and running.
2. This extension creates a client to the CouchBase server that needs to be monitored. So the CouchBase server that has to be monitored, should be available for access from the machine that has the extension installed.
3. The client created through the extension uses various REST endpoints provided by the CouchBase server to retrieve metrics.
Please make sure your user account has proper admin role to access all the [REST endpoints](https://developer.couchbase.com/documentation/server/5.0/rest-api/rest-endpoints-all.html). "Full" and "Cluster" level roles gives you access to all the REST endpoints.

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

  3. Configure the encryptionKey for passwordEncrypted(only if password encryption required). See next section for encrypting password.

     For example,
     ```
     #Encryption key for Encrypted password.
     encryptionKey: "axcdde43535hdhdgfiniyy576"
     ```
  
  4. Configure the controllerInfo section
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
     
## Password encryption
To avoid setting the clear text password in the config.yml, please follow the steps below to encrypt the password and set the encrypted password and the key in the config.yml:
1. Download the util jar to encrypt the password from [here](https://github.com/Appdynamics/maven-repo/raw/master/releases/com/appdynamics/appd-exts-commons/2.0.0/appd-exts-commons-2.0.0.jar).
2. Encrypt password from the command line using the following command :
   ```
   java -cp "appd-exts-commons-2.0.0.jar" com.appdynamics.extensions.crypto.Encryptor myKey myPassword
   ```
   where "myKey" is any random key,
         "myPassword" is the actual password that needs to be encrypted
3. Add the values for "encryptionKey", "passwordEncrypted" in the config.yml. 
   The value for "encryptionKey" is the value substituted for "myKey" in the above command.
   The value for "passwordEncrypted" is the result of the above command.
     
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

Please visit [Encryption Guidelines] (https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.
If you want to use password encryption, please send arguments as connectionProperties. You will have to fill in the encrypted Password and Encryption Key fields in the config but you will also have to give an empty "" value to the password field and the encrypted password will be automatically picked up.

## Extensions Workbench ##
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually
 deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench ] (https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting ##

Please follow the steps listed in this [troubleshooting-document] (https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. 
These are a set of common issues that customers might have faced during the installation of the extension. 
If these don't solve your issue, please follow the last step on the [troubleshooting-document] (https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.


 ## Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

    1. Stop the running machine agent.
    2. Delete all existing logs under <MachineAgent>/logs.
    3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
        <logger name="com.singularity">
        <logger name="com.appdynamics">
    4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
    5. Attach the zipped <MachineAgent>/conf/* directory here.
    6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here.

For any support related questions, you can also contact help@appdynamics.com.

## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/couchbase-monitoring-extensio).

## Version

|                              |           |      
|------------------------------|-----------|
|Current version               |2.0.2      |
|CouchBase version tested on   |6.5.1        |
|Last Update                   |04/28/2020 |

##### 2.0.2 - Fixed the latch countdown issue for XDCR metrics. MA v20.3.0+ compatibility update.
##### 2.0.0 - Revamped the extension to support new extensions framework(2.0.0), Added 3 different categories of metrics(query, xdcr and index), Added extra metrics in cluster, node and bucket categories.
