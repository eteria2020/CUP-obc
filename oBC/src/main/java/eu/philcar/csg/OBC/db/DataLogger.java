package eu.philcar.csg.OBC.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import eu.philcar.csg.OBC.data.model.ServerResponse;
import eu.philcar.csg.OBC.service.DataManager;
import eu.philcar.csg.OBC.service.ObcService;

/**
 * Created by Fulvio on 01/10/2018.
 */

@DatabaseTable(tableName = "dataLogger", daoClass = DataLoggers.class)
public class DataLogger extends DbRecord<ServerResponse> implements CustomOp{


    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(index = true)
    public Date time;

    @DatabaseField
    public long km; //
    @DatabaseField
    public int soc; //
    @DatabaseField
    public float v_min_cell; //
    @DatabaseField
    public float v_max_cell; //
    @DatabaseField
    public int ampere; //
    @DatabaseField
    public float v_battery; //
    @DatabaseField
    public long km_from_trip_beg; //



    @Override
    public void onDbWrite() {
    }

    @Override
    public void handleResponse(ServerResponse serverResponse, DataManager manager, int callOrder) {

    }
}
