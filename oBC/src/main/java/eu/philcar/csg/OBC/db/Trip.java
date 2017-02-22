package eu.philcar.csg.OBC.db;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "trips", daoClass = Trips.class )
public class Trip  extends DbRecord {
	
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
		public int  recharge; //initially unused now used to store server result
	
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


//		@DatabaseField
//		public boolean sospeso;
		
		public String toString() {
			return String.format("Trip { Id:%d , RId:%d \n Tms begin:%d , Tms end:%d \n TX 1 : %b , TX 2  : %b , n_pin : %d , id_parent : %d }", id,remote_id,begin_timestamp, end_timestamp, begin_sent, end_sent,n_pin, id_parent);
		}
		
		public long getMinutiDurata() {
			
			return (System.currentTimeMillis() - begin_time.getTime())/60000;
		}
}
