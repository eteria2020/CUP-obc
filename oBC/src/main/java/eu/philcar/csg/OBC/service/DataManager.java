package eu.philcar.csg.OBC.service;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.data.datasources.api.SharengoService;
import eu.philcar.csg.OBC.data.model.ResponseCustomer;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;

@Singleton
public class DataManager {

    private final SharengoService mSharengoService;
    private final DbManager mDbManager;

    @Inject
    public DataManager(SharengoService SharengoService,
                       DbManager DbManager) {
        mSharengoService = SharengoService;
        mDbManager = DbManager;
    }

    public Observable<Customer> syncCustomer() {
        final Customers table = mDbManager.getClientiDao();
        return mSharengoService.getCustomer()
                .concatMap(customer -> table.setCustomers(customer));
    }

    public Observable<Customer> saveCustomer(List<Customer> customer) {

        return  mDbManager.getClientiDao().setCustomers(customer);

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
