package com.delta.standalone.lib.Controller;

import com.delta.standalone.lib.pojo.DatasetConfig;
import com.delta.standalone.lib.pojo.DatasetResponse;
import com.delta.standalone.lib.Service.DatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class DatasetController {

    private DatasetService _datasetService;

    @Autowired
    public DatasetController(DatasetService datasetService) {
        this._datasetService = datasetService;
    }

    @RequestMapping(value = "/getDatasetFilesToRead", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DatasetResponse getDatasetFilesToRead(@RequestBody DatasetConfig datasetConfig) {
        try {
            return _datasetService.ProcessDataset(datasetConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
