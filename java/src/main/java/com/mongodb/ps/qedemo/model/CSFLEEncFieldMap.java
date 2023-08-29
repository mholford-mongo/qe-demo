package com.mongodb.ps.qedemo.model;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EMDoc {
    public List<BinaryDoc> keyId;

    public EMDoc(String base64, String subType) {
        this.keyId = List.of(new BinaryDoc(base64, subType));
    }

}

class BinaryDoc {
    @JsonProperty("$binary")
    public BinSubDoc binary;

    public BinaryDoc(String base64, String subType) {
        this.binary = new BinSubDoc(base64, subType);
    }

}

class BinSubDoc {
    public String base64;
    public String subType;

    public BinSubDoc(String base64, String subType) {
        this.base64 = base64;
        this.subType = subType;
    }

}

class PropertiesDoc {
    @JsonAnyGetter
    public Map<String, PropOrEncDoc> propMap;

    public PropertiesDoc() {
        this.propMap = new HashMap<>();
    }

    public void add(String k, PropOrEncDoc d) {
        propMap.put(k, d);
    }
}

interface PropOrEncDoc {

}

class EncDocDoc implements PropOrEncDoc {
    EncDoc encrypt;

    public EncDocDoc(EncDoc encrypt) {
        this.encrypt = encrypt;
    }

    public EncDoc getEncrypt() {
        return encrypt;
    }
}

class EncDoc {
    @JsonIgnore
    String DETERMINISTIC_ALGO = "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic";
    @JsonIgnore
    String RANDOM_ALGO = "AEAD_AES_256_CBC_HMAC_SHA_512-Random";
    String bsonType;
    String algorithm;

    public EncDoc(String bsonType, boolean deterministic) {
        this.algorithm = deterministic ? DETERMINISTIC_ALGO : RANDOM_ALGO;
        this.bsonType = bsonType;
    }

    public String getBsonType() {
        return bsonType;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}

class PropDoc implements PropOrEncDoc {
    public String bsonType;
    public PropertiesDoc properties;

    public PropDoc(String bsonType, PropertiesDoc properties) {
        this.bsonType = bsonType;
        this.properties = properties;
    }

}

public class CSFLEEncFieldMap {

    public String bsonType;
    public EMDoc encryptMetadata;
    public PropertiesDoc properties;

    public CSFLEEncFieldMap(String bsonType, String dek, String subType) {
        this.bsonType = bsonType;
        this.encryptMetadata = new EMDoc(dek, subType);
        this.properties = new PropertiesDoc();
    }

}
