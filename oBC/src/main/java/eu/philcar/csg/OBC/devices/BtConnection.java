package eu.philcar.csg.OBC.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;

//import rfidTest.qnet.it.DemoActivity.myconnectThread;

public class BtConnection {
    private DLog dlog = new DLog(this.getClass());

    private final boolean BT_CONNECTION_DISABLED = false;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Context context;
    private Handler extHandler;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket = null;
    private BluetoothDevice btDevice = null;
    private InputStream inStream;
    private OutputStream outStream;
    private Set<BluetoothDevice> pairedDevices;
    private ConnectThread connectThread = null;
    private ReceiveThread receiveThread = null;
    private int CurrentState;
    private boolean connected = false;

    public static final int MSG_STATE_CHANGE = 1;
    public static final int MSG_RECEIVE = 2;
    public static final int MSG_CONNECTED = 3;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_FAILED = 4;

    public BtConnection(Context context, Handler handler) {

        setContext(context);
        setHandler(handler);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter != null) {
            dlog.i("BT adapter found");
        } else {
            dlog.e("BT adapter not found!");
            return;
        }

        if (!btAdapter.isEnabled()) {
            dlog.i("BT not enabled. Enabling");
            if (!btAdapter.enable()) {
                dlog.e("Can't enable BT");
            }

        }

        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
            dlog.w("Cancel discovery");
        }

    }

    public boolean isBonded(String address) {

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        for (BluetoothDevice d : pairedDevices) {
            if (d.getAddress().equalsIgnoreCase(address))
                return true;
        }

        return false;
    }

    public Set<BluetoothDevice> getBondedDevices() {

        if (btAdapter == null) {
            dlog.e("btAdapter==null");
            return null;
        }

        pairedDevices = btAdapter.getBondedDevices();
        return pairedDevices;
    }

    public BluetoothDevice getDevice(String address) {

        if (btAdapter == null) {
            dlog.e("btAdapter==null");
            return null;
        }

        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            dlog.e("Invalid BT address: " + address);
            return null;
        }

        BluetoothDevice tdevice = btAdapter.getRemoteDevice(address);
        return tdevice;
    }

    public String getPin(String Name) {
        String pin = "";
        if (!Name.startsWith("#")) {
            byte[] data = (Name + "o").getBytes();

            CRC32 crc = new CRC32();

            crc.reset();
            crc.update(data, 0, data.length);
            long v = crc.getValue();
            pin = String.format("%08X", v).toLowerCase();
        } else {
            pin = "0593";
        }
        return pin;

    }

    public boolean isConnected() {
        return connected;
    }

    public BluetoothSocket getSocket() {
        if (connected) {
            return btSocket;
        } else {
            return null;
        }

    }

//	public void doPairing(BluetoothDevice device) {
//
//		String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
//
//		Intent intent = new Intent(ACTION_PAIRING_REQUEST);
//		String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
//		intent.putExtra(EXTRA_DEVICE, device);
//
//		String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
//		String PAIRING_VARIANT_PIN = "4321R1234";
//
//		intent.putExtra(EXTRA_PAIRING_VARIANT, PAIRING_VARIANT_PIN);
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//		context.startActivity(intent);
//	}

    public void startPairing(BluetoothDevice device) {

        if (Build.VERSION.SDK_INT < 19) {
            IntentFilter pairingFilter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
            App.Instance.registerReceiver(new PairingBroadcastReceiver(), pairingFilter);

            //Bonding the device:
            Method mm;
            try {
                mm = device.getClass().getMethod("createBond", (Class[]) null);
                mm.invoke(device, (Object[]) null);
            } catch (Exception e) {
                dlog.e("Error in startPairingPin:", e);
            }
        } else {
            String pin = getPin(device.getName());
            device.setPin(pin.getBytes());
            device.createBond();
        }

    }

    private class PairingBroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //may need to chain this to a recognizing function
            if (action.equalsIgnoreCase("android.bluetooth.device.action.PAIRING_REQUEST")) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                setPairingPin(device);

            }
        }
    }

    public void setPairingPin(BluetoothDevice device) {

        String pin = getPin(device.getName());

        App.Instance.setDevice(device.getName(), device.getAddress());
        App.Instance.loadPreferences();

        byte[] pinBytes = pin.getBytes();
        try {
            dlog.d("Try to set the PIN");
            Method m = device.getClass().getMethod("setPin", byte[].class);
            m.invoke(device, pinBytes);
            dlog.d("Success to add the PIN.");

            try {
                //Thread.sleep(1000);
                device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                Class[] arrayOfClass = new Class[0];
                //device.getClass().getMethod("cancelPairingUserInput", arrayOfClass).invoke(device);
                dlog.d("Success to setPairingConfirmation.");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                dlog.e("Error in setPairingConfirmation", e);
            }
        } catch (Exception e) {
            dlog.e("Error in setPairingPin:", e);

        }
    }

    public synchronized void reconnect() {

        dlog.i("BT reconnect request");

        if (btAdapter == null)
            return;

        if (btAdapter.isDiscovering())
            return;

        if (CurrentState != STATE_CONNECTING)
            this.Close();

        connect(btDevice);
    }

    public synchronized void connect(BluetoothDevice xdevice) {

        if (btAdapter.isDiscovering()) {
            dlog.w("BT is discovering. Connect request ignored");
            return;
        }

        if (xdevice == null) {
            dlog.w("null device. New request ignored");
            return;
        }

        switch (xdevice.getBondState()) {

            case BluetoothDevice.BOND_BONDING:
                dlog.d("Still bonding... abort connect");
                return;

            case BluetoothDevice.BOND_NONE:
                dlog.d("Not bonded... abort connect and start pairing");
                //startPairing(xdevice);
                //return;
                break;
        }

        if (!BT_CONNECTION_DISABLED) {

            btDevice = xdevice;
            String btDeviceAddr = xdevice.getAddress();
            if (CurrentState == STATE_CONNECTING && connectThread != null && btDeviceAddr.equalsIgnoreCase(connectThread.device.getAddress())) {
                dlog.w("BT connection already in progress. New request ignored");
                return;
            } else {
                if (connectThread != null)
                    connectThread.stopConnecting();
            }
            dlog.i("Starting BT connection with :" + xdevice.toString());

            if (connected)
                this.Close();

//			if (connectThread != null) {
//				dlog.i("..destroy previous thread");
//				connectThread.cancel();
//			}
            connected = false;
            connectThread = new ConnectThread(btDevice);
            connectThread.start();
            setState(STATE_CONNECTING);
        }

    }

    public void setHandler(Handler handler) {
        if (extHandler != null) {
            extHandler.removeCallbacks(connectThread);
        }

        this.extHandler = handler;
    }

    public void setContext(Context context) {
        if (this.context != null) {

            try {
                this.context.unregisterReceiver(searchDevices);
            } catch (IllegalArgumentException e) {

            }
        }

        this.context = context;

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentfilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentfilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentfilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentfilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentfilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(searchDevices, intentfilter);

    }

    public void Close() {

        //context.unregisterReceiver(searchDevices);
        dlog.d("Close connection");

        if (connectThread != null) {
            dlog.d("Stop connection thread");
            connectThread.stopConnecting();

            try {
                connectThread.join(5000);
            } catch (InterruptedException e) {

            }

            dlog.d("Force stop connection thread");
            connectThread.interrupt();

            try {
                connectThread.join(5000);
            } catch (InterruptedException e) {

            }

            connectThread = null;
        }

        if (receiveThread != null) {

            dlog.d("Stop receive thread");

            receiveThread.interrupt();

            if (btSocket != null)
                try {
                    dlog.d("Closing socket");
                    btSocket.close();
                } catch (IOException e1) {
                    dlog.e("Closing socket", e1);
                }

            try {
                receiveThread.join(5000);
            } catch (InterruptedException e) {

            }
            receiveThread = null;
        }

        if (extHandler != null) {
            dlog.d("Remove callbacks");
            extHandler.removeCallbacks(connectThread);
        }
        connected = false;
    }

    private synchronized void setState(int state) {
        dlog.d("Change state: " + state);
        CurrentState = state;
        if (extHandler != null) {
            extHandler.obtainMessage(MSG_STATE_CHANGE, state, -1).sendToTarget();
        }
    }

    private class ConnectThread extends Thread {

        int countDownTime = 0;
        int timeout = 0;
        boolean isConnected;
        BluetoothDevice device;
        boolean isStopRequested = false;

        public ConnectThread(BluetoothDevice device) {
            this(device, -1);
        }

        public ConnectThread(BluetoothDevice device, int timeout) {
            this.device = device;
            this.timeout = timeout;
            btSocket = null;
            isConnected = false;

            dlog.d("Create connectThread for :" + device.getName() + " - " + device.getAddress());

            try {
                btSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                //btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                dlog.e("Can't create RFCOMM socket", e);
            }

        }

        public void stopConnecting() {
            dlog.d("ConnectThread Stop requested");
            isStopRequested = true;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("ConnectThread : " + device.getName());
            dlog.i("Starting BT connection loop : " + device.getName());

            if (btAdapter.isDiscovering()) {
                dlog.d("Cancel pending discovery");
                btAdapter.cancelDiscovery();
            }

            int failcount = 0;
            isConnected = false;
            while (!btSocket.isConnected() && !isStopRequested && failcount < 10) {

                try {
                    if (!btAdapter.isDiscovering())
                        btSocket.connect();
                    else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (IOException e) {
                    failcount++;
                    if (failcount > 10 && (failcount % 10 == 0)) {
                        try {
                            btSocket.close();
                            //btAdapter.disable();
                            delay();
                            delay();
                            //btAdapter.enable();
                            delay();
                            delay();
                            btSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                        } catch (IOException ex) {
                            dlog.e("Can't create RFCOMM socket", ex);
                        }
                    }

                    delay();
                    dlog.i("BT connection failed.Retry");
                    continue;
                }
            }

            if (btSocket.isConnected()) {
                dlog.i("BT connection success : " + device.getName());
                connected = true;
                failcount = 0;

                try {
                    inStream = btSocket.getInputStream();
                    outStream = btSocket.getOutputStream();
                } catch (IOException e) {
                    dlog.e("Getting streams :", e);
                }

                receiveThread = new ReceiveThread();
                receiveThread.start();

                setState(STATE_CONNECTED);
                isConnected = true;
            } else {
                dlog.w("BT connection aborted :" + device.getName() + " - " + device.getAddress());
                try {
                    btSocket.close();
                } catch (IOException e) {
                    dlog.e("Closing socket:", e);
                }
                setState(STATE_FAILED);
            }

        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
            }
        }

        public boolean delay() {
            int delayTime = 1000;
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException e) {
            }

            return false;
        }
    }

    public void SendBytes(byte data[]) {

        OutputStream os;

        if (btSocket == null || !btSocket.isConnected()) {
            if (receiveThread != null && receiveThread.isInterrupted()) {
                dlog.d("Receive thread is interrupted. Don't restart");
                return;
            }
            reconnect();
            return;
        }

        try {
            os = btSocket.getOutputStream();
            os.write(data);
        } catch (IOException e) {

            if (receiveThread != null && receiveThread.isInterrupted()) {
                dlog.d("Receive thread is interrupted. Don't restart");
                return;
            }
            dlog.e("BT output stream exception.Reconnecting...", e);
            reconnect();
        }
    }

    public void SendString(String str) {
        OutputStream os;

        if (btSocket == null || !btSocket.isConnected()) {
            if (receiveThread != null && receiveThread.isInterrupted()) {
                dlog.d("Receive thread is interrupted. Don't restart");
                return;
            }
            reconnect();
            return;
        }

        try {
            synchronized (btSocket) {
                os = btSocket.getOutputStream();
                byte[] buffer = str.getBytes();
                os.write(buffer);
                os.flush();
            }
        } catch (IOException e) {
            if (receiveThread != null && receiveThread.isInterrupted()) {
                dlog.d("Receive thread is interrupted. Don't restart");
                return;
            }
            dlog.e("BT output stream exception.Reconnecting...", e);
            reconnect();
        } catch (Exception e) {
            dlog.e("btSocket.getOutputStream  unexpected exception", e);
            reconnect();
        }

    }

    private class ReceiveThread extends Thread {
        StringBuilder sb;

        @Override
        public void run() {

            Thread.currentThread().setName("ReceiveThread :");

            sb = new StringBuilder();
            int ch = 0;
            while (!this.isInterrupted()) {

                try {
                    ch = inStream.read();
                } catch (IOException e) {
                    if (!this.isInterrupted()) {
                        dlog.e("BT input stream exception.Reconnecting...", e);
                        reconnect();
                    }
                    return;
                } catch (Exception e) {
                    break;
                }

                if (ch < 0) {
                    dlog.e("BT input stream closed.Reconnecting...");
                    reconnect();
                    return;
                }

                if (ch == 0)
                    continue;

                if (ch != '\n') {
                    sb.append((char) ch);
                } else {
                    extHandler.obtainMessage(MSG_RECEIVE, sb.toString()).sendToTarget();
                    //TODO: notify cmd
                    //sb.setLength(0);
                    sb = new StringBuilder();
                }

            }

        }
    }

    private final BroadcastReceiver searchDevices = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = null;

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //New device found
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        //mt.setText("Pairing...");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        //mt.setText("Pairing is complete.");
                        break;
                    case BluetoothDevice.BOND_NONE:
                        //mt.setText("Unpaired.");
                        break;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //mt.setText("Scanning...");

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                //mt.setText("The scan is complete.");

            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

                switch (btAdapter.getState()) {
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //mt.setText("The Bluetooth is starting...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //mt.setText("Bluetooth has opened.");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //mt.setText("The Bluetooth is closing...");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        //mt.setText("The Bluetooth is Unopened.");
                        break;
                }

            }

        }
    };

}
