package eu.philcar.csg.OBC.db;


import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.os.Handler;

import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.TripsConnector;
import eu.philcar.csg.OBC.server.HttpConnector;
import eu.philcar.csg.OBC.service.TripInfo;

public class Trips extends DbTable<Trip,Integer> {

	private DLog dlog = new DLog(this.getClass());
	
	public  Trips(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		// TODO Auto-generated constructor stub
	}
	
	
	public  static Class GetRecordClass() {
		return Trip.class;
	}
	
	

	
	
	public Trip getLastOpenTrip() {
		try {
			PreparedQuery<Trip> query =  queryBuilder().orderBy("id", false).prepare();		

			List<Trip> list = this.query(query);
			
			if (list!=null && list.size()>0 && list.get(0).end_timestamp==0)
				return list.get(0);
			else
				return null;
										
		} catch (SQLException e) {
			dlog.e("getLastOpenTrip fail:",e);
			return null;

		}
	}
	
	
	public  List<Trip> getOpenTrips() {
		
		try {
			PreparedQuery<Trip> query =  queryBuilder().orderBy("begin_timestamp", true).where().eq("end_timestamp",0).prepare();		

			return this.query(query);
										
		} catch (SQLException e) {
			dlog.e("getOpenTrips fail:",e);
			return null;

		}
	}
	
	
	public void ResetFailed() {
		try {
			UpdateBuilder<Trip, Integer> updateBuilder = updateBuilder();
			
			updateBuilder.updateColumnValue("warning", null);
			updateBuilder.update();
			updateBuilder.reset();
		} catch (SQLException e) {
			dlog.e("ResetFailed error:",e);			
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public List<Trip> getTripsToSend() {
		
		try {
			Where<Trip,Integer> where  = queryBuilder().orderBy("begin_timestamp", true).where();
			where.and(
				where.or(
					where.eq("begin_sent",false),
					where.and(
						where.eq("end_sent",false),
						where.gt("end_timestamp", 0)
					)
				),
				where.or(
					where.isNull("warning"),
					where.eq("warning", "OPEN_TRIP")
				)
			);
			
			
			PreparedQuery<Trip> query =  where.prepare();
			
			dlog.d("Query : " + query.toString());
			
			return this.query(query);
										
		} catch (Exception e) {
			dlog.e("getTripsToSend fail:",e);
			return null;

		}
	}
	
	@SuppressWarnings("unchecked")
	public long getNTripsToSend() {
		
		try {
			Where<Trip,Integer> where  = queryBuilder().where();
			
			where.or(
				where.eq("begin_sent",false),
				where.and(
					where.eq("end_sent",false),
					where.gt("end_timestamp", 0)
				)
			);
			
			
			
			long count =  where.countOf();					
			return count;
										
		} catch (Exception e) {
			dlog.e("getNTripsToSend fail:",e);
			return 0;
		}
	}
	
	
	
	public boolean sendOffline(Context context, Handler handler) {
		HttpConnector http;
		List<Trip> list = getTripsToSend();

		
		if (list==null) {

			dlog.d("Trips to send : null");
			return false;
		}
		
		dlog.d("Trips to send : " + list.size());
		
		if (!App.hasNetworkConnection()) {
			dlog.w("No connection: aborted");
			return false;
		}
		
		//Dato che l'invio � asincrono viene richiesto l'invio solo della prima corsa non spedito, quando arriva il messaggio di risposto di invio eseguito(o fallito) passa alla successiva 
		for(Trip c : list) {
			
			dlog.d("Selected trip to send:" + c.toString());
			
			c.offline=true;
			TripInfo tripInfo =  new TripInfo(context);
			tripInfo.trip = c;
			TripsConnector cc = new TripsConnector(tripInfo);
			http = new HttpConnector(context);
			http.SetHandler(handler);
			http.Execute(cc);
			return true;
		}
		
		return false;
	}
	
	
	public Trip Begin(String targa, Customer cliente, Location location, int carburante, int km) {
		Trip corsaAperta = null;
		
		//TODO: gestire eventuale corsa ancora aperta.
		
		corsaAperta  = new Trip();
		
		corsaAperta.plate = targa;
		
		corsaAperta.id_customer = cliente.id;
		corsaAperta.begin_time = new Date();
		corsaAperta.begin_timestamp = DbManager.getTimestamp();
		corsaAperta.begin_battery = carburante;
		corsaAperta.begin_km = km;
		corsaAperta.begin_sent = false;
		
		if (location!=null) {
			corsaAperta.begin_lat = location.getLatitude();
			corsaAperta.begin_lon = location.getLongitude();
		}
		
			
		int id;
		
		try {
			id = this.create(corsaAperta);
		} catch (SQLException e) {
			dlog.e("Begin corsa",e);			
		}
		dlog.d("Begin: "+corsaAperta.toString());
		return corsaAperta;
	}
	
	
	public boolean End(Trip corsaAperta,Location location, int carburante, int km) {
		
		//TODO: gestire eventuale corsa NON ancora aperta.
		
		if (corsaAperta == null)
			return false;

		corsaAperta.end_time = new Date();
		corsaAperta.end_battery = carburante;
		corsaAperta.end_km = km;
		if (location!=null) {
			corsaAperta.end_lat = location.getLatitude();
			corsaAperta.end_lon = location.getLongitude();
		}
		

		
		try {
			this.update(corsaAperta);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	public int getRemoteIDfromLocal(int id){
		try {
			Where<Trip,Integer> where  = queryBuilder().where();


					where.eq("id",id);



			Trip trip =  where.queryForFirst();
			return trip!=null?trip.remote_id:0;

		} catch (SQLException e) {
			dlog.e("getNTripsToSend fail:",e);
			return 0;
		}
	}
	
	
}
