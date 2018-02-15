package eu.philcar.csg.OBC.data.model;

import com.google.gson.Gson;

/**
 * Created by Fulvio on 15/02/2018.
 */

public class Pin {
    private String primary;
    private String secondary;
    private String company;

    public Pin(String primary) {
        this.primary = primary;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getSecondary() {
        return secondary;
    }

    public void setSecondary(String secondary) {
        this.secondary = secondary;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJson() {
        Gson gson = new Gson();
        return  gson.toJson(this);
    }
}
