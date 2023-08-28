package com.mongodb.ps.qedemo;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomDocs {
    public static final int BATCH_SIZE = 1000;
    public static final int HIT_RATE = 10;
    public static final int REPORT_RATE = 100;
    public static final Random rand = new Random();
    public static final List<String> TYPES = new ArrayList<>();

    static {
        TYPES.addAll(List.of("Visa", "Check", "Cash", "MasterCard", "Medicare", "Medicard"));
    }


    public static List<String> getNames() {
        var userDir = System.getProperty("user.dir");
        var filePath = String.format("%s/java/src/main/resources/firstnames", userDir);
        List<String> output = new ArrayList<>();
        try (var br = new BufferedReader(new FileReader(filePath))) {
            String s;
            while ((s = br.readLine()) != null) {
                output.add(s + rand.nextInt(1000));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    public static List<String> batchedInsert(int num, MongoCollection<Document> coll, String name, int batchSize) {
        var names = getNames();
        List<Document> batch = new ArrayList<>();
        List<String> hits = new ArrayList<>();
        int i = 0;
        while (i < num) {
            if (batch.size() == batchSize) {
                coll.insertMany(batch);
                System.out.printf("%s inserted a batch; Total count: %d out of %d%n", name, i, num);
                batch = new ArrayList<>();
            }
            var doc = randDoc(names);
            var pr = (Document) doc.get("patientRecord");
            var ssn =  pr.get("ssn");
            if (i % HIT_RATE == 0) {
                hits.add(ssn.toString());
            }
            batch.add(doc);
            i++;
        }
        coll.insertMany(batch);
        System.out.printf("Inserted a batch; Total count: %d out of %d%n", i, num);

        return hits;
    }

    public static List<String> create(int num, MongoCollection<Document> coll, String name) {
        var names = getNames();
        List<String> hits = new ArrayList<>();
        for (int i = 0; i < num;i++) {
            var doc = randDoc(names);
            var pr = (Document) doc.get("patientRecord");
            var ssn = pr.get("ssn");
            if (i % HIT_RATE == 0) {
                hits.add(ssn.toString());
            }
            coll.insertOne(doc);
            if (i % REPORT_RATE == 0) {
                System.out.printf("%s inserted %d of %d%n", name, i, num);
            }
        }

        return hits;
    }

    public static String randName(List<String> names) {
        int idx = rand.nextInt(names.size());
        return names.get(idx);
    }

    public static String randomType() {
        int idx = rand.nextInt(TYPES.size());
        return TYPES.get(idx);
    }

    public static String digit() {
        return String.valueOf(rand.nextInt(10));
    }

    public static String randSsn() {
        return String.format("%s%s%s-%s%s-%s%s%s%s",
                digit(), digit(), digit(), digit(), digit(), digit(), digit(), digit(), digit());
    }

    public static List<String> getMisses(List<String> hits){
        List<String> output = new ArrayList<>();
        while (output.size() < hits.size()) {
            String cand = randSsn();
            if (!hits.contains(cand)) {
                output.add(cand);
            }
        }
        return output;
    }

    public static Document randDoc(List<String> names) {
        var doc = new Document();
        doc.put("patientName", randName(names));
        doc.put("patientId", rand.nextInt(1_000_000_000));
        var prDoc = new Document();
        prDoc.put("ssn", randSsn());
        var billingDoc = new Document();
        billingDoc.put("type", randomType());
        billingDoc.put("number", rand.nextInt(1_000_000_000));
        prDoc.put("billing", billingDoc);
        doc.put("patientRecord", prDoc);
        return doc;
    }
}
