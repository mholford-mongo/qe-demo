package com.mongodb.ps.qedemo;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateEncryptedCollectionParams;
import com.mongodb.client.vault.ClientEncryptions;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QEClient extends BenchClient {
    String kmsProvider;
    String keyvaultDbName;
    String keyvaultCollName;
    String kvNs;
    Map<String, Map<String, Object>> kmsProviderCreds;
    AutoEncryptionSettings autoEncSettings;
    Document encFieldMap;

    public QEClient(String uri, Map<String, Object> metadata) {
        super(uri, metadata);
        kmsProvider = (String) metadata.get("kmsProvider");
        keyvaultDbName = (String) metadata.get("keyvaultDbName");
        keyvaultCollName = (String) metadata.get("keyvaultCollName");
        kvNs = String.format("%s.%s", keyvaultDbName, keyvaultCollName);
        kmsProviderCreds = Helpers.getKmsProviderCredentials(kmsProvider);
        autoEncSettings = Helpers.getAutoEncryptionSettings(kmsProvider, kvNs, kmsProviderCreds);
        connParams = new HashMap<>();
        connParams.put("autoEncryptionOpts", autoEncSettings);
        encFieldMap = getEncFieldMap();
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
        client.getDatabase(keyvaultDbName).getCollection(keyvaultCollName).drop();

        var clientEncSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(uri))
                        .build())
                .keyVaultNamespace(kvNs)
                .kmsProviders(kmsProviderCreds)
                .build();
        var clientEnc = ClientEncryptions.create(clientEncSettings);

        var createCollOpts = new CreateCollectionOptions().encryptedFields(encFieldMap);
        var encCollParams = new CreateEncryptedCollectionParams(kmsProvider);
        var masterKeyCreds = Helpers.getMasterKeyCredentials(kmsProvider);
        encCollParams.masterKey(masterKeyCreds);

        clientEnc.createEncryptedCollection(client.getDatabase(dbName), collName, createCollOpts, encCollParams);

    }

    private Document getEncFieldMap() {
        var d = new Document();
        d.put("keyId", null);
        d.put("path", "patientRecord.ssn");
        d.put("bsonType", "string");
        var qtdoc = new Document();
        qtdoc.put("queryType", "equality");
        d.put("queries", qtdoc);
        var d2 = new Document();
        d2.put("keyId", null);
        d2.put("path", "patientRecord.billing");
        d2.put("bsonType", "object");
        var fmdoc = new Document();
        fmdoc.put("fields", List.of(d, d2));
        return fmdoc;
    }

    @Override
    public String getName() {
        return "com.mongodb.ps.qedemo.QEClient";
    }
}
