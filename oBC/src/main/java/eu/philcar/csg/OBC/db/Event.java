package eu.philcar.csg.OBC.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.DataManager;

@DatabaseTable(tableName = "eventi", daoClass = Events.class)
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
    public int level; //Can be used to store the local trip id

    @DatabaseField
    public double lon;

    @DatabaseField
    public double lat;

    @DatabaseField
    public int km;

    @DatabaseField
    public int battery;

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
            DLog.E("Error updating event", e);

        }
    }

    @Override
    public void handleResponse(EventResponse eventResponse, DataManager manager, int callOrder) {
        sent = true;
        sending_error = eventResponse.getResult() <= 0;
        manager.updateEventSendingResponse(this);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", event=" + event +
                ", label='" + label + '\'' +
                ", intval=" + intval +
                ", txtval='" + txtval + '\'' +
                ", id_customer=" + id_customer +
                ", id_trip=" + id_trip +
                ", level=" + level +
                ", lon=" + lon +
                ", lat=" + lat +
                ", km=" + km +
                ", battery=" + battery +
                ", sent=" + sent +
                ", sending_error=" + sending_error +
                ", json_data='" + json_data + '\'' +
                '}';
    }

    public String toStringOneLine() {
        return "Event{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", event=" + event +
                ", label=" + label +
                ", intval=" + intval +
                ", txtval='" + txtval +
                ", id_customer=" + id_customer +
                ", id_trip=" + id_trip +
                ", level=" + level +
                ", lon=" + lon +
                ", lat=" + lat +
                ", km=" + km +
                ", battery=" + battery +
                ", sent=" + sent +
                ", sending_error=" + sending_error +
                ", json_data='" + json_data + +
                '}';
    }
}
