### io.deltastandalone.springboot

[![Java CI with Maven](https://github.com/prdpsvs/io.deltastandalone.springboot/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/prdpsvs/io.deltastandalone.springboot/actions/workflows/maven.yml)

## Delta Format Use cases outside Spark

Delta lake format is becoming a new open-source standard for data analytics. But why?
* ACID Support
* Time Travel
* Schema Evolution

Primarly, deltalake format standardization is acheived with spark engine (big data enigneering and analytical workloads). Can we read the datasets stored in delta format outside Spark using standard libraries? Yes, we can however, we need to figure out a way how transaction log (a.k.a _delta_log) can be parsed.

### What consists of Transaction Log?
Whenever user modifies a dataset (insert/update/delete), delta lake breaks the operation into series of steps composed of one or more action. A few examples of an action are Add file, Remove file, Update metadata etc. All these actions are recorded as atomic steps called commits stored as a json file. Each action on a dataset is a commit, resulting in a json (000000.json) file. 10 commits become a checkpoint (0000010.checkpoint. parquet) file.  
Is Transaction Log user friendly to understand?
Yes, understanding each commit json file is very straight forward. It contains the following metadata
* commitInfo – commitInfo object has timestamp, type of operation, operation metrics, operation parameters, readversion and isBlindAppend
* Series of actions
  -	If the action is add/remove, then it has path, partitionValues (if any), size, modification time, data change and stats.
  -	Stats contains minimum and maximum values of all columns stored in a file.

### Are there ways to read and Interpret Transaction Log?
Yes, there are couple of ways to read and interpret transaction log files under _delta_log folder.
* As each commit or series of commits (checkpoint) are json files, its easy-to-read json contents. I would not prefer this approach for two reasons 
  - This one is obvious, Rebuilding metadata by reading json content for each commit since last checkpoint can be cumbersome. Note the change in transaction log     metadata structure can lead to read failures (read change protocol action).
  - Delta format adheres to optimistic concurrency. What happens if two or more users are reading the dataset while you are writing or vise verse?
* Is there a better way to read delta format datasets? Yes, the delta standalone library is a solution. It is a single node java library that can be used to read from and write to delta datasets on file storage. This file storage can be ADLS Gen2, Windows/Linux file systems, S3 buckets or another file store that supports HDFS api’s.

### What is Delta Standalone Library?

Delta Standalone library provides APIs to interact with a dataset metadata in the transaction log, implementing the Delta Transaction Log Protocol to achieve the transactional guarantees of the Delta Lake format. The good part is that this library does not depend on Apache Spark and has only a few transitive dependencies, therefore it can be used by any compute (Web Api’s, Azure Functions, Web Jobs with combination of MPP systems such as SQL databases/data warehouses).

### Where can I use this Library?
If you observe this library closely, you will notice that the power of this library is not to read the actual data but the metadata (transaction log a.k.a _delta_log). Now let’s define use cases where we can use this library?
* Synapse Data Warehouse or any other databases on Azure stack can’t read datasets in delta format. Can we use this library to retrieve files injunction with MPP or database systems that have compute power to read parquet files? 
* Can your background services, micro services, HTAP services read datasets from ADLS G2 or any other storage instead of storage all the datasets in a sql layer by duplicating data for transactional and analytical needs?
* Can various ELT services leverage this library as a metadata layer and skip the usage of Spark simply to read delta log?

### How to use Delta Standalone Library?

This library is simple to use. You need to know about three classes to successfully implement delta reads of a dataset.
* DeltaLog – is the interface/class to programmatically interact with the metadata in transaction log (under _delta_log folder) for a dataset. This class provides access to the snapshot class in context of reading a dataset 
* Snapshot & DeltaScan – snapshot represents the state of a dataset at a specific version. DeltaLog class also provides a way to read a version using getSnapshotForTimestampAsOf or getSnapshotForVersionAsOf. DeltaScan provides memory-optimized iterator over metadata files optionally by passing in a partition filtering predicate (partition pruning)
* OptimisticTransaction – This is a main class to set the updates to the transaction log. During a transaction all reads must be done using OptimisticTransaction instead of DeltaLog in order to detect conflicts and concurrent updates.

### Pre-requsites to setup the solution

* Set the storage configuration to the storage where delta datasets are stored. Refer to below method where storage configuration is set to use ADLS Gen2 storage account. The following method uses application registration to connect to storage account with storage blob data contributor role. The application registration secret is stored in KeyVault and KeyVault credentials are stored in application.properties file.
https://github.com/prdpsvs/io.deltastandalone.springboot/blob/8fa502050b25bae67db51810c888f7fdbca45438/src/main/java/com/delta/standalone/lib/Service/ConfigurationService.java#L20-L31
  - Client Id, Client Secret and Tenant Id values are stored as a secret in Key Vault. Store the secret in following format.
    ```
    {
      "clientId": "",
      "clientSecret": "",
      "tenantId": ""
    }
    ```
  - Key Vault credentials are stored in application.properties file within project structure. Add following properties to application.properties file. The below code will fetch the secret from KeyVault
https://github.com/prdpsvs/io.deltastandalone.springboot/blob/8fa502050b25bae67db51810c888f7fdbca45438/src/main/java/com/delta/standalone/lib/Service/KeyVaultClientProvider.java#L17
    ```
      azure.key-vault.clientId=  
      azure.key-vault.clientSecret=  
      azure.key-vault.endpoint=  
      azure.key-vault.tenantId= 
    ```

### How to use Delta Standalone?

Now that you know the most important classes to read delta log and pre-requisites, let’s get right into an example. This delta standalone example is wrapped by a spring boot application with DatasetController class. DatasetController class has many request mappings. One of the request mappings is getDatasetfilesToRead method to get the delta files paths to read based on inputs and configuration provided.

* Request Mapping - getDatasetfilesToRead
https://github.com/prdpsvs/io.deltastandalone.springboot/blob/8fa502050b25bae67db51810c888f7fdbca45438/src/main/java/com/delta/standalone/lib/Controller/DatasetController.java#L20-L28

  * Initialize DeltaLog class to read the dataset from storage configuration and user input. The below line uses transtive hadoop dependency 'org.apache.hadoop.conf.Configuration' to use underlying log store (in this case, Azure Log Store) api to connect to storage account.
https://github.com/prdpsvs/io.deltastandalone.springboot/blob/8fa502050b25bae67db51810c888f7fdbca45438/src/main/java/com/delta/standalone/lib/Service/DatasetService.java#L71
  * Get the latest snapshot, schema of dataset and apply partition pruning rules
https://github.com/prdpsvs/io.deltastandalone.springboot/blob/8fa502050b25bae67db51810c888f7fdbca45438/src/main/java/com/delta/standalone/lib/Service/DatasetService.java#L73-L75
https://github.com/prdpsvs/io.deltastandalone.springboot/blob/8fa502050b25bae67db51810c888f7fdbca45438/src/main/java/com/delta/standalone/lib/Service/DatasetService.java#L82-L87
https://github.com/prdpsvs/io.deltastandalone.springboot/blob/8fa502050b25bae67db51810c888f7fdbca45438/src/main/java/com/delta/standalone/lib/Service/DatasetService.java#L144-L152
  * Non partition columns data filtering (Residual Predicate) - TBD  
Above steps will provide the list of files for a given version of a dataset.

* Request Mapping - getDatasetRecords (TBD)





