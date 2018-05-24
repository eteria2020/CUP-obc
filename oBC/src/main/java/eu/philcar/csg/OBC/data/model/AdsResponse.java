package eu.philcar.csg.OBC.data.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.util.List;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;
import eu.philcar.csg.OBC.helpers.DLog;

/**
 * Created by Fulvio on 17/05/2018.
 */

public class AdsResponse extends BaseResponse {

    @SerializedName("Image")
    private List<AdsImage> images;

    public AdsResponse(List<AdsImage> images) {
        this.images = images;
    }

    @Nullable
    public String getLastIndex(){
        try{
            return images.get(images.size()-1).index;
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getLastEnd(){
        try{
            return images.get(images.size()-1).end;
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public AdsImage getLastImage(){
        try{
            return images.get(images.size()-1);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getLastFilename(){
        try {
            URL urlImg = new URL(images.get(images.size() - 1).imageUrl);
            String extension = urlImg.getFile().substring(urlImg.getFile().lastIndexOf('.') + 1);
            return String.valueOf(images.get(images.size() - 1).id).concat(".").concat(extension);
        }catch (Exception e){
                return null;
        }
    }

    public List<AdsImage> getImages() {
        return images;
    }
}
