package com.mongodb.ps.qedemo;

import java.util.Map;

public class PlainClient extends BenchClient{
    public PlainClient(String uri, Map<String, Object> metadata) {
        super(uri, metadata);
    }

    @Override
    public String getName() {
        return "com.mongodb.ps.qedemo.PlainClient";
    }
}
