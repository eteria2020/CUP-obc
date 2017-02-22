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
	public String tipo;
	
	@DatabaseField	
	public String codice;
	
	@DatabaseField
	public String via;

	@DatabaseField
	public String nome;
	
	@DatabaseField
	public String citta;
	
	@DatabaseField
	public String cap;
	
	@DatabaseField
	public String provincia;
	
	@DatabaseField
	public Boolean attivo;

	@DatabaseField
	public Double lon;

	@DatabaseField
	public Double lat;

	@DatabaseField(index = true)
	public long aggiornamento;

	public Location getLoc(){
		Location loc1 = new Location("");
		loc1.setLatitude(lat);
		loc1.setLongitude(lon);
		return loc1;
	}
	

	

	
}
