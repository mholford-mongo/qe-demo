package com.mongodb.ps.qedemo.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.BsonDocument;
import org.bson.Document;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SchemaInfo {
    private final String dbName;
    private final String collName;
    private final List<FieldInfo> fields;
    private Map<String, FieldInfo> fieldInfoMap = new HashMap<>();
    private ObjectMapper om = new ObjectMapper();

    public SchemaInfo(String dbName, String collName, List<FieldInfo> fields) {
        this.dbName = dbName;
        this.collName = collName;
        this.fields = fields;
        for (FieldInfo f : fields) {
            fieldInfoMap.put(f.getName(), f);
        }
    }


    public FieldInfo getFieldInfo(String field) {
        return fieldInfoMap.get(field);
    }

    public List<FieldInfo> queryableFields() {
        return fields.stream().filter(FieldInfo::isQueryable).collect(Collectors.toList());
    }

    public Document encryptedFieldMap() {
        var qeMap = new QEEncFieldMap();
        for (FieldInfo fi : fields) {
            qeMap.addField(new FieldDoc(fi.getName(), fi.getType(), fi.isQueryable()));
        }
        var w = new StringWriter();
        try {
            om.writeValue(w, qeMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            System.out.println("QE EncryptedFieldMap::\n" + om.writerWithDefaultPrettyPrinter().writeValueAsString(qeMap));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Document.parse(w.toString());
    }

    public BsonDocument csfleFieldMap(String dek, String subType) {
        var m = new CSFLEEncFieldMap("object", dek, subType);
        for (FieldInfo fi : fields) {
            //  need to nest the fields
            var fname = fi.getName();
            var fnameSplits = Arrays.asList(fname.split("\\."));
            var fsIter = fnameSplits.iterator();
            var curDoc = m.properties;
            while (fsIter.hasNext()) {
                var curName = fsIter.next();
                if (fsIter.hasNext()) {
                    if (!curDoc.propMap.containsKey(curName)) {
                        var newPropertiesDoc = new PropertiesDoc();
                        var propDoc = new PropDoc("object", newPropertiesDoc);
                        curDoc.add(curName, propDoc);
                        curDoc = newPropertiesDoc;
                    } else {
                        var curPropDoc = (PropDoc) curDoc.propMap.get(curName);
                        curDoc = curPropDoc.properties;
                    }

                } else {
                    // A leaf node
                    var encDoc = new EncDoc(fi.getType(), fi.isQueryable());
                    var encDocDoc = new EncDocDoc(encDoc);
                    curDoc.add(curName, encDocDoc);
                }
            }
        }

        var w = new StringWriter();
        try {
            om.writeValue(w, m);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            System.out.println("CSFLE EncryptedFieldMap::\n" + om.writerWithDefaultPrettyPrinter().writeValueAsString(m));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return BsonDocument.parse(w.toString());
    }
    public String getDbName() {
        return dbName;
    }

    public String getCollName() {
        return collName;
    }
}
