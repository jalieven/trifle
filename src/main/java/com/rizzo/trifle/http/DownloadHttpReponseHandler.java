package com.rizzo.trifle.http;

import com.ning.http.client.Response;

import java.util.Map;

public interface DownloadHttpReponseHandler {

    Integer onCompleted(String url, Response response, Map<String, String> attributes, String baseUrl, String extension);

    void onThrowable(Throwable t);

}
