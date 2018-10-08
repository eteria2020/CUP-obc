package eu.philcar.csg.OBC.data.model;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;

/**
 * Created by Fulvio on 15/02/2018.
 */

public class Pin extends BaseResponse {
    private String primary;
    private String secondary;
    private String company;

    public Pin(String primary, String secondary, String company) {
        this.primary = primary;
        this.secondary = secondary;
        this.company = company;
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

}
