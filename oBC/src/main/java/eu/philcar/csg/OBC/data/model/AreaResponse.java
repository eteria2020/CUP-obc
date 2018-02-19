package eu.philcar.csg.OBC.data.model;

import java.util.List;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;

/**
 * Created by Fulvio on 16/02/2018.
 */

public class AreaResponse extends BaseResponse {
    private String close_trip;
    private String costo_apertura;
    private String costo_chiusura;
    private List<String> coordinates;

    public AreaResponse(String close_trip, String costo_apertura, String costo_chiusura, List<String> coordinates) {
        this.close_trip = close_trip;
        this.costo_apertura = costo_apertura;
        this.costo_chiusura = costo_chiusura;
        this.coordinates = coordinates;
    }

    public String getClose_trip() {
        return close_trip;
    }

    public void setClose_trip(String close_trip) {
        this.close_trip = close_trip;
    }

    public String getCosto_apertura() {
        return costo_apertura;
    }

    public void setCosto_apertura(String costo_apertura) {
        this.costo_apertura = costo_apertura;
    }

    public String getCosto_chiusura() {
        return costo_chiusura;
    }

    public void setCosto_chiusura(String costo_chiusura) {
        this.costo_chiusura = costo_chiusura;
    }

    public List<String> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<String> coordinates) {
        this.coordinates = coordinates;
    }
}
