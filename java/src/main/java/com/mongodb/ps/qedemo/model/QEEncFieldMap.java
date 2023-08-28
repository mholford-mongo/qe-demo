package com.mongodb.ps.qedemo.model;


import java.util.ArrayList;
import java.util.List;

class QueryTypeDoc {
    public String queryType;

    public QueryTypeDoc() {
        this.queryType = "equality";
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
}

class FieldDoc {
    public String path;
    public String bsonType;
    public List<QueryTypeDoc> queries;

    public FieldDoc(String path, String bsonType, boolean queryable) {
        this.path = path;
        this.bsonType = bsonType;
        if (queryable) {
            this.queries = new ArrayList<>();
            queries.add(new QueryTypeDoc());
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBsonType() {
        return bsonType;
    }

    public void setBsonType(String bsonType) {
        this.bsonType = bsonType;
    }

    public List<QueryTypeDoc> getQueries() {
        return queries;
    }

    public void setQueries(List<QueryTypeDoc> queries) {
        this.queries = queries;
    }
}

public class QEEncFieldMap {
    public List<FieldDoc> fields;

    public QEEncFieldMap() {
        fields = new ArrayList<>();
    }

    public List<FieldDoc> getFields() {
        return fields;
    }

    public void setFields(List<FieldDoc> fields) {
        this.fields = fields;
    }

    public void addField(FieldDoc f) {
        fields.add(f);
    }
}
