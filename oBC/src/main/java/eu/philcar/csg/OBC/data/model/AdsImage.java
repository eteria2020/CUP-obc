package eu.philcar.csg.OBC.data.model;

import android.os.Bundle;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Fulvio on 18/05/2018.
 */

public class AdsImage {

    @SerializedName("URL")
    public String imageUrl;

    @SerializedName("ID")
    public int id;

    @SerializedName("CLICK")
    public String clickUrl;

    @SerializedName("INDEX")
    public String index;

    @SerializedName("END")
    public String end;

    public Bundle getBundle(){
        Bundle image = new Bundle();

        image.putString("ID", String.valueOf(id));
        image.putString("URL", imageUrl);
        image.putString(("CLICK"), clickUrl);
        image.putString(("INDEX"), index);
        image.putString(("END"), end);
        return image;
    }


}
