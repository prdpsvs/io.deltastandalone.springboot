package com.delta.standalone.lib.pojo;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatasetResponse {
    private long datasetVersion;
    private List<String> datasetFilePaths;
    private String datasetBasePath;

    public long getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(long datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public List<String> getDatasetFilePaths() {
        return datasetFilePaths;
    }

    public void setDatasetFilePaths(List<String> datasetFilePaths) {
        this.datasetFilePaths = datasetFilePaths;
    }

    public String getDatasetBasePath() {
        return datasetBasePath;
    }

    public void setDatasetBasePath(String datasetBasePath) {
        this.datasetBasePath = datasetBasePath;
    }
}
