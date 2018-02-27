package eu.philcar.csg.OBC.db;

import java.sql.SQLException;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.DataManager;

@DatabaseTable(tableName = "eventi", daoClass = Events.class )  
public class Event extends DbRecord<EventResponse> {
	

	@DatabaseField(generatedId = true)
	public int id;
	
	@DatabaseField
	public long timestamp;
	
	@DatabaseField	
	public int event;

	@DatabaseField	
	public String label;

	@DatabaseField
	public int intval;
	
	@DatabaseField
	public String txtval;
	
	@DatabaseField
	public int id_customer;
	
	@DatabaseField
	public int id_trip;
	
	@DatabaseField
	public int level;
	
	@DatabaseField
	public double lon;
	
	@DatabaseField
	public double lat;

	@DatabaseField
	public int  km;
	
	@DatabaseField
	public int  battery;

	@DatabaseField
	public boolean sent;
	
	@DatabaseField
	public boolean sending_error;


	public String json_data;
	
	public void Update() {
		Events eventi = App.Instance.dbManager.getEventiDao();
		try {
			eventi.createOrUpdate(this);
		} catch (SQLException e) {
			DLog.E("Error updating event",e);

	    }
	}


	@Override
	public void handleResponse(EventResponse eventResponse, DataManager manager, int callOrder) {
		sent = true;
		if (eventResponse.getResult() > 0) {
			sending_error=false;
		} else {
			sending_error=true;
		}
		manager.updateEventSendingResponse(this);
	}
}
