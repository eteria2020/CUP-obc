package eu.philcar.csg.OBC.data.datasources.api;

import eu.philcar.csg.OBC.helpers.DLog;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Fulvio on 14/05/2018.
 */

public class HttpLogger implements HttpLoggingInterceptor.Logger {
    private DLog dlog = new DLog(HttpLogger.class);
    @Override
    public void log(String message) {
        dlog.d(message);
    }

    public void  e(String message, Throwable e){
        dlog.e(message, e);
    }
}
