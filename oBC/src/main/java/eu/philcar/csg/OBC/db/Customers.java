package eu.philcar.csg.OBC.db;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Handler;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.ConnectionSource;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class Customers extends DbTable<Customer,Integer> {

	 
	public  Customers(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		// TODO Auto-generated constructor stub
	}
	
	
	public  static Class GetRecordClass() {
		return Customer.class;
	}

	
	public void deleteAll() {
		try {
			this.executeRawNoArgs("DELETE  FROM customers");
					
		} catch (SQLException e) {
			DLog.E("deleteAll failed:",e);

		}
	}
	
	public boolean isPresent(int id, double timestamp) {
		
		try {
			
			PreparedQuery<Customer> query =  queryBuilder().setCountOf(true).where().eq("id",id).and().eq("update_timestamp", timestamp).prepare();	
			long c = this.countOf(query);
			
			return c > 0;
					
		} catch (SQLException e) {
			DLog.E("isPresent fail:",e);

		}
		
		return false;
	}
	
	
	
	public long mostRecentTimestamp() {
		long max =0;
		try {
			 max = this.queryRawValue("SELECT max(update_timestamp) FROM customers"); 
					
		} catch (SQLException e) {
			DLog.E("mostRecentTimestamp fail:",e);

		}
		
		return max;
	}
	
	
	public long getSize() {
		long count =0;
		try {
			count = this.queryRawValue("SELECT count(*) FROM customers"); 
					
		} catch (SQLException e) {
			DLog.E("getSize fail:",e);

		}
		
		return count;
	}
	
	public Customer getClienteByCardCode(String cardCode) {
		
		try {
		 
			List<Customer> list = this.queryForEq("card_code", cardCode.toUpperCase());
			
			if (list==null || list.size()!=1)
				return null;
			else
				return list.get(0);
		} catch (SQLException e) {
				DLog.E("mostRecentTimestamp fail:",e);

		}	
		return null;
	}

	public List<String> getMaintainerCards() {
		
		try {
			List<Customer> list  = this.queryForEq("info_display", "sharengo");
			
			if (list==null)
				return null;
			
			List<String> codes = new ArrayList<String>();
			
			for(Customer c : list) {
				codes.add(c.card_code);
			}
			return codes;
			
			
		} catch (Exception e) {
			DLog.E("getMaintainerCards");			
		}
		
		return null;
	}


	@Deprecated
	public void startWhitelistDownload(Context ctx, Handler handler) {
		DLog.D("Start whitelist download..");
		/*CustomersConnector cn = new CustomersConnector();
		cn.setLastUpdate(mostRecentTimestamp());
		HttpsConnector http = new HttpsConnector(ctx);
		http.SetHandler(handler);
		http.Execute(cn);*/
	}

	public Observable<Customer> setCustomers(final Collection<Customer> customers){
		return Observable.create(emitter -> {
            if (emitter.isDisposed()) return;

            try {int index =0;
                for (Customer customer : customers) {
                	index++;
                    customer.encrypt();
                    int result = createOrUpdate(customer).getNumLinesChanged();
                    if (result >= 0) emitter.onNext(customer);
                }
                emitter.onComplete();
            } catch(Exception e) {
                DLog.E("Exception setting Customer",e);
                emitter.onError(e);
            }
        });
	}

	@Override
	public CreateOrUpdateStatus createOrUpdate(Customer data) throws SQLException {
		if(data.pins!=null)
			data.pin=data.pins.getJson();
		return super.createOrUpdate(data);
	}
}
