package com.mongodb.ps.qedemo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BenchClient {
    protected MongoCollection<Document> coll;
    protected List<String> hits;
    protected String uri;
    protected Map<String, Object> metadata;
    protected Map<String, Object> connParams;
    protected String dbName;
    protected String collName;
    protected MongoClient client;

    public BenchClient(String uri, Map<String, Object> metadata) {
        this.uri = uri;
        this.metadata = metadata;
        this.connParams = new HashMap<>();
        this.dbName = (String) metadata.get("db");
        this.collName = (String) metadata.get("coll");
    }

    public void initClient() {
        var clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .build();
        client = MongoClients.create(clientSettings);
        coll = client.getDatabase(dbName).getCollection(collName);
        coll.drop();
    }

    public void insert(int numDocs) {
        long start = now();
        hits = RandomDocs.create(numDocs, coll, getName());
        var duration = (now() - start) / 1000.;
        System.out.printf("%s inserted %d docs in %s seconds%n", getName(), numDocs, duration);
    }

    public void batchedInsert(int numDocs, int batchSize) {
        long start = now();
        hits = RandomDocs.batchedInsert(numDocs, coll, getName(), batchSize);
        var duration = (now() - start) / 1000.;
        System.out.printf("%s inserted %d docs in %s seconds%n", getName(), numDocs, duration);
    }

    public void find() {
        var misses = RandomDocs.getMisses(hits);
        List<String> finds = new ArrayList<>();
        finds.addAll(misses);
        finds.addAll(hits);
        Collections.shuffle(hits);
        var found = 0;
        var missed = 0;
        var start = now();
        for (String f : finds) {
            var sd = new Document();
            sd.put("patientRecord.ssn", f);
            var findIter = coll.find(sd);
            try (var c = findIter.cursor()) {
                if (c.hasNext()) {
                    found++;
                } else {
                    missed++;
                }
            }
        }
        var duration = (now() - start)/1000.;
        System.out.printf("%s found %d and missed %d in %s seconds%n", getName(), found, missed, duration);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    public abstract String getName();
}
