package eu.philcar.csg.OBC.interfaces;


import org.json.JSONObject;

/**
 * Created by Fulvio on 06/10/2017.
 */

public abstract class BasicJson {

    abstract public boolean isValid();

    abstract public void  fromJson(String json);

    abstract public String toJson();

    abstract public JSONObject toJsonObject();

    abstract public boolean equals(Object obj);

}
