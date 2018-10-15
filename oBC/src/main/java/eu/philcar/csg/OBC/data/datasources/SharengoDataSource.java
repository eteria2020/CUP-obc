package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
import eu.philcar.csg.OBC.service.Reservation;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 15/02/2018.
 */

public interface SharengoDataSource {

    Observable<List<Customer>> getCustomer(long lastupdate);

    Observable<List<BusinessEmployee>> getBusinessEmployees();

    Observable<Config> getConfig(String car_plate);

    Observable<List<Reservation>> getReservation(String car_plate);

    Observable<Reservation> consumeReservation(int reservation_id);

    Observable<List<Area>> getArea(String md5);

    Observable<List<ServerCommand>> getCommands(String plat);

    Observable<List<ModelResponse>> getModel(String plate);

    Observable<List<Poi>> getPois(long lastupdate);

    Observable<EventResponse> sendEvent(Event trip, DataManager dataManager);

    Observable<TripResponse> openTrip(Trip trip, DataManager dataManager);

    Observable<Trip> openTripPassive(Trip trip, DataManager dataManager);

    Observable<Trip> closeTripPassive(Trip trip, DataManager dataManager);

    Observable<TripResponse> updateTrip(Trip trip);

    Observable<TripResponse> closeTrip(Trip trip, DataManager dataManager);
}
