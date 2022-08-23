package com.delta.standalone.lib.Service;

import com.delta.standalone.lib.Exception.ItemNotFoundException;
import com.delta.standalone.lib.pojo.DatasetConfig;
import com.delta.standalone.lib.pojo.DatasetResponse;
import com.delta.standalone.lib.pojo.DatasetRule;
import io.delta.standalone.DeltaLog;
import io.delta.standalone.DeltaScan;
import io.delta.standalone.Snapshot;
import io.delta.standalone.actions.AddFile;
import io.delta.standalone.data.CloseableIterator;
import io.delta.standalone.expressions.And;
import io.delta.standalone.expressions.EqualTo;
import io.delta.standalone.expressions.Expression;
import io.delta.standalone.expressions.Literal;
import io.delta.standalone.types.StructType;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.webapp.ResponseInfo;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DatasetService {

    ConfigurationService _configHandler;
    DatasetResponse _datasetResponse;

    @Autowired
    public DatasetService(ConfigurationService configurationService, DatasetResponse datasetResponse) {
        this._configHandler = configurationService;
        this._datasetResponse = datasetResponse;
    }

    public DatasetResponse ProcessDataset(DatasetConfig datasetConfig) throws ItemNotFoundException, IOException, ParseException {

        try {
            if (datasetConfig.datasetContainerName == null || datasetConfig.datasetContainerName.trim().isEmpty())
                throw new ItemNotFoundException("Dataset Container name is not provided in the request.");

            if (datasetConfig.datasetFolderPath == null || datasetConfig.datasetFolderPath.trim().isEmpty())
                throw new ItemNotFoundException("Dataset folder path is not provided in the request.");

            if (datasetConfig.storageConnectionSecretName == null || datasetConfig.storageConnectionSecretName.trim().isEmpty())
                throw new ItemNotFoundException("Storage connection secret configuration is not provided in the request.");

            if (datasetConfig.datasetStorageName == null || datasetConfig.datasetStorageName.trim().isEmpty())
                throw new ItemNotFoundException("Dataset storage account name is not provided in the request.");
        } catch (ItemNotFoundException e) {
            System.out.println(e.fillInStackTrace());
            throw e;
        }
        Configuration conf;
        List<String> filePathsToReturn = new ArrayList<>();

        try {
            // Applies storage configuration to read delta log of a dataset from storage account
            conf = _configHandler.setStorageConfiguration(datasetConfig.datasetStorageName, datasetConfig.storageConnectionSecretName);
        } catch (IOException e) {
            throw e;
        }

        // Read delta log file for a given dataset
        DeltaLog log = DeltaLog.forTable(conf, datasetConfig.getDatasetPath());

        // Get the latest snapshot of the dataset
        // Future versions will support asOfVersion And TimeStamp
        Snapshot latestSnapshot = log.update();

        // Update dataset response with storage dataset path and version of the dataset used
        this._datasetResponse.setDatasetVersion(latestSnapshot.getVersion());
        this._datasetResponse.setDatasetBasePath(datasetConfig.getDatasetBasePath());
        this._datasetResponse.setDatasetFilePaths(filePathsToReturn);

        // Get the dataset schema of the Latest snapshot
        StructType schema = latestSnapshot.getMetadata().getSchema();

        // Apply partition pruning on dataset by using the dataset rules provided by user
        // Partition pruning will filter the number of files based on how dataset is partitioned.
        DeltaScan scan = applyPartitionPruningOnDataset(latestSnapshot, schema, datasetConfig.datasetRules);
        CloseableIterator<AddFile> fileIterator = scan.getFiles();

        // Get column filters which should be applied further on dataset
        ArrayList<DatasetRule> nonPRules = null;
        if (datasetConfig.datasetRules != null)
            nonPRules = (ArrayList<DatasetRule>) datasetConfig.datasetRules.stream().filter(r -> !r.isPartitioned).collect(Collectors.toList());

        // Iterate through each file post partitioning pruning
        while (fileIterator.hasNext()) {
            AddFile addFile = fileIterator.next();
            System.out.println("Checking stats for file -> " + addFile.getPath());

            // If no column rules are provided, add the file to filePathsToReturn
            if (nonPRules != null && nonPRules.size() > 0) {

                // Get minimum and maximum stats of all columns for each file from _delta_log
                JSONObject stats = new JSONObject(addFile.getStats());
                JSONObject minValues = new JSONObject(stats.get("minValues").toString());
                JSONObject maxValues = new JSONObject(stats.get("maxValues").toString());

                List<Boolean> ruleResults = new ArrayList<>();
                boolean result = true;
                // Apply filters values from the input and return if file should be returned or not.
                applyNPRules(filePathsToReturn, nonPRules, addFile, stats, minValues, maxValues, ruleResults, result);
            } else
                filePathsToReturn.add(addFile.getPath());
        }

        return _datasetResponse;
    }


    private void applyNPRules(List<String> filesToReturn, ArrayList<DatasetRule> nonPRules, AddFile addFile, JSONObject stats, JSONObject minValues, JSONObject maxValues, List<Boolean> ruleResults, boolean result) throws ParseException {

        // Go through each rule and check if the value of a columns is available within file stats
        for (DatasetRule rule : nonPRules) {
            //Check if column value is available in the file and add to the list
            System.out.println("Column Name to Filter -> " + rule.columnName);
            if((stats.isEmpty() || minValues.isEmpty() || maxValues.isEmpty()))
                ruleResults.add(true);
            // Verify if a column value is in between min and max value. If true, add the flag to ruleResults list
            else if ((!stats.isEmpty() && !minValues.isEmpty() && !maxValues.isEmpty()) && checkDataStatsOnFile(rule, minValues, maxValues))
                ruleResults.add(true);
            else ruleResults.add(false);
        }

        // Perform and Operation on all rules.
        for (Boolean el: ruleResults) {
            result = el && result;
        }

        // If all rules match the criteria, add the file to the list of file path to return as part of this request.
        if(result)
            filesToReturn.add(addFile.getPath());
    }

    private DeltaScan applyPartitionPruningOnDataset(Snapshot latestSnapshot, StructType schema, ArrayList<DatasetRule> rules) {

        // If no partitions rules are provided in the input, get the full dataset scan
        if (rules == null || rules.stream().noneMatch(r -> r.isPartitioned))
            return latestSnapshot.scan();
        else
            // Apply partition pruning on all columns provided in the input
            return latestSnapshot.scan(applyPartitionedColumnRules(schema, rules.stream().filter(r -> r.isPartitioned).collect(Collectors.toList())));
    }
    private Expression applyPartitionedColumnRules(StructType schema, List<DatasetRule> rules) {
        Expression ex;
        int rulesSize = rules.size();
        ex = getExpression(schema, rules.get(0));
        for (int i = 1; i < rulesSize; i++)
            ex = new And(ex, getExpression(schema, rules.get(i)));
        return ex;
    }

    private Expression getExpression(StructType schema, DatasetRule rule) {
        return new EqualTo(schema.column(rule.columnName), getDataType(rule.value, rule.dataType.toLowerCase()));
    }

    private Literal getDataType(Object value, String valueType) {
        Literal l;
        switch (valueType) {
            case "int":
                l = Literal.of((Integer)value);
                break;
            case "float":
                l = Literal.of((Float)value);
                break;
            case "double":
                l = Literal.of((Double)value);
                break;
            case "long":
                l = Literal.of((Long)value);
                break;
            default:
                l = Literal.of((String)value);
                break;
        }
        return l;
    }

    private boolean checkDataStatsOnFile(DatasetRule rule, JSONObject minValues, JSONObject maxValues) throws ParseException {

        System.out.println("Column Value To Filter -> " + rule.value);

        if(minValues.has(rule.columnName) & maxValues.has(rule.columnName)) {

            System.out.println("Min Value -> " + minValues.get(rule.columnName));
            System.out.println("Max Value -> " + maxValues.get(rule.columnName));

            if(rule.dataType.equalsIgnoreCase("int")) {
                int value = (Integer)rule.value;
                return value >= minValues.getInt(rule.columnName) && value <= maxValues.getInt(rule.columnName);
            }
            else if(rule.dataType.equalsIgnoreCase("float")) {
                Float value = (Float)rule.value;
                return value >= minValues.getFloat(rule.columnName) && value <= maxValues.getFloat(rule.columnName);
            }
            else if(rule.dataType.equalsIgnoreCase("double")) {
                Double value = (Double)rule.value;
                return value >= minValues.getDouble(rule.columnName) && value <= maxValues.getDouble(rule.columnName);
            }
            else if(rule.dataType.equalsIgnoreCase("date")) {
                DateFormat df = new SimpleDateFormat(rule.valueFormat);
                Date value = df.parse((String)rule.value);
                return df.parse(minValues.getString(rule.columnName)).after(value) && df.parse(maxValues.getString(rule.columnName)).before(value);
            }
            else{
                String value = (String) rule.value;
                return value.compareTo(minValues.getString(rule.columnName)) >= 1 && value.compareTo(maxValues.getString(rule.columnName)) <= 1;
            }
        }
        System.out.println("Stat Not Found");
        return false;
    }
}
