package com.delta.standalone.lib.Service;

import com.delta.standalone.lib.AppUtils;
import com.delta.standalone.lib.Exception.ConfigurationLoaderException;
import com.delta.standalone.lib.Exception.KeyVaultProviderException;
import com.delta.standalone.lib.KeyVaultClientCredential;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Properties;

@Component
public class KeyVaultClientProvider {

    public String getStorageConnectionSecret(String storageConnectionSecret) throws IOException {
        Properties envProperties;
        try {
            envProperties = AppUtils.loadProperties();
        }
        catch(IOException e) {
            throw new IOException("Unable to load application properties to fetch KeyVault connection");
        }
            KeyVaultClientCredential clientCredential = new KeyVaultClientCredential(
                    envProperties.getProperty("azure.key-vault.clientId"),
                    envProperties.getProperty("azure.key-vault.clientSecret"));
            KeyVaultClient client = new KeyVaultClient(clientCredential);
            SecretBundle secretBundle = client.getSecret(envProperties.getProperty("azure.key-vault.endpoint"), storageConnectionSecret);
            return secretBundle.value();

    }
}
