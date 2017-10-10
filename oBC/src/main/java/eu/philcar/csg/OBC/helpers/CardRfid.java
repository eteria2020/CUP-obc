package eu.philcar.csg.OBC.helpers;

import org.json.JSONException;
import org.json.JSONObject;

import eu.philcar.csg.OBC.interfaces.BasicJson;

/**
 * Created by Fulvio on 06/10/2017.
 */

public class CardRfid extends BasicJson {

    private DLog dlog = new DLog(this.getClass());

    private  String code;
    private  String name;

    public CardRfid(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public CardRfid(){
        this.code = null;
        this.name = null;
    }

    public static CardRfid decodeFromJson(String json) {
        CardRfid result=new CardRfid("","");
        try {
            JSONObject jo = new JSONObject(json);

            result = new CardRfid(jo.optString("code", null), jo.optString("name", null));

        }catch (Exception e){
            DLog.E("CardRfid decodefromJson: exception while decoding",e);
        }
        return result;
    }


    public String getCode() {
        return code!=null?code:"";
    }

    public String getName() {
        return name!=null?name:"";
    }

    @Override
    public boolean isValid(){
        return code!=null && name!=null;
    }

    @Override
    public String toJson() {

            JSONObject jo = new JSONObject();
            try {
                jo.put("code", getCode());
                jo.put("name", getName());
            } catch (JSONException e) {
                dlog.e("Exception while writing json",e);
            }

        return jo.toString();

    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("code", getCode());
            jo.put("name", getName());
        } catch (JSONException e) {
            dlog.e("Exception while writing json",e);
        }

        return jo;
    }

    @Override
    public void fromJson(String json) {
        try {
            JSONObject jo = new JSONObject(json);

            this.code = jo.optString("code", null);
            this.name = jo.optString("name", null);

        }catch (Exception e){
            DLog.E("CardRfid decodefromJson: exception while decoding",e);
            this.code=null;
            this.name=null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        CardRfid comparisonCard;
        if(obj instanceof CardRfid) {
            comparisonCard = (CardRfid) obj;
        }
        else {
            return false;
        }

        return comparisonCard.getCode().equalsIgnoreCase(getCode());

    }

    @Override
    public String toString() {
        return toJson();
    }
}
