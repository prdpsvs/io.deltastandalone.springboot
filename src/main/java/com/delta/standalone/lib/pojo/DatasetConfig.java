package com.delta.standalone.lib.pojo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;


public class DatasetConfig {

    @JsonProperty("DatasetStorageName")
    public String datasetStorageName;
    @JsonProperty("DatasetContainerName")
    public String datasetContainerName;
    @JsonProperty("DatasetFolderPath")
    public String datasetFolderPath;
    @JsonProperty("StorageConnectionSecretName")
    public String storageConnectionSecretName;
    @JsonProperty("DatasetRules")
    public ArrayList<DatasetRule> datasetRules;

    public String getDatasetPath() {
        return "abfss://"+datasetContainerName+"@"+datasetStorageName+".dfs.core.windows.net"+datasetFolderPath;
    }

    public String getDatasetBasePath() {
        return "abfss://"+datasetContainerName+"@"+datasetStorageName+".dfs.core.windows.net/";
    }

}
