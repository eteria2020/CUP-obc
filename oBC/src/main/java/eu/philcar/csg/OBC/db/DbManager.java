package eu.philcar.csg.OBC.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.injection.ApplicationContext;

@Singleton
public class DbManager extends OrmLiteSqliteOpenHelper {

	private static final String DB_NAME = "sharengo.db";

	private static final int DATABASE_VERSION = 5;
	private static DbManager instance = null;

	private final Class<?> tables[] = {Customers.GetRecordClass(), BusinessEmployees.GetRecordClass(), Trips.GetRecordClass(), Events.GetRecordClass(), Pois.GetRecordClass(), DataLoggers.GetRecordClass()};

	public static String getDbName() {
		return App.getAppDataPath().concat(DB_NAME);
	}

	public static DbManager getInstance(Context context) {
		if (instance == null)
			instance = new DbManager(context);

		return instance;
	}

	public static long getTimestamp() {
		return System.currentTimeMillis() / 1000;
	}

	@Inject
	public DbManager(@ApplicationContext Context context) {
		//super(context, new File( Environment.getExternalStorageDirectory(),DB_NAME).toString(), null, DATABASE_VERSION);
		super(context, getDbName(), null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {

		for (Class<?> t : tables) {
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
				updateFromVersion1(database, connectionSource, oldVersion, newVersion);//Change poi Name value

			case 2:
				updateFromVersion1(database, connectionSource, oldVersion, newVersion);// poi variable lenght
			case 3:
				updateFromVersion3(database, connectionSource, oldVersion, newVersion);//added business customer
			case 4:
				updateFromVersion4(database, connectionSource, oldVersion, newVersion);//ADDED DataLogger Table

			default:
				// no updates needed
				break;
		}
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		try {
			ConnectionSource connectionSource = getConnectionSource();
			TableUtils.dropTable(connectionSource, Pois.GetRecordClass(), true);
			TableUtils.createTable(connectionSource, Pois.GetRecordClass());
		} catch (Exception e) {
			DLog.E(this.getClass().toString() + "onDowngrade Exception", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateFromVersion1(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {

			TableUtils.dropTable(connectionSource, Pois.GetRecordClass(), true);
			TableUtils.createTable(connectionSource, Pois.GetRecordClass());

			DLog.D("Upgrade to Db versione " + (oldVersion + 1) + " sucessfull");

		} catch (Exception e) {
			DLog.E("Upgrade to Db versione " + (oldVersion + 1) + " failed", e);
		}
	}

	private void updateFromVersion3(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {

			TableUtils.createTable(connectionSource, BusinessEmployees.GetRecordClass());

			DLog.D("Upgrade to DB version " + (oldVersion + 1) + " successful");

		} catch (Exception e) {
			DLog.E("Upgrade to DB version " + (oldVersion + 1) + " failed", e);
		}
	}

	private void updateFromVersion4(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {

			TableUtils.createTable(connectionSource, DataLoggers.GetRecordClass());

			DLog.D("Upgrade to DB version " + (oldVersion + 1) + " successful");

		} catch (Exception e) {
			DLog.E("Upgrade to DB version " + (oldVersion + 1) + " failed", e);
		}
	}


	/*@SuppressWarnings("unchecked")
	private void updateFromVersion3(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {

			db.beginTransaction();
			db.execSQL("ALTER TABLE trips ADD COLUMN done_cleanliness INTEGER DEFAULT 0");
			db.endTransaction();


			DLog.D("Upgrade to Db versione 4 sucessfull");

		}catch (Exception e) {
			DLog.E("Upgrade to Db versione 4 failed",e);
		}
		onUpgrade(db, connectionSource, oldVersion + 1, newVersion);
	}*/

	/*private void updateFromVersion2(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {

			db.execSQL("ALTER TABLE eventi ADD COLUMN id_trip_local int DEFAULT 0");
			DLog.D("Upgrade to Db versione 2 sucessfull");

		}catch (Exception e) {
			DLog.E("Upgrade to Db versione 2 failed",e);
		}
		onUpgrade(db, connectionSource, oldVersion + 1, newVersion);
	}
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

	public Customers getClientiDao() {
		try {
//			Customers c = getDao(Customer.class);

			//c.setAutoCommit(true);
			return getDao(Customer.class);
		} catch (Exception e) {

			DLog.E("Error getting dao clienti", e);
			return null;
		}

	}

	public BusinessEmployees getDipendentiDao() {
		try {
//			BusinessEmployees b = getDao(BusinessEmployee.class);
			//b.setAutoCommit(true);
			return getDao(BusinessEmployee.class);
		} catch (Exception e) {

			DLog.E("Error getting dao dipendenti", e);
			return null;
		}
	}

	public Trips getTripDao() {
		try {
//			Trips c = getDao(Trip.class);
			//c.setAutoCommit(true);
			return getDao(Trip.class);
		} catch (Exception e) {

			DLog.E("Error getting dao corse", e);
			return null;
		}

	}

	public Events getEventiDao() {
		try {
//			Events c = getDao(Event.class);
			//c.setAutoCommit(true);
			return getDao(Event.class);
		} catch (Exception e) {

			DLog.E("Error getting dao eventi", e);
			return null;
		}

	}

	public Pois getPoisDao() {
		try {
//			Pois c = getDao(Poi.class);
			//c.setAutoCommit(true);
			return getDao(Poi.class);
		} catch (Exception e) {

			DLog.E("Error getting dao pois", e);
			return null;
		}

	}

/*	public DataLoggers getDataLoggersDao() {
		try {
//			DataLoggers c = getDao(DataLogger.class);
			//c.setAutoCommit(true);
			return getDao(DataLogger.class);
		} catch (Exception e) {

			DLog.E("Error getting dao DataLoggers", e);
			return null;
		}

	}*/

}