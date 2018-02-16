package eu.philcar.csg.OBC.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.api.SharengoService;
import eu.philcar.csg.OBC.data.model.ConfigResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import io.reactivex.Observable;

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

        return  mDbManager.getClientiDao().createOrUpdateMany(customer);

    }

    public Observable<BusinessEmployee> saveEmployee(List<BusinessEmployee> customer) {

        return  mDbManager.getDipendentiDao().createOrUpdateMany(customer);

    }

    public void saveConfig(ConfigResponse customer) {

        App.Instance.setConfig(customer.getJson(),null);

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
