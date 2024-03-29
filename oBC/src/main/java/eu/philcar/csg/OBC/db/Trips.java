package eu.philcar.csg.OBC.db;

import android.content.Context;
import android.location.Location;
import android.os.Handler;

import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoApiRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoPhpRepository;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class Trips extends DbTable<Trip, Integer> {

	private DLog dlog = new DLog(this.getClass());

	public Trips(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		// TODO Auto-generated constructor stub
	}

	public static Class GetRecordClass() {
		return Trip.class;
	}

	public Trip getLastOpenTrip() {
		try {
			PreparedQuery<Trip> query = queryBuilder().orderBy("id", false).limit(1L).prepare();

			List<Trip> list = this.query(query);

			if (list != null && list.size() > 0 && list.get(0).end_timestamp == 0)
				return list.get(0);
			else
				return null;

		} catch (SQLException e) {
			dlog.e("Trips().getLastOpenTrip();fail:", e);
			return null;

		}
	}

	public List<Trip> getOpenTrips() {

		try {
			PreparedQuery<Trip> query = queryBuilder().orderBy("begin_timestamp", true).where().eq("end_timestamp", 0).prepare();

			return this.query(query);

		} catch (SQLException e) {
			dlog.e("Trips().getOpenTrips();", e);
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
			dlog.e("Trips.ResetFailed();", e);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Trip> getTripsToSend() {

		try {
			Where<Trip, Integer> where = queryBuilder().orderBy("begin_timestamp", true).where();
			where.and(
					where.or(
							where.eq("begin_sent", false),
							where.and(
									where.eq("end_sent", false),
									where.gt("end_timestamp", 0)
							)
					),
					where.or(
							where.isNull("warning"),
							where.eq("warning", "OPEN_TRIP")
					)
			);

			PreparedQuery<Trip> query = where.prepare();

			dlog.d("Trips.getTripsToSend();" + query.toString());

			return this.query(query);

		} catch (Exception e) {
			dlog.e("Trips.getTripsToSend();", e);
			return null;

		}
	}

	@SuppressWarnings("unchecked")
	public long getNTripsToSend() {

		try {
			Where<Trip, Integer> where = queryBuilder().where();

			where.or(
					where.eq("begin_sent", false),
					where.and(
							where.eq("end_sent", false),
							where.gt("end_timestamp", 0)
					)
			);

//			long count = where.countOf();
			return where.countOf();

		} catch (Exception e) {
			dlog.e("Trips.getNTripsToSend();", e);
			return 0;
		}
	}

	private Disposable offlineDisposable = null;


	public boolean sendOffline(Context context, Handler handler, SharengoApiRepository apiRepository, SharengoPhpRepository phpRepository) {
		//HttpConnector http;
		if (RxUtil.isRunning(offlineDisposable)) {
			dlog.d("Trips.sendOffline();is running");
			return false;
		}

		List<Trip> list = getTripsToSend();

		if (list == null) {

			dlog.d("Trips.sendOffline();null");
			return false;
		}

		dlog.d("Trips.sendOffline();to send:" + list.size());
		if (list.size() == 0)
			return false;

		if (!App.hasNetworkConnection()) {
			dlog.w("Trips.sendOffline();No connection: aborted");
			return false;
		}

		Observable.just(1)
				.concatMap(i -> {
					if (App.fullNode)
						return apiRepository.closeTrips(list);
					else
						return phpRepository.closeTrips(list);
				})
//				.subscribeOn(Schedulers.newThread())
				.subscribe(new Observer<Trip>() {
					@Override
					public void onSubscribe(Disposable d) {
						offlineDisposable = d;
					}

					@Override
					public void onNext(Trip trip) {
						dlog.d("Trips.sendOffline();Sent offline trip, response is " + trip.toString());

					}

					@Override
					public void onError(Throwable e) {
						dlog.e("Trips.sendOffline();Error while communicating offline trip, ", e);
						offlineDisposable.dispose();
					}

					@Override
					public void onComplete() {
						dlog.d("Trips.sendOffline();Completed sendOfflineTrip successfully");
						offlineDisposable.dispose();
					}
				});


/*for(Trip c : list) {

			dlog.d("Selected trip to send:" + c.toString());
			
			c.offline=true;
			TripInfo tripInfo =  new TripInfo(context);
			tripInfo.trip = c;
			TripsConnector cc = new TripsConnector(tripInfo);
			http = new HttpConnector(context);
			http.SetHandler(handler);
			http.Execute(cc);
			return true;
		}*/


		return false;
	}


	public Trip Begin(String targa, Customer cliente, Location location, int carburante, int km) {
		Trip corsaAperta = new Trip();

		//TODO: gestire eventuale corsa ancora aperta.

//		corsaAperta = new Trip();

		corsaAperta.plate = targa;

		corsaAperta.id_customer = cliente.id;
		corsaAperta.begin_time = new Date();
		corsaAperta.begin_timestamp = DbManager.getTimestamp();
		corsaAperta.begin_battery = carburante;
		corsaAperta.begin_km = km;
		corsaAperta.begin_sent = false;

		if (location != null) {
			corsaAperta.begin_lat = location.getLatitude();
			corsaAperta.begin_lon = location.getLongitude();
		}

/*		int id;

		try {
			id = this.create(corsaAperta);
		} catch (SQLException e) {
			dlog.e("Begin corsa", e);
		}*/
		dlog.d("Trips.Begin();Begin: " + corsaAperta.toString());
		return corsaAperta;
	}

/*
	public boolean End(Trip corsaAperta, Location location, int carburante, int km) {

		//TODO: gestire eventuale corsa NON ancora aperta.

		if (corsaAperta == null)
			return false;

		corsaAperta.end_time = new Date();
		corsaAperta.end_battery = carburante;
		corsaAperta.end_km = km;
		if (location != null) {
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
*/

/*
	public int getRemoteIDfromLocal(int id) {
		try {
			Where<Trip, Integer> where = queryBuilder().where();

			where.eq("id", id);

			Trip trip = where.queryForFirst();
			return trip != null ? trip.remote_id : 0;

		} catch (SQLException e) {
			dlog.e("getNTripsToSend fail:", e);
			return 0;
		}
	}
*/

	public List<Trip> getTripfromTime(long timestamp) {

		try {
			Where<Trip, Integer> where = queryBuilder().orderBy("remote_id", false).limit(1L).where();

			where.and(where.le("begin_timestamp", timestamp),
					where.ge("end_timestamp", timestamp));
			//where.eq("sending_error", false)
			//where.ge("timestamp",((System.currentTimeMillis()/1000)-60*60*24*7))

			PreparedQuery<Trip> query = where.prepare();
			dlog.d("Trips.getTripfromTime();query:" + query.toString());

			return this.query(query);

		} catch (SQLException e) {
			dlog.e("Trips.getTripfromTime();", e);
			return null;

		}
	}

	public Observable<Trip> findTripParentfromTrip(Trip trip) {

		try {
			Where<Trip, Integer> where = queryBuilder().orderBy("remote_id", false).limit(1L).where();

			where.and(where.gt("id_parent", 0),
					where.le("end_timestamp", trip.begin_timestamp),
					where.eq("id_customer", trip.id_customer),
					where.ge("end_timestamp", trip.begin_timestamp - 1000 * 60 * 4));
			//where.eq("sending_error", false)
			//where.ge("timestamp",((System.currentTimeMillis()/1000)-60*60*24*7))

			PreparedQuery<Trip> query = where.prepare();
			dlog.d("Trips.findTripParentfromTrip();query:" + query.toString());
			List<Trip> result = this.query(query);
			if (result.size() > 0) {
				return Observable.just(this.query(query))
						.concatMap(Observable::fromIterable);
			} else return Observable.just(new Trip());

		} catch (SQLException e) {
			dlog.e("Trips.findTripParentfromTrip();", e);
			return null;

		}
	}

}
