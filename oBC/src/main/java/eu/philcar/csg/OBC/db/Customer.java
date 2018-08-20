package eu.philcar.csg.OBC.db;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;
import eu.philcar.csg.OBC.data.model.Pin;
import eu.philcar.csg.OBC.data.model.ServerResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Encryption;
import eu.philcar.csg.OBC.data.common.ExcludeSerialization;
import eu.philcar.csg.OBC.service.DataManager;

@DatabaseTable(tableName = "customers", daoClass = Customers.class )
public class Customer extends DbRecord<Customer> implements CustomOp, ServerResponse{

	@ExcludeSerialization
	public static final int N_ERROR_PIN = 0;
	@ExcludeSerialization
	public static final int N_PRIMARY_PIN = 1;
	@ExcludeSerialization
	public static final int N_SECONDARY_PIN = 2;
	@ExcludeSerialization
	public static final int N_COMPANY_PIN = 100;

	@SerializedName("i")
	@DatabaseField(id = true)
	public int 		id;

	@SerializedName("n")
	@DatabaseField
	public String 	name;

	@SerializedName("c")
	@DatabaseField
	public String 	surname;

	@ExcludeSerialization
	@DatabaseField
	public String 	language;

	@SerializedName("t")
	@DatabaseField
	public String	mobile;

	@SerializedName("a")
	@DatabaseField
	public boolean 	enabled;

	@SerializedName("id")
	@DatabaseField
	public String 	info_display;

	@ExcludeSerialization
	@DatabaseField
	public String	 pin;

	@SerializedName("cc")
	@DatabaseField(index = true)
	public String 	card_code;

	@SerializedName("tm")
	@DatabaseField(index = true)
	public long 	update_timestamp;

	@SerializedName("ps")
	public Pin pins=null;

	@ExcludeSerialization
	private boolean isEnctypted = true;

	public Customer(boolean isNew) {
		if(isNew)
			isEnctypted=false;
	}

	@Deprecated
	public Customer() {
	}

	public void encrypt() {
        if(!isEnctypted) {
            name = Encryption.encrypt(name);
            surname = Encryption.encrypt(surname);
            mobile = Encryption.encrypt(mobile);
            isEnctypted = true;
        }
	}

	public void decrypt() {
        if(isEnctypted) {
            name = Encryption.decrypt(name);
            surname = Encryption.decrypt(surname);
            mobile = Encryption.decrypt(mobile);
            isEnctypted = false;
        }
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
				return N_ERROR_PIN;
			}
		} catch (NoSuchAlgorithmException e) {
			DLog.E("Cliente checkPin",e);
			return N_ERROR_PIN;
		} catch (UnsupportedEncodingException e) {
			DLog.E("Cliente checkPin",e);
			return N_ERROR_PIN;
		}

		DLog.D("Customer hashed pin : " + hashStr);

		if (pin==null)
			return N_ERROR_PIN;

		String pin1="",pin2="";

		try {
			JSONObject jobj = new JSONObject(pin);
			DLog.D("Valid JSON : " + pin);

			if (jobj.has("primary") && jobj.getString("primary").equals(hashStr)) {
				return N_PRIMARY_PIN;
			}

			if (jobj.has("secondary") && jobj.getString("secondary").equals(hashStr)) {
				return N_SECONDARY_PIN;
			}

			if (jobj.has("company") && jobj.getString("company").equals(hashStr)) {
				return N_COMPANY_PIN;
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
			return N_PRIMARY_PIN;
		} else if (hashStr.contentEquals(pin2)) {
			DLog.D("Customer checkPin 2: hash match " + hashStr + "=="+pin2);
			return N_SECONDARY_PIN;
		} else {
			DLog.D("Customer checkPin : hash mismatch " + hashStr + "!="+pin );
			return N_ERROR_PIN;
		}
	}


	public Boolean checkAdmin(){
		return info_display.compareTo("sharengo")==0;
	}

	@Override
	public String toString() {
		return "Customer{" +
				"id=" + id +
				", name='" + name + '\'' +
				", surname='" + surname + '\'' +
				", language='" + language + '\'' +
				", mobile='" + mobile + '\'' +
				", enabled=" + enabled +
				", info_display='" + info_display + '\'' +
				", pin='" + pin + '\'' +
				", card_code='" + card_code + '\'' +
				", update_timestamp=" + update_timestamp +
				", pins=" + pins +
				", isEnctypted=" + isEnctypted +
				'}';
	}

	public boolean isCompanyPinEnabled() {
		try {
			JSONObject pinJson = new JSONObject(pin);
			return !pinJson.has("companyPinDisabled") || !pinJson.getBoolean("companyPinDisabled");

		} catch (JSONException e) {
			DLog.D("Invalid JSON parsing");
		}
		return true;
	}

	public boolean update(){
		try {
			DLog.D("Customer update: start updating Customer from DB");
			DbManager dbm = App.Instance.dbManager;
			Customers daoCustomers = dbm.getClientiDao();
			Customer customer = daoCustomers.queryForId(id);
			customer.decrypt();
			updateFromCustomer(customer);

			return true;

		}catch(Exception e){
			DLog.E("Customer update: Exception while updating Customer",e);
			return false;
		}
	}

	private void updateFromCustomer(Customer customer){

		id = customer.id;
		name = customer.name;
		surname = customer.surname;
		language = customer.language;
		mobile = customer.mobile;
		enabled = customer.enabled;
		info_display = customer.info_display;
		pin = customer.pin;
		card_code = customer.card_code;
		update_timestamp = customer.update_timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Customer){
			if(id != ((Customer) o).id) {
				return false;
			}
			if(update_timestamp != ((Customer) o).update_timestamp) {
				return false;
			}

			return true;
		}
		return false;
	}

	@Override
	public void onDbWrite() {
		isEnctypted=false;
		encrypt();
	}

	@Override
	public void handleResponse(Customer customer, DataManager manager, int callOrder) {

	}
}
