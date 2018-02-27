package eu.philcar.csg.OBC.db;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.joda.time.LocalTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;
import eu.philcar.csg.OBC.data.model.ServerResponse;
import eu.philcar.csg.OBC.service.DataManager;

@DatabaseTable(tableName = "business_employee", daoClass = BusinessEmployees.class )
public class BusinessEmployee extends DbRecord<BusinessEmployee> implements ServerResponse {

	@DatabaseField(id = true)
	public int id;

	@SerializedName("codice_azienda")
	@DatabaseField
	public String businessCode;

	@SerializedName("azienda_abilitata")
	@DatabaseField
	public boolean isBusinessEnabled;

	@SerializedName("limite_orari")
	@DatabaseField
	public String timeLimits;

	public boolean isBusinessEnabled() {
		return isBusinessEnabled;
	}

	public boolean isWithinTimeLimits() {
		Date date = new Date();

		String dayOfWeek = new SimpleDateFormat("EE", Locale.ENGLISH).format(date);
		dayOfWeek = dayOfWeek.substring(0, 2).toLowerCase();
		System.out.println(dayOfWeek);
		if (timeLimits.contains(dayOfWeek)) {
			String hours = timeLimits.substring(timeLimits.indexOf(dayOfWeek));
			hours = hours.substring(3, hours.indexOf(")"));
			if (hours.isEmpty()) return true;
			String[] times = hours.split(",");
			for (String time : times) {
				if (isWithinLimit(date, time)) return true;
			}
			return false;
		} else {
			return false;
		}
	}

	private static boolean isWithinLimit(Date date, String time) {
		String[] fromAndTo = time.split("-");
		LocalTime from = LocalTime.parse(fromAndTo[0]);
		LocalTime to = LocalTime.parse(fromAndTo[1]);
		LocalTime now = LocalTime.parse(new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(date));
		return now.isAfter(from.minusMinutes(1)) && now.isBefore(to);
	}

	@Override
	public void handleResponse(BusinessEmployee businessEmployee, DataManager manager, int callOrder) {

	}


}
