package eu.philcar.csg.OBC.service;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.api.SharengoService;
import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.CommandResponse;
import eu.philcar.csg.OBC.data.model.ConfigResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.ServerCommand;
import io.reactivex.Observable;

@Singleton
public class DataManager {

    private final DbManager mDbManager; //TODO use interface DbHelper


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

    public void saveConfig(ConfigResponse customer) {

        App.Instance.setConfig(customer.getJson(),null);

    }

    public void saveArea(List<AreaResponse> area){

        persistArea(area);
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
                    DLog.E("Exception updating Customer",e);
                    emitter.onError(e);
                }
        });
    }

    private void persistArea(List<AreaResponse> area){
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
    }


    public long getMaxLastupdate(){
        return mDbManager.getClientiDao().mostRecentTimestamp();
    }

//        new Function<ResponseCustomer>, ObservableSource<? extends ResponseCustomer>>() {
//                    @Override
//                    public ObservableSource<? extends Customer> apply(@NonNull List<Customer> customer)
//                            throws Exception {
//                        return table.setCustomers(customer);
//                    }
//                });




}
