package com.delta.standalone.lib.Service;

import org.apache.hadoop.conf.Configuration;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ConfigurationService {

   KeyVaultClientProvider _kvProvider;

   @Autowired
   public ConfigurationService(KeyVaultClientProvider keyVaultClientProvider) {
      _kvProvider = keyVaultClientProvider;
   }

     public Configuration setStorageConfiguration(String storageAccountName, String storageConnectionSecret) throws IOException {


        JSONObject json = new JSONObject(_kvProvider.getStorageConnectionSecret(storageConnectionSecret));
        Configuration conf = new Configuration();
        conf.set("fs.azure.account.auth.type."+storageAccountName+".dfs.core.windows.net", "OAuth");
        conf.set("fs.azure.account.oauth.provider.type."+storageAccountName+".dfs.core.windows.net", "org.apache.hadoop.fs.azurebfs.oauth2.ClientCredsTokenProvider");
        conf.set("fs.azure.account.oauth2.client.id."+storageAccountName+".dfs.core.windows.net", json.getString("clientId"));
        conf.set("fs.azure.account.oauth2.client.secret."+storageAccountName+".dfs.core.windows.net", json.getString("clientSecret"));
        conf.set("fs.azure.account.oauth2.client.endpoint."+storageAccountName+".dfs.core.windows.net", "https://login.microsoftonline.com/"+json.getString("tenantId")+"/oauth2/token");
        return conf;
    }
}
