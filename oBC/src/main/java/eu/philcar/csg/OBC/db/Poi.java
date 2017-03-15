package eu.philcar.csg.OBC.db;

import android.location.Location;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Encryption;

@DatabaseTable(tableName = "poi", daoClass = Pois.class )  
public class Poi extends DbRecord {
	
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
	

	

	
}
