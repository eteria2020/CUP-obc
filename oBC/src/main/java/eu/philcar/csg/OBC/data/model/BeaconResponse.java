package eu.philcar.csg.OBC.data.model;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;

/**
 * Created by Fulvio on 01/03/2018.
 */

public class BeaconResponse extends BaseResponse {
    private int reservations;
    private int commands;

    public BeaconResponse(int reservations, int commands) {
        this.reservations = reservations;
        this.commands = commands;
    }

    public int getReservations() {
        return reservations;
    }

    public void setReservations(int reservations) {
        this.reservations = reservations;
    }

    public int getCommands() {
        return commands;
    }

    public void setCommands(int commands) {
        this.commands = commands;
    }

    @Override
    public String toString() {
        return "reservation: " + reservations + " commands: " + commands;
    }
}
