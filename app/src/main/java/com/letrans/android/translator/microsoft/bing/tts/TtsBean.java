package com.letrans.android.translator.microsoft.bing.tts;

public class TtsBean {
    private String apiKey;
    private String baseUri;
    private String tk_host;
    private String uri_host;
    private String baseTtsUri;

    TtsBean(String apiKey, String baseUri, String tk_host, String uri_host, String baseTtsUri) {
        this.apiKey = apiKey;
        this.baseUri = baseUri;
        this.tk_host = tk_host;
        this.uri_host = uri_host;
        this.baseTtsUri = baseTtsUri;
    }

    public String getHost() {
        return tk_host;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public String getUri_host() {
        return uri_host;
    }

    public String getBaseTtsUri() {
        return baseTtsUri;
    }

    public void setHost(String host) {
        this.tk_host = host;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public void setUri_host(String uri_host) {
        this.uri_host = uri_host;
    }

    public void setBaseTtsUri(String baseTtsUri) {
        this.baseTtsUri = baseTtsUri;
    }
}
