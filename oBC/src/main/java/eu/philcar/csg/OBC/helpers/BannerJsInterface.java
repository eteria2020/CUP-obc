package eu.philcar.csg.OBC.helpers;

import android.webkit.JavascriptInterface;

import eu.philcar.csg.OBC.App;

public class BannerJsInterface {

    @JavascriptInterface
    public double getLongitude() {
        if (App.getLastLocation() != null)
            return App.getLastLocation().getLongitude();
        else
            return 0;
    }

    @JavascriptInterface
    public double getLatitude() {
        if (App.getLastLocation() != null)
            return App.getLastLocation().getLongitude();
        else
            return 0;
    }
}
