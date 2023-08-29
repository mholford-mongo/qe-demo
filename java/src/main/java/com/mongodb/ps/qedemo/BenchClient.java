package com.mongodb.ps.qedemo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.ps.qedemo.model.FieldInfo;
import com.mongodb.ps.qedemo.model.SchemaInfo;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.ps.qedemo.RandomDocs.randDoc;
import static com.mongodb.ps.qedemo.Utils.getMisses;
import static com.mongodb.ps.qedemo.Utils.getNested;

public class BenchClient {
    protected MongoCollection<Document> coll;
    protected List<String> hits;
    protected String uri;
    protected Map<String, Object> metadata;
    protected Map<String, Object> connParams;
    protected String dbName;
    protected String collName;
    protected MongoClient client;
    protected SchemaInfo schema;
    protected Map<String, List<Object>> hitMap;
    protected int hitRate;
    protected int reportRate;

    public BenchClient(String uri, Map<String, Object> metadata, SchemaInfo schema) {
        this.uri = uri;
        this.metadata = metadata;
        this.connParams = new HashMap<>();
        this.dbName = schema.getDbName();
        this.collName = schema.getCollName();
        this.hitRate = (int) metadata.get("hitRate");
        this.reportRate = (int) metadata.get("reportRate");
        this.schema = schema;
    }

    public void initClient() {
        var clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .build();
        client = MongoClients.create(clientSettings);
        coll = client.getDatabase(dbName).getCollection(collName);
        coll.drop();
        hitMap = new HashMap<>();
    }

    public void insert(int numDocs) {
        long start = now();
        for (int i = 0; i < numDocs; i++) {
            var doc = randDoc();
            if (i % hitRate == 0) {
                for (FieldInfo fi : schema.queryableFields()) {
                    String fname = fi.getName();
                    if (!hitMap.containsKey(fname)) {
                        hitMap.put(fname, new ArrayList<>());
                    }
                    var val = getNested(fname, doc);
                    hitMap.get(fname).add(val);
                }
            }
            coll.insertOne(doc);
            if (i % reportRate == 0) {
                System.out.printf("%s inserted %d of %d%n", getName(), i, numDocs);
            }
        }
        var duration = (now() - start) / 1000.;
        System.out.printf("%s inserted %d docs in %s seconds%n", getName(), numDocs, duration);
    }

    public void batchedInsert(int numDocs, int batchSize) {
        long start = now();
        List<Document> batch = new ArrayList<>();
        int i = 0;
        while (i < numDocs) {
            if (batch.size() == batchSize) {
                coll.insertMany(batch);
                System.out.printf("%s inserted a batch; Total count: %d out of %d%n", getName(), i, numDocs);
                batch = new ArrayList<>();
            }
            var doc = randDoc();
            if (i % hitRate == 0) {
                for (FieldInfo fi : schema.queryableFields()) {
                    String fname = fi.getName();
                    if (!hitMap.containsKey(fname)) {
                        hitMap.put(fname, new ArrayList<>());
                    }
                    var val = getNested(fname, doc);
                    hitMap.get(fname).add(val);
                }
            }
            batch.add(doc);
            i++;
        }
        coll.insertMany(batch);
        System.out.printf("Inserted a batch; Total count: %d out of %d%n", i, numDocs);
        var duration = (now() - start) / 1000.;
        System.out.printf("%s inserted %d docs in %s seconds%n", getName(), numDocs, duration);
    }

    public void find() {
        var found = 0;
        var missed = 0;
        var start = now();
        for (FieldInfo fi : schema.queryableFields()) {
            String fname = fi.getName();
            var hits = hitMap.get(fname);
            var misses = getMisses(hits, fname, schema);
            List<Object> finds = new ArrayList<>();
            finds.addAll(hits);
            finds.addAll(misses);
            Collections.shuffle(finds);

            for (Object f : finds) {
                var sd = new Document();
                sd.put(fname, f);
                var findIter = coll.find(sd);
                try (var c = findIter.cursor()) {
                    if (c.hasNext()) {
                        found++;
                    } else {
                        missed++;
                    }
                }
            }
        }
        var duration = (now() - start) / 1000.;
        System.out.printf("%s found %d and missed %d in %s seconds%n", getName(), found, missed, duration);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    public String getName() {
        return "PlainClient";
    }
}
