package eu.philcar.csg.OBC.db;


import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Handler;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.ConnectionSource;

import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.CustomersConnector;
import eu.philcar.csg.OBC.server.HttpConnector;
import eu.philcar.csg.OBC.server.PoisConnector;

public class Pois extends DbTable<Poi,Integer> {

	 
	public  Pois(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		// TODO Auto-generated constructor stub
	}
	
	
	public  static Class GetRecordClass() {
		return Poi.class;
	}

	
	public void deleteAll() {
		try {
			this.executeRawNoArgs("DELETE  FROM poi");
					
		} catch (SQLException e) {
			DLog.E("deleteAll failed:",e);

		}
	}
	
	public boolean isPresent(int id, long aggiornamento) {
		
		try {
			
			PreparedQuery<Poi> query =  queryBuilder().setCountOf(true).where().eq("id",id).and().eq("aggiornamento", aggiornamento).prepare();	
			long c = this.countOf(query);
			
			return (c>0)?true:false;
					
		} catch (SQLException e) {
			DLog.E("isPresent fail:",e);

		}
		
		return false;
	}
	
	
	public List<Poi> getPois(String tipo) {
		
		try {
			
			PreparedQuery<Poi> query =  queryBuilder().where().eq("tipo",tipo).and().eq("attivo", true).prepare();
			
			return this.query(query);
		
					
		} catch (SQLException e) {
			DLog.E("getPois fail:",e);

		}
		
		return null;
	}
	public List<Poi> getPoisAbilitedToCustomer() {

		try {

			PreparedQuery<Poi> query =  queryBuilder().where().eq("abilitato",true).and().eq("attivo", true).prepare();

			return this.query(query);


		} catch (SQLException e) {
			DLog.E("getPois fail:",e);

		}

		return null;
	}
	
	
	public long mostRecent() {
		long max =0;
		try {
			 max = this.queryRawValue("SELECT max(aggiornamento) FROM poi"); 
					
		} catch (SQLException e) {
			DLog.E("mostRecent fail:",e);

		}
		
		return max;
	}
	
	
	public long getSize() {
		long count =0;
		try {
			count = this.queryRawValue("SELECT count(*) FROM poi"); 
					
		} catch (SQLException e) {
			DLog.E("getSize fail:",e);

		}
		
		return count;
	}
	

	
	public void startDownload(Context ctx, Handler handler) {
		DLog.D("Start pois download..");
		PoisConnector cn = new PoisConnector();
		cn.setLastUpdate(mostRecent());
		HttpConnector http = new HttpConnector(ctx);
		http.SetHandler(handler);
		http.Execute(cn);
	}
	
}
