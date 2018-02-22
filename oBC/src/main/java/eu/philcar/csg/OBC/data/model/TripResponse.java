package eu.philcar.csg.OBC.data.model;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;

/**
 * Created by Fulvio on 19/02/2018.
 */

public class TripResponse extends BaseResponse {
    private int result;
    private String message;
    private String extra;

    public TripResponse(int result, String message, String extra) {
        this.result = result;
        this.message = message;
        this.extra = extra;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
