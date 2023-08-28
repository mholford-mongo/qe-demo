package com.mongodb.ps.qedemo.model;

import java.util.function.Supplier;

public class FieldInfo {
    private final String name;
    private final String type;
    private final boolean queryable;
    private final Supplier<Object> randomizer;

    public FieldInfo(String name, String type, boolean queryable, Supplier<Object> randomizer) {
        this.name = name;
        this.type = type;
        this.queryable = queryable;
        this.randomizer = randomizer;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isQueryable() {
        return queryable;
    }

    public Object getRandomValue() {
        return randomizer.get();
    }
}
