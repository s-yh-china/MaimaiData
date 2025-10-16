package com.paperpig.maimaidata.network.server;

import android.util.Log;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class HttpRedirectServer extends NanoHTTPD {
    public static int Port = 9457;
    private final static String TAG = "HttpRedirectServer";

    protected HttpRedirectServer() throws IOException {
        super(Port);
    }

    @Override
    public void start() throws IOException {
        super.start();
        Log.d(TAG, "Http server running on http://localhost:" + Port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        return newFixedLengthResponse(
            Response.Status.ACCEPTED,
            MIME_HTML,
            "<html><body><h1>登录信息已获取,可关闭该窗口并请切回MaimaiData!</h1></body></html><script>alert('登录信息已获取,请切回MaimaiData!');</script>"
        );
    }
}
