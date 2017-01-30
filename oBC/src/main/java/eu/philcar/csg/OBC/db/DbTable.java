package eu.philcar.csg.OBC.db;

import java.sql.SQLException;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

public class DbTable<tableClass, pk>  extends BaseDaoImpl<tableClass, pk>{

	public DbTable(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		
	}

	
	public  static Class GetRecordClass() {
		return DbTable.class;
	}
	
	
}
