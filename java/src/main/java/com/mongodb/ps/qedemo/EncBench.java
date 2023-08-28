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
        var schema = new SchemaInfo("medicalRecords", "patients", fields);

        execPlainClient(uri, numDocs);
//
        execQEClient(uri, numDocs);

        execAutoCSFLEClient(uri, numDocs);
    }

    private static void execQEClient(String uri, int numDocs) {
        Map<String, Object> meta = Map.of("kmsProvider", "local",
                "keyvaultDbName", "encryption", "keyvaultCollName",
                "__keyVault", "db", "medicalRecords", "coll", "patients");
        var qeClient = new QEClient(uri, meta);
        System.out.println("Benchmarking com.mongodb.ps.qedemo.QEClient");
        qeClient.initClient();
        qeClient.insert(numDocs);
        qeClient.find();
    }

    private static void execAutoCSFLEClient(String uri, int numDocs) {
        Map<String, Object> meta = Map.of("kmsProvider", "local",
                "keyvaultDbName", "encryption", "keyvaultCollName",
                "__keyVault", "db", "medicalRecords", "coll", "patients");
        var autoCsfleClient = new AutoCSFLEClient(uri, meta);
        System.out.println("Benchmarking com.mongodb.ps.qedemo.AutoCSFLEClient");
        autoCsfleClient.initClient();
        autoCsfleClient.insert(numDocs);
        autoCsfleClient.find();
    }

    private static void execPlainClient(String uri, int numDocs) {
        int batchSize = 1000;
        Map<String, Object> meta = Map.of("db", "medicalRecords", "coll", "patients");
        var plainClient = new PlainClient(uri, meta);
        System.out.println("Benchmarking com.mongodb.ps.qedemo.PlainClient");
        plainClient.initClient();
        plainClient.batchedInsert(numDocs, batchSize);
        plainClient.find();
    }

    public static void main(String[] args) {
        new EncBench().exec();
    }
}
