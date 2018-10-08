package eu.philcar.csg.OBC.data.datasources.api;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Created by Fulvio on 27/06/2018.
 */

public class LoggerInterceptor implements Interceptor {
    HttpLogger logger = new HttpLogger();

    @SuppressLint("DefaultLocale")
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long t1 = System.nanoTime();
        try {
            Buffer requestBuffer = new Buffer();
            String params = "";
            String url = request.url().toString();

            if (request.method().equalsIgnoreCase("POST") && request.body() != null) {
                request.body().writeTo(requestBuffer);
                params = "body";
            } else if (request.method().equalsIgnoreCase("GET")) {
                String query = request.url().query();
                requestBuffer.writeString(query == null ? "" : query, Charset.defaultCharset());
                params = "params";
                if (url.contains("?"))
                    url = url.substring(0, request.url().toString().lastIndexOf('?'));
            }

            logger.log(String.format("--> %s request %s %s: %s", request.method(), url, params, requestBuffer.readUtf8()));
        } catch (Exception e) {
            logger.e("Exception writing log " + request.url(), e);
        }

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        try {
            logger.log(String.format("<-- %s response %s for %s in %.1fms Body ",
                    response.request().method(), response.code(), response.request().url(), (t2 - t1) / 1e6d));

        } catch (Exception e) {
        logger.e("Exception writing log " + request.url(), e);
    }
        return response;
    }
}
