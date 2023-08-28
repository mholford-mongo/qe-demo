package com.mongodb.ps.qedemo;

import com.mongodb.AutoEncryptionSettings;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class Helpers {
    private static final Dotenv dotEnv = Dotenv.configure().directory("./.env").load();

    public static Map<String, Map<String, Object>> getKmsProviderCredentials(String provider) {
        switch (provider) {
            case "local":
                String keyfileName = "customer-master-key.txt";
                var keyfile = new File(keyfileName);
                if (!keyfile.exists()) {
                    var localKey = new byte[96];
                    new SecureRandom().nextBytes(localKey);
                    try (var stream = new FileOutputStream(keyfileName)) {
                        stream.write(localKey);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                var localKey = new byte[96];
                try (var fis = new FileInputStream(keyfileName)) {
                    if (fis.read(localKey) < 96) {
                        throw new RuntimeException("Expected to read 96 bytes from key");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Map<String, Object> keyMap = new HashMap<>();
                keyMap.put("key", localKey);
                Map<String, Map<String, Object>> kmsProvCreds = new HashMap<>();
                kmsProvCreds.put("local", keyMap);
                return kmsProvCreds;
            case "aws":
                Map<String, Object> kmsProviderDetails = new HashMap<>();
                kmsProviderDetails.put("accessKeyId", env("AWS_ACCESS_KEY_ID")); // Your AWS access key ID
                kmsProviderDetails.put("secretAccessKey", env("AWS_SECRET_ACCESS_KEY")); // Your AWS secret access key

                Map<String, Map<String, Object>> kmsProviderCredentials = new HashMap<>();
                kmsProviderCredentials.put("aws", kmsProviderDetails);
                return kmsProviderCredentials;
            default:
                throw new RuntimeException("Unrecognized provider: " + provider);
        }
    }

    public static BsonDocument getMasterKeyCredentials(String kmsProvider) {
        switch (kmsProvider) {
            case "local":
                return new BsonDocument();
            case "aws":
                var doc = new BsonDocument();
                doc.put("provider", new BsonString(kmsProvider));
                doc.put("key", new BsonString(env("AWS_KEY_ARN")));
                doc.put("region", new BsonString(env("AWS_KEY_REGION")));
                return doc;
            default:
                throw new RuntimeException("Unrecognized provider: " + kmsProvider);
        }
    }

    public static AutoEncryptionSettings getAutoEncryptionSettings
            (String provider, String kvNamespace, Map<String, Map<String, Object>> kmsProviderCredentials) {
        Map<String, Object> extras = new HashMap<>();
        extras.put("cryptSharedLibPath", env("SHARED_LIB_PATH"));
        return AutoEncryptionSettings.builder()
                .keyVaultNamespace(kvNamespace)
                .kmsProviders(kmsProviderCredentials)
                .extraOptions(extras)
                .build();
    }

    public static String env(String name) {
        return dotEnv.get(name);
    }
}
