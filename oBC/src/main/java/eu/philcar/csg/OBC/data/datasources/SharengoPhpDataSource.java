package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.CommandResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
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


    Observable<TripResponse> updateTrip(Trip trip);

    Observable<TripResponse> closeTrip(Trip trip, DataManager dataManager);

}
