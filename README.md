### io.deltastandalone.springboot

[![Java CI with Maven](https://github.com/prdpsvs/io.deltastandalone.springboot/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/prdpsvs/io.deltastandalone.springboot/actions/workflows/maven.yml)


### Add following properties to application.properties file

azure.key-vault.clientId=  
azure.key-vault.clientSecret=  
azure.key-vault.endpoint=  
azure.key-vault.tenantId=  


### Delta Format Use cases outside Spark

Delta lake format is becoming a new open-source standard for data analytics. But why?
* ACID Support
* Time Travel
* Schema Evolution

Primarly, deltalake format standardization is created in big data enigneering and analytical applications. Can we read the datasets stored in delta format outside Spark using standard libraries? Yes, we can however, we need to figure out a way how transaction log (a.k.a _delta_log) is interpreted to be able to read a specific version of data set.
What consists of Transaction Log?
Whenever user modifies a dataset (insert/update/delete), delta lake breaks the operation into series of steps composed of one or more action. A few examples of an action are Add file, Remove file, Update metadata etc. All these actions are recorded as atomic steps called commits stored as a json file. Each action on a dataset is a commit, resulting in a json (000000.json) file. 10 commits become a checkpoint (0000010.checkpoint. parquet) file.  
Is Transaction Log user friendly to understand?
Yes, understanding each commit json file is very straight forward. It contains the following metadata
1.	commitInfo – commitInfo object has timestamp, type of operation, operation metrics, operation parameters, readversion and isBlindAppend
2.	Series of actions
a.	If the action is add/remove, then it has path, partitionValues (if any), size, modification time, data change and stats.
b.	Stats contains minimum and maximum values of all columns stored in a file.
Are there any ways to read and Interpret Transaction Log?
Yes, there are couple of ways to read and interpret transaction log files under _delta_log folder.
1.	As each commit or series of commits (checkpoint) are json files, its easy-to-read json contents. I would not prefer this approach for two reasons, 
a.	This one is obvious, Rebuilding metadata by reading json content for each commit since last checkpoint can be cumbersome. Note the change in transaction log metadata structure can lead to read failures (read change protocol action).
b.	Delta format adheres to optimistic concurrency. What happens if two or more users are reading the dataset while you are writing or vise verse?
2.	Is there a better way to read delta format datasets? Yes, the delta standalone library is a solution. It is a single node java library that can be used to read from and write to delta datasets on file storage. This file storage can be ADLS Gen2, Windows/Linux file systems, S3 buckets or another file store that supports HDFS api’s.
What is Delta Standalone Library?

Delta Standalone library provides APIs to interact with a table’s metadata in the transaction log, implementing the Delta Transaction Log Protocol to achieve the transactional guarantees of the Delta Lake format. The good part is that this library does not depend on Apache Spark and has only a few transitive dependencies, therefore it can be used by any compute (Web Api’s, Azure Functions, Web Jobs with combination of MPP systems such as SQL databases/data warehouses).

Where can I use this Library?
If you observe this library closely, you will notice that the power of this library is not to read the actual data but the metadata (transaction log a.k.a _delta_log). Now let’s define use cases where we can use this library?
1.	Synapse Data Warehouse or any other databases on Azure stack can’t read datasets in delta format. Can we use this library to retrieve files injunction with MPP or database systems that have compute power to read parquet files? 
2.	Can your background services, micro services, HTAP services read datasets from ADLS G2 or any other storage instead of storage all the datasets in a sql layer by duplicating data for transactional and analytical needs?
3.	Can various ELT services leverage this library as a metadata layer and skip the usage of Spark simply to read delta log?

How to use Delta Standalone Library?

This library is simple to use. You need to know about three classes to successfully implement delta reads of a dataset.
1.	DeltaLog – is the interface/class to programmatically interact with the metadata in transaction log (under _delta_log folder) for a dataset. This class provides access to the snapshot class in context of reading a dataset 
2.	Snapshot & DeltaScan – snapshot represents the state of a dataset at a specific version. DeltaLog class also provides a way to read a version using getSnapshotForTimestampAsOf or getSnapshotForVersionAsOf. DeltaScan provides memory-optimized iterator over metadata files optionally by passing in a partition filtering predicate (partition pruning)
3.	OptimisticTransaction – This is a main class to set the updates to the transaction log. During a transaction all reads must be done using OptimisticTransaction instead of DeltaLog in order to detect conflicts and concurrent updates.

Now that you know the most important classes to read delta log, let’s get right into an example:




