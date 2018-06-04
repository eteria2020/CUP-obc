package eu.philcar.csg.OBC.db;

import android.location.Location;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.DataManager;

@DatabaseTable(tableName = "trips", daoClass = Trips.class )
public class Trip  extends DbRecord<TripResponse> {
	public static final int OPEN_TRIP = 0;
	public static final int CLOSE_TRIP = 1;
	
		@DatabaseField(generatedId = true)
		public int id;
		
		@DatabaseField
		public int id_customer;
		
		@DatabaseField
		public String plate;

		@DatabaseField
		public int remote_id;
		
		@DatabaseField
		public Date begin_time;
		
		@DatabaseField
		public long begin_timestamp;

		
		@DatabaseField
		public int begin_km;
		
		@DatabaseField
		public int begin_battery;
		
		@DatabaseField
		public double begin_lon;
		
		@DatabaseField
		public double begin_lat;
		
		@DatabaseField
		public Date end_time;
		
		@DatabaseField
		public long end_timestamp;		
		
		@DatabaseField
		public int end_km;
		
		@DatabaseField
		public int end_battery;
		
		@DatabaseField
		public double end_lon;
		
		@DatabaseField
		public double end_lat;
		
		@DatabaseField
		public Date recharge_time;
		
		@DatabaseField
		public int  recharge; //initially unused now used to store server result to deny trip if rebooted
	
		@DatabaseField
		public boolean begin_sent;
		
		@DatabaseField
		public boolean end_sent;

		@DatabaseField
		public boolean offline;
		
		@DatabaseField		
		public String  warning;
		
		@DatabaseField 
		public int int_cleanliness;
		
		@DatabaseField 
		public int ext_cleanliness;

		@DatabaseField 
		public int park_seconds;

		@DatabaseField 
		public int n_pin;
		
		@DatabaseField 
		public int id_parent;

		/*@DatabaseField
		public int done_cleanliness;*/


//		@DatabaseField
//		public boolean sospeso;


	public Trip() {
	}

	public Trip(int id_customer, String plate, Date begin_time, long begin_timestamp, int begin_battery, int begin_km) {
		this.id_customer = id_customer;
		this.plate = plate;
		this.begin_time = begin_time;
		this.begin_timestamp = begin_timestamp;
		this.begin_battery = begin_battery;
		this.begin_km = begin_km;
		this.begin_sent = false;
	}

	public void setBeginLocation(Location location) {
		if (location != null) {
			this.begin_lat = location.getLatitude();
			this.begin_lon = location.getLongitude();
		}
	}

	public String toString() {
			return String.format("Trip { Id:%d , RId:%d \n Tms begin:%d , Tms end:%d \n TX 1 : %b , TX 2  : %b , n_pin : %d , id_parent : %d }", id,remote_id,begin_timestamp, end_timestamp, begin_sent, end_sent,n_pin, id_parent);
		}
		
		public long getMinutiDurata() {
			
			return (System.currentTimeMillis() - begin_time.getTime())/60000;
		}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Trip){
			if(id != ((Trip) o).id) {
				return false;
			}
			if(remote_id != ((Trip) o).remote_id) {
				return false;
			}
			if(id_customer != ((Trip) o).id_customer) {
				return false;
			}
			if(begin_timestamp != ((Trip) o).begin_timestamp) {
				return false;
			}

			return true;
		}
		return false;
	}

	@Override
	public void handleResponse(TripResponse tripResponse, DataManager dataManager, int callOrder) {
		switch (callOrder){
			case Trip.OPEN_TRIP:

				recharge = tripResponse.getResult();
				if (tripResponse.getResult() > 0) {

					remote_id = tripResponse.getResult();
					begin_sent = true;
					dataManager.updateBeginSentDone(this);

				} else {
					if(end_timestamp == 0) {//if trip offline not update value
						switch (tripResponse.getResult()) {

							case -15:
								warning = "OPEN TRIP";
								begin_sent = true;
								break;

							case -16:
								warning = "FORBIDDEN";
								begin_sent = true;
								break;

							case -26:
							case -27:
							case -28:
							case -29:
								warning = "PREAUTH";
								begin_sent = true;
								break;


							default:
								warning = "FAIL";
								begin_sent = false;
						}
					}
					if (tripResponse.getExtra() != null && !tripResponse.getExtra().isEmpty()) {
						try {
							remote_id = Integer.parseInt(tripResponse.getExtra());
						} catch (Exception e) {
							DLog.E("Exception while extracting extra", e);
						}
					}
					dataManager.updateBeginSentDone(this);
				}
				break;
			case CLOSE_TRIP:
				recharge = tripResponse.getResult();
				if (tripResponse.getResult() > 0) {
					end_sent = true;
				} else {
					switch (tripResponse.getResult()) {

						case -3:
							warning="NO_MATCH";
							begin_sent = false;
							end_sent = false;
							dataManager.updateBeginSentDone(this);
							break;

						default:
							warning="FAIL";
							end_sent = false;
					}
				}

				dataManager.updateTripEndSentDone(this);
				break;
		}
	}
}
