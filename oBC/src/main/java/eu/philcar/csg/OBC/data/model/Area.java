package eu.philcar.csg.OBC.data.model;

import android.location.Location;
import android.support.annotation.NonNull;

import com.skobbler.ngx.SKCoordinate;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.controller.map.util.GeoUtils;
import eu.philcar.csg.OBC.data.common.ExcludeSerialization;
import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 16/02/2018.
 */

public class Area extends BaseResponse implements Comparable<Area> {
    private String close_trip;
    private int costo_apertura;
    private int costo_chiusura;
    private List<Double> coordinates;
    @ExcludeSerialization
    private List<SKCoordinate> points;
    @ExcludeSerialization
    private List<SKCoordinate> envelope;
    @ExcludeSerialization
    private SKCoordinate min = null;
    @ExcludeSerialization
    private SKCoordinate max = null;
    @ExcludeSerialization
    private SKCoordinate maxLat = null;
    @ExcludeSerialization
    private int area = 0;

    public Area() {
    }

    public Area(String close_trip, int costo_apertura, int costo_chiusura, List<Double> coordinates) {
        this.close_trip = close_trip;
        this.costo_apertura = costo_apertura;
        this.costo_chiusura = costo_chiusura;
        this.coordinates = coordinates;
        this.points = new ArrayList<>();
        this.envelope = new ArrayList<>();
    }

    private void init() {
        this.points = new ArrayList<>();
        this.envelope = new ArrayList<>();
    }

    public Observable<Area> initPoints() {
        init();
        try {
            int x = coordinates.size();
            for (int j = 0; j < x; j += 3) {
                SKCoordinate point = new SKCoordinate(0, 0);
                try {
                    point = new SKCoordinate(coordinates.get(j), coordinates.get(j + 1));

                    if (min == null)
                        min = new SKCoordinate(point.getLongitude(), point.getLatitude());
                    if (max == null) {
                        max = new SKCoordinate(point.getLongitude(), point.getLatitude());
                        maxLat = point;
                    }

                    if (point.getLatitude() < min.getLatitude()) {
                        min.setLatitude(point.getLatitude());
                    } else if (point.getLatitude() > max.getLatitude()) {
                        max.setLatitude(point.getLatitude());
                        maxLat = point;
                    }

                    if (point.getLongitude() < min.getLongitude()) {
                        min.setLongitude(point.getLongitude());
                    } else if (point.getLongitude() > max.getLongitude())
                        max.setLongitude(point.getLongitude());

                } catch (NumberFormatException e) {
                    DLog.E("Exception parsing coordinate while init Points", e);
                }
                points.add(point);
            }
        } catch (Exception e) {

        }

        return Observable.just(this);
    }

    public List<SKCoordinate> getEnvelope() {
        return envelope;
    }

    public Observable<Area> initEnvelop() {
        Double offset = 0.15;
        //Set the start of the Polyline as the max lat
        int indexMaxLat = points.indexOf(maxLat);
        for (int k = indexMaxLat; k < points.size() - 1; k++) {
            envelope.add(points.get(k));
        }
        for (int k = 0; k < indexMaxLat; k++) {
            envelope.add(points.get(k));
        }
        envelope.add(new SKCoordinate(points.get(indexMaxLat).getLongitude(), points.get(indexMaxLat).getLatitude() + Double.MIN_VALUE));
        envelope.add(new SKCoordinate(points.get(indexMaxLat).getLongitude(), points.get(indexMaxLat).getLatitude() + offset));
        envelope.add(new SKCoordinate(max.getLongitude() + offset * 1.35, max.getLatitude() + offset));//1
        envelope.add(new SKCoordinate(max.getLongitude() + offset * 1.35, min.getLatitude() - offset));//2
        envelope.add(new SKCoordinate(min.getLongitude() - offset * 1.35, min.getLatitude() - offset));//3
        envelope.add(new SKCoordinate(min.getLongitude() - offset * 1.35, max.getLatitude() + offset));//4
        envelope.add(new SKCoordinate(points.get(indexMaxLat).getLongitude(), points.get(indexMaxLat).getLatitude() + offset + Double.MIN_VALUE));//5
        envelope.add(points.get(indexMaxLat));

        return Observable.just(this);
    }

    public Boolean insideItaly() {
        return !(max.getLongitude() >= 18.53 ||
                min.getLongitude() <= 6.63 ||
                ((max.getLatitude()>44 && max.getLongitude() >13.7484) || max.getLatitude() >= 47.10 )|| //check for upper right area corner
                min.getLatitude() <= 36.64);
    }

    /**
     * compare 2 Area to order by the size of the inner area
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(@NonNull Area o) {
        return (int) (o.getArea() - getArea());
    }

    public long getArea() {
        long distance = 0;
        Location l1 = new Location(""), l2 = new Location("");
        for (int i = 0; i < points.size()-1; i++) {
            SKCoordinate c1 = points.get(i), c2 = points.get(i + 1);
            l1.setLatitude(c1.getLatitude());
            l1.setLongitude(c1.getLongitude());
            l2.setLatitude(c2.getLatitude());
            l2.setLongitude(c2.getLongitude());
            distance += GeoUtils.harvesineDistance(l1, l2);
        }


        return distance;
    }

    public String getClose_trip() {
        return close_trip;
    }

    public void setClose_trip(String close_trip) {
        this.close_trip = close_trip;
    }

    public int getCosto_apertura() {
        return costo_apertura;
    }

    public void setCosto_apertura(int costo_apertura) {
        this.costo_apertura = costo_apertura;
    }

    public int getCosto_chiusura() {
        return costo_chiusura;
    }

    public void setCosto_chiusura(int costo_chiusura) {
        this.costo_chiusura = costo_chiusura;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Double> coordinates) {
        this.coordinates = coordinates;
    }
}
