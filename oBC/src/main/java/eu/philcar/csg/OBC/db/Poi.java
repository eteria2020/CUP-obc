package eu.philcar.csg.OBC.db;

import android.location.Location;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;
import eu.philcar.csg.OBC.data.model.ServerResponse;
import eu.philcar.csg.OBC.service.DataManager;

@DatabaseTable(tableName = "poi", daoClass = Pois.class )  
public class Poi extends DbRecord<ServerResponse> implements CustomOp{
	
	@DatabaseField(id = true)
	public int id;
	
	@DatabaseField(index = true)
	public String type;
	
	@DatabaseField(index = true)
	public String type_group;
	
	@DatabaseField	
	public String code;
	
	@DatabaseField	
	public String name;	
	
	@DatabaseField	
	public String brand;		
	
	@DatabaseField
	public String address;
	
	@DatabaseField
	public String town;
	
	@DatabaseField
	public String zip;
	
	@DatabaseField
	public String province;
	
	@DatabaseField
	public Boolean attivo;

	@DatabaseField
	public Double lon;

	@DatabaseField
	public Double lat;

	@DatabaseField(index = true)
	public long update;

	public Location getLoc(){
		Location loc1 = new Location("");
		loc1.setLatitude(lat);
		loc1.setLongitude(lon);
		return loc1;
	}


	@Override
	public void handleResponse(ServerResponse e, DataManager manager, int callOrder) {

	}

	@Override
	public void onDbWrite() {
		brand="";
		type_group="";
		attivo=true;
	}
}
