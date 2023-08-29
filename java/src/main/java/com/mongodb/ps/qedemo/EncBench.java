package com.mongodb.ps.qedemo;

import com.mongodb.ps.qedemo.model.FieldInfo;
import com.mongodb.ps.qedemo.model.SchemaInfo;

import java.util.ArrayList;
import java.util.Map;

public class EncBench {
    public void exec() {
        var uri = "mongodb://localhost:27017,localhost:27018,localhost:27019";
        var numDocs = 10_000;
        var fields = new ArrayList<FieldInfo>();
        fields.add(new FieldInfo("patientRecord.ssn", "string", true, RandomDocs::randSsn));
        fields.add(new FieldInfo("patientRecord.billing.number", "int", true, RandomDocs::randBillingNumber));
        var schema = new SchemaInfo("medicalRecords", "patients", fields);

//        execPlainClient(uri, numDocs, schema);
//
//        execQEClient(uri, numDocs, schema);
//
        execAutoCSFLEClient(uri, numDocs, schema);
    }

    private static void execQEClient(String uri, int numDocs, SchemaInfo schema) {
        Map<String, Object> meta = Map.of("kmsProvider", "local",
                "keyvaultDbName", "encryption", "keyvaultCollName",
                "__keyVault", "hitRate", 10, "reportRate", 10);
        var qeClient = new QEClient(uri, meta, schema);
        System.out.println("Benchmarking QEClient");
        qeClient.initClient();
        qeClient.insert(100);
        qeClient.find();
    }

    private static void execAutoCSFLEClient(String uri, int numDocs, SchemaInfo schema) {
        Map<String, Object> meta = Map.of("kmsProvider", "local",
                "keyvaultDbName", "encryption", "keyvaultCollName",
                "__keyVault",  "hitRate", 100, "reportRate", 1000);
        var autoCsfleClient = new AutoCSFLEClient(uri, meta, schema);
        System.out.println("Benchmarking AutoCSFLEClient");
        autoCsfleClient.initClient();
        autoCsfleClient.batchedInsert(numDocs, 1000);
        autoCsfleClient.find();
    }

    private static void execPlainClient(String uri, int numDocs, SchemaInfo schema) {
        int batchSize = 1000;
        Map<String, Object> meta = Map.of("hitRate", 100, "reportRate", 1000);
        var plainClient = new BenchClient(uri, meta, schema);
        System.out.println("Benchmarking com.mongodb.ps.qedemo.PlainClient");
        plainClient.initClient();
        plainClient.batchedInsert(numDocs, batchSize);
        plainClient.find();
    }

    public static void main(String[] args) {
        new EncBench().exec();
    }
}
