package com.mongodb.ps.qedemo;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryptions;
import com.mongodb.ps.qedemo.model.SchemaInfo;
import org.bson.BsonDocument;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AutoCSFLEClient extends BenchClient {
    private final String kvNs;
    private final Map<String, Map<String, Object>> kmsProviderCreds;
    AutoEncryptionSettings autoEncSettings;
    String kmsProvider;

    public AutoCSFLEClient(String uri, Map<String, Object> metadata, SchemaInfo schema) {
        super(uri, metadata, schema);
        kmsProvider = (String) metadata.get("kmsProvider");
        String keyvaultDbName = (String) metadata.get("keyvaultDbName");
        String keyvaultCollName = (String) metadata.get("keyvaultCollName");
        kvNs = String.format("%s.%s", keyvaultDbName, keyvaultCollName);
        kmsProviderCreds = Helpers.getKmsProviderCredentials(kmsProvider);
        autoEncSettings = autoEnc(kvNs, kmsProviderCreds);
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
        hitMap = new HashMap<>();
    }

    private AutoEncryptionSettings autoEnc(String kvNs, Map<String, Map<String, Object>> kmsProviderCreds) {
        var schemaMap = schema.csfleFieldMap(getDek(), "04");
        Map<String, BsonDocument> schemaMapMap = new HashMap<>();
        schemaMapMap.put(String.format("%s.%s", schema.getDbName(), schema.getCollName()), schemaMap);
        Map<String, Object> extras = new HashMap<>();
        extras.put("cryptSharedLibPath", Helpers.env("SHARED_LIB_PATH"));
        return AutoEncryptionSettings.builder()
                .keyVaultNamespace(kvNs)
                .kmsProviders(kmsProviderCreds)
                .schemaMap(schemaMapMap)
                .extraOptions(extras)
                .build();
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
        return "AutoCSFLEClient";
    }
}
