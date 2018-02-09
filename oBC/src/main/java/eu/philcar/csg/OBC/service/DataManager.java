package eu.philcar.csg.OBC.service;

import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
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
                .concatMap(new Function<List<Customer>, ObservableSource<? extends Customer>>() {
                    @Override
                    public ObservableSource<? extends Customer> apply(@NonNull List<Customer> customer)
                            throws Exception {
                        return table.setCustomers(customer);
                    }
                });
    }

}
