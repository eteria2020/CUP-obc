package eu.philcar.csg.OBC.helpers;

import android.os.Messenger;

/**
 * Created by Fulvio on 05/07/2017.
 */
public class Clients {

    public static final int Welcome =0;
    public static final int Main =2;
    public static final int SOS =3;
    public static final int FAQ =4;
    public static final int Goodbye =5;
    public static final int ServiceTest =6;

    private final int name;
    private final Messenger client;

    public Clients(int name, Messenger client){
        this.name =name;
        this.client=client;
    }

    public int getName() {
        return name;
    }

    public Messenger getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        return client.equals(o);
    }
}
