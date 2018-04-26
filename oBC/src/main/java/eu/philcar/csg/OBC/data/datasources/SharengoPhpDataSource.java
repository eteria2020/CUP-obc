package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.CommandResponse;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
import eu.philcar.csg.OBC.service.Reservation;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 16/02/2018.
 */

public interface SharengoPhpDataSource {


    Observable<List<AreaResponse>> getArea(String plate, String md5);

    Observable<List<ServerCommand>> getCommands(String plat);

    /*Observable<List<ServerCommand>> openTrip( int cmd_type,  String plate,  int customerId,  long beginTimestamp,  int km,  int soc,
                                              double beginLon,  double beginLat,  String warning,  int intCleanliness,
                                              int extCleanliness,String macAddress, String imei,  int nPin);*/

    Observable<TripResponse> openTrip(Trip trip, DataManager dataManager);

    Observable<Trip> openTripPassive(Trip trip, DataManager dataManager);

    Observable<TripResponse> updateTrip(Trip trip);

    Observable<TripResponse> closeTrip(Trip trip, DataManager dataManager);

    Observable<EventResponse> sendEvent(Event trip, DataManager dataManager);


    Observable<List<Reservation>> getReservation(String plate);


    Observable<Void> consumeReservation(int reservation_id);

    Observable<List<Poi>> getPois(long lastupdate);

}
