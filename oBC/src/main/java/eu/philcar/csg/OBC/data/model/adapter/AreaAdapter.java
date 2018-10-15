package eu.philcar.csg.OBC.data.model.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import eu.philcar.csg.OBC.data.model.Area;

/**
 * Created by Fulvio on 02/07/2018.
 * Use this class only for writing the area on file otherwise the order might not be the same as the initial Json
 */

public class AreaAdapter extends TypeAdapter<Area> {

    @Override
    public void write(JsonWriter out, Area value) throws IOException {

        out.beginObject();
        out.name("close_trip").value(value.getClose_trip());
        out.name("costo_apertura").value(value.getCosto_apertura());
        out.name("costo_chiusura").value(value.getCosto_chiusura());
        out.name("coordinates").beginArray();
        for (Double coord : value.getCoordinates()) {
            if (coord == 0D)
                out.value(0);
            else
                out.value(coord);
        }
        out.endArray();
        out.endObject();
    }

    @Override
    public Area read(JsonReader in) throws IOException {
        return new Area();
    }
}
