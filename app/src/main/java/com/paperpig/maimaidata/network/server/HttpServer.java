package com.paperpig.maimaidata.network.server;

import android.util.Log;
import com.paperpig.maimaidata.crawler.CrawlerCaller;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class HttpServer extends NanoHTTPD {
    public static int Port = 8284;
    private final static String TAG = "HttpServer";

    protected HttpServer() throws IOException {
        super(Port);
    }

    @Override
    public void start() throws IOException {
        super.start();
        Log.d(TAG, "Http server running on http://localhost:" + Port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "Serve request: " + session.getUri());
        if (session.getUri().equals("/auth")) {
            return redirectToWechatAuthUrl(session);
        } else {
            return redirectToAuthUrlWithRandomParm(session);
        }
    }

    // To avoid fu***ing cache of wechat webview client
    private Response redirectToAuthUrlWithRandomParm(IHTTPSession ignore) {
        Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
        r.addHeader("Location", "http://127.0.0.1:8284/auth?random=" + System.currentTimeMillis());
        return r;
    }

    private Response redirectToWechatAuthUrl(IHTTPSession ignore) {
        String url = CrawlerCaller.INSTANCE.getWechatAuthUrl();
        if (url == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_HTML, "");
        }
        Log.d(TAG, url);

        Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
        r.addHeader("Location", url);
        r.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        r.addHeader("Pragma", "no-cache");
        r.addHeader("Expires", "0");
        return r;
    }
}
