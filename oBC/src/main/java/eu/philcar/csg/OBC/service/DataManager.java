package eu.philcar.csg.OBC.service;

import android.graphics.Bitmap;

import com.google.gson.Gson;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.model.AdsImage;
import eu.philcar.csg.OBC.data.model.AdsResponse;
import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.ServerCommand;
import io.reactivex.Observable;

@Singleton
public class DataManager { //TODO change to interface-type system like api does |||| use an Injection module style

    private final DbManager mDbManager; //TODO use interface DbHelper

    private AdsResponse imageCar;
    private AdsResponse imageStart;
    private AdsResponse imageEnd;

    @Inject
    public DataManager(DbManager DbManager) {
        mDbManager = DbManager;
    }



    public Observable<Customer> saveCustomer(List<Customer> customer) {

        return  mDbManager.getClientiDao().createOrUpdateMany(customer);

    }

    public Observable<BusinessEmployee> saveEmployee(List<BusinessEmployee> customer) {

        return  mDbManager.getDipendentiDao().createOrUpdateMany(customer);

    }

    public Observable<Trip> saveTrip(Trip trip) {

        return  mDbManager.getCorseDao().createOrUpdateOne(trip);

    }

    public Observable<Trip> saveTrips(Collection<Trip> trip) {

        return  mDbManager.getCorseDao().createOrUpdateMany(trip);

    }

    public Observable<Event> saveEvent(Event event) {

        return  mDbManager.getEventiDao().createOrUpdateOne(event);

    }

    public Observable<Event> saveEvents(Collection<Event> event) {

        return  mDbManager.getEventiDao().createOrUpdateMany(event);

    }

    public void saveConfig(Config customer) {

        App.Instance.setConfig(customer.getJson(),null);

    }

    public void saveModel(ModelResponse model) {

        DLog.D("Model car is" + model.toString());

    }

    public Observable<List<Area>> saveArea(List<Area> area){

        return persistArea(area);
    }

    public Observable<Poi> savePoi(List<Poi> pois) {

        return  mDbManager.getPoisDao().createOrUpdateMany(pois);

    }

    public void updateBeginSentDone(Trip trip){
        UpdateBuilder<Trip,Integer> builder = mDbManager.getCorseDao().updateBuilder();
        try {
            builder.updateColumnValue("warning", trip.warning);
            builder.updateColumnValue("recharge", trip.recharge);
            builder.updateColumnValue("begin_sent", trip.begin_sent);
            builder.updateColumnValue("remote_id", trip.remote_id);
            builder.where().idEq(trip.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {
            DLog.E("setTxApertura : ",e);
        }
    }

    public void updateTripSetOffline(Trip trip) {

        if (trip==null)
            return;


        UpdateBuilder<Trip,Integer> builder =  mDbManager.getCorseDao().updateBuilder();
        try {
            builder.updateColumnValue("recharge", trip.recharge);
            builder.updateColumnValue("offline", trip.offline);
            builder.where().idEq(trip.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {
            DLog.E("setTxOffline : ",e);
        }
    }

    public void updateEventSendingResponse(Event event) {

        if (event==null)
            return;


        UpdateBuilder<Event,Integer> builder =  mDbManager.getEventiDao().updateBuilder();
        try {
            builder.updateColumnValue("sending_error", event.sending_error);
            builder.updateColumnValue("sent", event.sent);
            builder.where().idEq(event.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {
            DLog.E("setTxOffline : ",e);
        }
    }

    public void updateTripEndSentDone(Trip trip) {

        if (trip==null)
            return;


        UpdateBuilder<Trip,Integer> builder =  mDbManager.getCorseDao().updateBuilder();
        try {
            builder.updateColumnValue("end_sent", true);
            builder.where().idEq(trip.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {
            DLog.E("setTxChiusura : ",e);
        }
    }


    public Observable<ServerCommand> handleCommands(List<ServerCommand> commands){

        return Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;
            try {
                for (ServerCommand row : commands) {
                    emitter.onNext(row);
                }
                emitter.onComplete();
            } catch(Exception e) {
                DLog.E("Exception handling commands",e);
                emitter.onError(e);
            }
        });
    }

    /**
     * emit the firs active reservation and then sent the completion signal
     * @param reservations
     * @return
     */
    public Observable<Reservation> handleReservations(List<Reservation> reservations){

        return Observable.create(emitter -> {
            if (emitter.isDisposed())
                return;
            try {
                Reservation first=null;
                for (Reservation reservation : reservations) {
                    if(first==null) {
                        if(reservation.active)
                            first=reservation;
                    }
                    if(reservation.isMaintenance() && reservation.active){
                        first=reservation;
                        break;
                    }
                }
                emitter.onNext(first);
                emitter.onComplete();
            } catch(Exception e) {
                DLog.E("Exception handling reservation",e);
                emitter.onError(e);
            }
        });
    }


    private Observable<List<Area>> persistArea(List<Area> area){
        File file = new File(App.getAppDataPath(),"area.json");

        String result;

            Gson gson = new Gson();
            result = gson.toJson(area);

        try {
            OutputStream os = new FileOutputStream(file);
            os.write(result.getBytes());
            os.close();
        } catch (IOException e) {
            DLog.E("File output error area.json",e);
        }
        return Observable.just(area);
    }


    public long getMaxCustomerLastupdate(){
        return mDbManager.getClientiDao().mostRecentTimestamp();
    }

    public long getMaxPoiLastupdate(){
        return mDbManager.getPoisDao().mostRecent();
    }

//        new Function<ResponseCustomer>, ObservableSource<? extends ResponseCustomer>>() {
//                    @Override
//                    public ObservableSource<? extends Customer> apply(@NonNull List<Customer> customer)
//                            throws Exception {
//                        return table.setCustomers(customer);
//                    }
//                });


    public void saveBannerStart(final AdsResponse adsStart){
        if(adsStart!=null){
            imageStart=adsStart;
            App.setBannerStart(adsStart.getLastImage());
        }
    }
    public void saveBannerCar(final AdsResponse adsCar){
        if(adsCar!=null){
            imageCar=adsCar;
            App.setBannerCar(adsCar.getLastImage());
        }
    }
    public void saveBannerEnd(final AdsResponse adsEnd){
        if(adsEnd!=null){
            imageEnd=adsEnd;
            App.setBannerEnd(adsEnd.getLastImage());
        }
    }

    public AdsResponse getImageCar() {
        return imageCar;
    }

    public AdsResponse getImageStart() {
        return imageStart;
    }

    public AdsResponse getImageEnd() {
        return imageEnd;
    }

    public Observable<Bitmap> getBitmapCar() {
        return Observable.just(1)
                .concatMap(i->Observable.just(new File(App.getBannerImagesFolder(), imageCar.getLastFilename())))
                .concatMap(file1 -> Observable.just(Picasso.get().load(file1).get()));

    }

    public Observable<Bitmap> getBitmapStart() {
        return Observable.just(1)
                .concatMap(i->Observable.just(new File(App.getBannerImagesFolder(), imageStart.getLastFilename())))
                .concatMap(file1 -> Observable.just(Picasso.get().load(file1).get()));


    }

    public Observable<Bitmap> getBitmapEnd() {
        return Observable.just(1)
                .concatMap(i->Observable.just(new File(App.getBannerImagesFolder(), imageEnd.getLastFilename())))
                .concatMap(file1 -> Observable.just(Picasso.get().load(file1).get()));

    }

}
