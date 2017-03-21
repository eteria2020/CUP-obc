package eu.philcar.csg.OBC.db;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Encryption;

@DatabaseTable(tableName = "customers", daoClass = Customers.class )  
public class Customer extends DbRecord {
	

	@DatabaseField(id = true)
	public int id;
	
	@DatabaseField
	public String name;
	
	@DatabaseField	
	public String surname;
	
	@DatabaseField
	public String language;
	
	@DatabaseField
	public String mobile;
	
	@DatabaseField
	public boolean enabled;
	
	@DatabaseField
	public String info_display;
	
	@DatabaseField
	public String pin;
	
	@DatabaseField(index = true)
	public String card_code;
	
	@DatabaseField(index = true)
	public long update_timestamp;
	
	public void encrypt() {
		name = Encryption.encrypt(name);
		surname = Encryption.encrypt(surname);
		mobile = Encryption.encrypt(mobile);		
	}
	
	public void decrypt() {
		name = Encryption.decrypt(name);
		surname = Encryption.decrypt(surname);
		mobile = Encryption.decrypt(mobile);			
	}
	
	
	private String subMd5(byte hash[]) {
		if (hash.length>=4) {
			StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<4; i++) {
	            hexString.append(Integer.toHexString((0xF0 & hash[i])>>4));
	            hexString.append(Integer.toHexString((0x0F & hash[i])));
	        }
	        return hexString.toString();
		}
		return "*";
	}
	
	public int checkPin(String input) {
				
		input = input.trim();
		DLog.D("Cliente checkPin : " + input);
		
		String hashStr="";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] bytes = input.getBytes("UTF-8");
			byte[] hash = md.digest(bytes);
			hashStr = subMd5(hash);
			if (hashStr.length()!=8) {
				DLog.E("Cliente checkPin : hash too short :" + hashStr.length());
				return 0;
			}
		} catch (NoSuchAlgorithmException e) {
			DLog.E("Cliente checkPin",e);
			return 0;
		} catch (UnsupportedEncodingException e) {
			DLog.E("Cliente checkPin",e);
			return 0;
		}	
		
		DLog.D("Customer hashed pin : " + hashStr);
		
		if (pin==null)
			return 0;
		
		String pin1="",pin2="";
		
		try {
			
			JSONObject jobj = new JSONObject(pin);			
			DLog.D("Valid JSON : " + pin);
			Iterator keys = jobj.keys();
			int i =1;
			while (keys.hasNext()) {
			    String t = (String) keys.next();
			    if (jobj.has(t) && jobj.getString(t).equals(hashStr)) {
			    	DLog.D("Customer checkPin " + i + " : hash match " + hashStr + "=="+jobj.getString(t));
			    	if (t.equalsIgnoreCase("primary"))
			    		return 1;
			    	else if (t.equalsIgnoreCase("secondary"))
			    		return 2;
		    		else			    		
		    			return i; 
			    }
			    i++;
			}
			
		} catch( Exception e) {   // If JSON is invalid try to use simpler JSON	
			DLog.D("Invalid JSON, falling to simple JSON");			
		}		
		
		
		try {
			
			JSONArray jarray = new JSONArray(pin);
			
			DLog.D("Valid JSON : " + pin);
			
			if (jarray.length()>0)
				pin1= jarray.getString(0);
			
			if (jarray.length()>1)
				pin2= jarray.getString(1);
			
			DLog.D("Pin1 : " + pin1 + ",Pin2 :" + pin2);
			
		} catch( Exception e) {   // Se il json non è valido uso  direttamente il valore di pin
			DLog.D("Invalid JSON, falling to raw value : " + pin);
			pin1=pin;
		}
		
				
		if (hashStr.contentEquals(pin1)) {
			DLog.D("Customer checkPin 1 : hash match " + hashStr + "=="+pin1);
			return 1;
		} else if (hashStr.contentEquals(pin2)) {
			DLog.D("Customer checkPin 2: hash match " + hashStr + "=="+pin2);
			return 2;
		} else {
			DLog.D("Customer checkPin : hash mismatch " + hashStr + "!="+pin );
			return 0;
		}
		
			

		
		
	}


	public Boolean checkAdmin(){
		return info_display.compareTo("sharengo")==0;
	}
}
