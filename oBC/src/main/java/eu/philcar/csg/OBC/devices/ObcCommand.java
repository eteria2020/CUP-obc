package eu.philcar.csg.OBC.devices;

import android.os.Message;
import android.os.Messenger;

public class ObcCommand {
    private String cmd;
    private String args;
    public Messenger replyTo = null;
    public Message timeout = null;

    public static ObcCommand CreatePing() {
        return new ObcCommand("PING");
    }

    public static ObcCommand CreateLed(int led, int state) {

        return new ObcCommand("LED,", led + "," + state);

    }

    public static ObcCommand CreateDoors(int state, String message) {

        message = (message != null ? message : "");
        return new ObcCommand("DOORS,", "" + (state == 1 ? '1' : '0') + "," + message);

    }

    public static ObcCommand CreateLcd(String message) {

        message = (message != null ? message : "");
        return new ObcCommand("LCD,", message);

    }

    public static ObcCommand CreateEngine(int state) {
        return new ObcCommand("ENGINE,", "" + (state == 1 ? '1' : '0'));
    }

    public static ObcCommand CreateWhitelist() {
        return new ObcCommand("WL_UPLOAD");
    }

    public static ObcCommand CreateReservation(String code) {
        return new ObcCommand("RESERVATION," + (code != null ? code : ""));
    }

    public static ObcCommand CreateSetTag(String code) {
        return new ObcCommand("SETTAG," + (code != null ? code : ""));
    }

    public static ObcCommand CreateReboot() {
        return new ObcCommand("REBOOT");
    }

    public static ObcCommand CreateSetTarga(String targa) {
        return new ObcCommand("TARGA," + (targa != null ? targa : ""));
    }

    public static ObcCommand CreateSetCharger(int state) {
        return new ObcCommand("CHARGE," + (state != 0 ? "on" : "off"));
    }

    public ObcCommand(String cmd) {
        this(cmd, null);
    }

    public ObcCommand(String cmd, String args) {
        this.cmd = cmd;
        this.args = args;
    }

    public boolean MatchesResponse(String response) {
        response = response.trim();
        return response.equalsIgnoreCase(cmd);

    }

    @Override
    public String toString() {
        if (args != null) {
            if (!cmd.endsWith(",") || args.startsWith(","))
                cmd = "," + cmd;
            return cmd + args;
        } else
            return cmd;
    }

}
