package com.mongodb.ps.qedemo.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;

import java.io.IOException;
import java.io.StringWriter;
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
    private ObjectMapper om;

    public SchemaInfo(String dbName, String collName, List<FieldInfo> fields) {
        this.dbName = dbName;
        this.collName = collName;
        this.fields = fields;
        for (FieldInfo f : fields) {
            fieldInfoMap.put(f.getName(), f);
        }
    }

    public String getDbName() {
        return dbName;
    }

    public String getCollName() {
        return collName;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public FieldInfo getFieldInfo(String field){
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
        return Document.parse(w.toString());
    }
}
