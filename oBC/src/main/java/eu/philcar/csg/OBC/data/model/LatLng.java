package eu.philcar.csg.OBC.data.model;


/**
 * Created by Fulvio on 27/03/2018.
 */

public class LatLng {
    public double latitude;
    public double longitude;

    public LatLng(double lat, double lon) {
        if(-180.0D <= lon && lon < 180.0D) {
            this.longitude = lon;
        } else {
            this.longitude = ((lon - 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D;
        }

        this.latitude = Math.max(-90.0D, Math.min(90.0D, lat));
    }


    public int hashCode() {
        long var3 = Double.doubleToLongBits(this.latitude);
        int var2 = 31 + (int)(var3 ^ var3 >>> 32);
        var3 = Double.doubleToLongBits(this.longitude);
        var2 = 31 * var2 + (int)(var3 ^ var3 >>> 32);
        return var2;
    }

    public boolean equals(Object var1) {
        if(this == var1) {
            return true;
        } else if(!(var1 instanceof LatLng)) {
            return false;
        } else {
            LatLng var2 = (LatLng)var1;
            return Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(var2.latitude) && Double.doubleToLongBits(this.longitude) == Double.doubleToLongBits(var2.longitude);
        }
    }

    public String toString() {
        double var1 = this.latitude;
        double var3 = this.longitude;
        return (new StringBuilder(60)).append("lat/lng: (").append(var1).append(",").append(var3).append(")").toString();
    }
}

