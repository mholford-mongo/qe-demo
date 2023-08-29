package com.mongodb.ps.qedemo;

import com.mongodb.ps.qedemo.model.SchemaInfo;

import java.util.Map;

public class PlainClient extends BenchClient{
    public PlainClient(String uri, Map<String, Object> metadata, SchemaInfo schema) {
        super(uri, metadata, schema);
    }

    @Override
    public String getName() {
        return "PlainClient";
    }
}
