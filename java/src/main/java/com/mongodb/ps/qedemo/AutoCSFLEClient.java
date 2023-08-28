package com.mongodb.ps.qedemo;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryptions;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AutoCSFLEClient extends BenchClient {
    private final String keyvaultDbName;
    private final String keyvaultCollName;
    private final String kvNs;
    private final Map<String, Map<String, Object>> kmsProviderCreds;
    AutoEncryptionSettings autoEncSettings;
    String kmsProvider;

    public AutoCSFLEClient(String uri, Map<String, Object> metadata) {
        super(uri, metadata);
        kmsProvider = (String) metadata.get("kmsProvider");
        keyvaultDbName = (String) metadata.get("keyvaultDbName");
        keyvaultCollName = (String) metadata.get("keyvaultCollName");
        kvNs = String.format("%s.%s", keyvaultDbName, keyvaultCollName);
        kmsProviderCreds = Helpers.getKmsProviderCredentials(kmsProvider);
        autoEncSettings = autoEnc(kmsProvider, kvNs, kmsProviderCreds);
    }

    @Override
    public void initClient() {
        var clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .autoEncryptionSettings(autoEncSettings)
                .build();
        client = MongoClients.create(clientSettings);
        coll = client.getDatabase(dbName).getCollection(collName);
        coll.drop();
//        client.getDatabase(keyvaultDbName).getCollection(keyvaultCollName).drop();
    }

    private AutoEncryptionSettings autoEnc(String kmsProvider, String kvNs,
                                           Map<String, Map<String, Object>> kmsProviderCreds) {
        var schemaMap = getSchemaMap();
        Map<String, BsonDocument> schemaMapMap = new HashMap<>();
        schemaMapMap.put(String.format("%s.%s", metadata.get("db"), metadata.get("coll")), schemaMap);
        Map<String, Object> extras = new HashMap<>();
        extras.put("cryptSharedLibPath", Helpers.env("SHARED_LIB_PATH"));
        return AutoEncryptionSettings.builder()
                .keyVaultNamespace(kvNs)
                .kmsProviders(kmsProviderCreds)
                .schemaMap(schemaMapMap)
                .extraOptions(extras)
                .build();
    }

    private BsonDocument getSchemaMap() {
        Document jsonSchema = new Document()
                .append("bsonType", "object")
                .append("encryptMetadata", new Document()
                        .append("keyId", new ArrayList<>((Arrays.asList(new Document()
                                .append("$binary", new Document()
                                        .append("base64", getDek())
                                        .append("subType", "04")))))))
                .append("properties", new Document()
                        .append("patientRecord", new Document()
                                .append("bsonType", "object")
                                .append("properties", new Document()
                                        .append("ssn", new Document()
                                                .append("encrypt", new Document()
                                                        .append("bsonType", "string")
                                                        .append("algorithm",
                                                                "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic")))
                                        .append("billing", new Document()
                                                .append("bsonType", "object")
                                                .append("properties", new Document()
                                                        .append("number", new Document()
                                                                .append("encrypt", new Document()
                                                                        .append("bsonType", "int")
                                                                        .append("algorithm",
                                                                                "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                                                                        ))))))));
        return BsonDocument.parse(jsonSchema.toJson());
    }

    private String getDek() {
        var clientEncSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .build())
                .keyVaultNamespace(kvNs)
                .kmsProviders(kmsProviderCreds)
                .build();
        var clientEnc = ClientEncryptions.create(clientEncSettings);
        var dataKeyId = clientEnc.createDataKey(kmsProvider, new DataKeyOptions());
        var base64DataKeyId = Base64.getEncoder().encodeToString(dataKeyId.getData());
        System.out.println("DataKeyId [base64]: " + base64DataKeyId);
        clientEnc.close();
        return base64DataKeyId;
    }

    @Override
    public String getName() {
        return "com.mongodb.ps.qedemo.AutoCSFLEClient";
    }
}
