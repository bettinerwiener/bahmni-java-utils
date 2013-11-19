package org.bahmni.webclients;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpClientInternal {
    private int connectTimeout;
    private int readTimeout;
    private String sessionIdKey;
    private String sessionIdValue;
    private DefaultHttpClient defaultHttpClient;


    public HttpClientInternal(int connectTimeout, int readTimeout, String sessionIdKey, String sessionIdValue) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.sessionIdKey = sessionIdKey;
        this.sessionIdValue = sessionIdValue;
    }

    public HttpClientInternal(int connectionTimeout, int readTimeout) {
        this.connectTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public String get(URI uri) {
        return get(uri, null);
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public String get(URI uri, Map<String, String> headers) {
        HttpURLConnection connection = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");

            if (StringUtils.isNotEmpty(sessionIdKey))
                connection.setRequestProperty("Cookie", String.format("%s=%s", sessionIdKey, sessionIdValue));

            if (headers != null) {
                for (String key : headers.keySet()) {
                    connection.setRequestProperty(key, headers.get(key));
                }
            }
            connection.setDoOutput(true);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.getProperty("line.separator"));
            }
        } catch (Exception e) {
            throw new WebClientsException(e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return stringBuilder.toString();
    }

    public Map<String, String> getCookies() {
        HashMap<String, String> map = new HashMap<>();
        map.put(sessionIdKey, sessionIdValue);
        return map;
    }

    HttpResponse get(HttpRequestDetails requestDetails) {
        defaultHttpClient = new DefaultHttpClient();
        DefaultHttpClient httpClient = defaultHttpClient;

        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, readTimeout);
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectTimeout);

        HttpGet httpGet = new HttpGet(requestDetails.getUri());
        requestDetails.addDetailsTo(httpGet);

        try {
            return httpClient.execute(httpGet);
        } catch (IOException e) {
            throw new WebClientsException("Error executing request");
        }
    }

    void closeConnection(){
        if(defaultHttpClient != null)
            defaultHttpClient.getConnectionManager().shutdown() ;
    }

}