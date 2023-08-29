package com.mongodb.ps.qedemo.model;


import java.util.ArrayList;
import java.util.List;

class QueryTypeDoc {
    public String queryType;

    public QueryTypeDoc() {
        this.queryType = "equality";
    }
}

class FieldDoc {
    public String path;
    public String bsonType;
    public List<QueryTypeDoc> queries;
    public String keyId = null;

    public FieldDoc(String path, String bsonType, boolean queryable) {
        this.path = path;
        this.bsonType = bsonType;
        if (queryable) {
            this.queries = new ArrayList<>();
            queries.add(new QueryTypeDoc());
        }
    }
}

public class QEEncFieldMap {
    public List<FieldDoc> fields;

    public QEEncFieldMap() {
        fields = new ArrayList<>();
    }

    public void addField(FieldDoc f) {
        fields.add(f);
    }
}
