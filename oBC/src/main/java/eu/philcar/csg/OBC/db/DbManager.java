package eu.philcar.csg.OBC.db;
import android.content.Context;
import java.io.File;
import java.sql.SQLException;

import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import ch.qos.logback.core.net.server.Client;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import eu.philcar.csg.OBC.helpers.DLog;
public class DbManager extends OrmLiteSqliteOpenHelper {

	public  static final String DATABASE_NAME = "/sdcard/csg/sharengo.db";
	private static final int DATABASE_VERSION = 2;

	private static DbManager instance = null;;

	private final Class<?> tables[] = { Customers.GetRecordClass() , Trips.GetRecordClass(), Events.GetRecordClass(), Pois.GetRecordClass() };


	public static  DbManager getInstance(Context context) {
		if (instance == null)
			instance = new DbManager(context);

		return instance;
	}

	public static long getTimestamp() {
		return System.currentTimeMillis()/1000;
	}

	public DbManager(Context context) {
		//super(context, new File( Environment.getExternalStorageDirectory(),DATABASE_NAME).toString(), null, DATABASE_VERSION);
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}


	@Override
	public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {

		for(Class<?> t : tables) {
			try {
				TableUtils.createTable(connectionSource, t);
			} catch (SQLException e) {
				DLog.E("Unable to create table", e);
			}
		}
	}



	@Override
	public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		switch (oldVersion) {
			case 1:
				updateFromVersion1(database, connectionSource, oldVersion, newVersion);
				break;

			/*case 2:
				updateFromVersion2(database, connectionSource, oldVersion, newVersion);
				break;*/

			default:
				// no updates needed
				break;
		}
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}

	@SuppressWarnings("unchecked")
	private void updateFromVersion1(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {

			TableUtils.dropTable(connectionSource, Pois.GetRecordClass(),true);
			TableUtils.createTable(connectionSource, Pois.GetRecordClass());
			DLog.D("Upgrade to Db versione 2 sucessfull");

		}catch (Exception e) {
			DLog.E("Upgrade to Db versione 2 failed",e);
		}
		onUpgrade(db, connectionSource, oldVersion + 1, newVersion);
	}

	/*private void updateFromVersion2(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {

			db.execSQL("ALTER TABLE eventi ADD COLUMN id_trip_local int DEFAULT 0");
			DLog.D("Upgrade to Db versione 2 sucessfull");

		}catch (Exception e) {
			DLog.E("Upgrade to Db versione 2 failed",e);
		}
		onUpgrade(db, connectionSource, oldVersion + 1, newVersion);
	}*/
	/*
	private void updateFromVersion2(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
//			db.beginTransaction();
			db.execSQL("ALTER TABLE `corse` ADD COLUMN n_pin INTEGER;");
//			db.endTransaction();
//			db.close();
			DLog.D("Upgrade to Db versione 22 sucessfull");
		} catch (Exception e) {
			DLog.E("Upgrade to Db versione 22 failed",e);
		}
	    onUpgrade(db, connectionSource, oldVersion + 1, newVersion);
	}
	*/



	@Override
	public void close() {
		super.close();
	}


	public  Customers getClientiDao() {
		try {
			Customers c = getDao(Customer.class);
			c.setAutoCommit(true);
			return  c;
		} catch (Exception e) {

			DLog.E("Error getting dao clienti", e);
			return null;
		}

	}


	public  Trips getCorseDao() {
		try {
			Trips c = getDao(Trip.class);
			c.setAutoCommit(true);
			return c;
		} catch (Exception e) {

			DLog.E("Error getting dao corse", e);
			return null;
		}

	}

	public  Events getEventiDao() {
		try {
			Events c = getDao(Event.class);
			return c;
		} catch (Exception e) {

			DLog.E("Error getting dao eventi", e);
			return null;
		}

	}

	public  Pois getPoisDao() {
		try {
			Pois c = getDao(Poi.class);
			return c;
		} catch (Exception e) {

			DLog.E("Error getting dao pois", e);
			return null;
		}

	}


}