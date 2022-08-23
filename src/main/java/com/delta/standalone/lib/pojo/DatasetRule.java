package com.delta.standalone.lib.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DatasetRule{
    @JsonProperty("ColumnName")
    public String columnName;
    @JsonProperty("DataType")
    public String dataType;
    @JsonProperty("Value")
    public Object value;
    @JsonProperty("IsPartitioned")
    public boolean isPartitioned;
    @JsonProperty("ValueFormat")
    public String valueFormat;

}
