package com.mongodb.ps.qedemo;

import com.mongodb.ps.qedemo.model.FieldInfo;
import com.mongodb.ps.qedemo.model.SchemaInfo;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static Object getNested(String field, Document doc) {
        var keys = Arrays.asList(field.split("\\."));
        var keyIter = keys.iterator();
        var currDoc = doc;
        while (keyIter.hasNext() && currDoc != null) {
            String k = keyIter.next();
            if (!currDoc.containsKey(k)) {
                throw new RuntimeException(String.format("Key %s not found", field));
            }
            var val = currDoc.get(k);
            if (val instanceof Document) {
                currDoc = (Document) val;
            } else {
                return val;
            }
        }
        throw new RuntimeException(String.format("Key %s not found", field));
    }

    public static List<Object> getMisses(List<Object> hits, String field, SchemaInfo schema) {
        List<Object> output = new ArrayList<>();
        FieldInfo fi = schema.getFieldInfo(field);
        while (output.size() < hits.size()) {
            Object cand = fi.getRandomValue();
            if (!hits.contains(cand)) {
                output.add(cand);
            }
        }
        return  output;
    }
}
