package com.apps.meks.arduapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /*
         * Notifications from UsbService will be received here.
         */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private MyHandler mHandler;

    //UI Elemente
    private static TextView selectedCurveTextView;
    private static TextView primerIgnitionTextView;
    private static TextView voltageTextView;
    private static TextView temperatureTextView;
    private static TextView rpmTextView;

    private static int rpm = 0; //revolution per minute = Drehzahl
    private static double voltage = 0;
    private static int temperature = 0;
    private static int primerIgnition = 0; //Vorzündung
    private static int selectedCurve = 0;

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        //UI Elemente
        selectedCurveTextView = (TextView) findViewById(R.id.selectedCurve);
        primerIgnitionTextView = (TextView) findViewById(R.id.primerIgnition);
        voltageTextView = (TextView) findViewById(R.id.voltage);
        temperatureTextView = (TextView) findViewById(R.id.temperature);
        rpmTextView = (TextView) findViewById(R.id.rpm);

        mHandler = new MyHandler(this);
//          Send button

//        editText = (EditText) findViewById(R.id.editText1);
//        Button sendButton = (Button) findViewById(R.id.buttonSend);
//        sendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!editText.getText().toString().equals("")) {
//                    String data = editText.getText().toString();
//                    if (usbService != null) { // if UsbService was correctly binded, Send data
//                        usbService.write(data.getBytes());
//                    }
//                }
//            }
//        });

        String inputCommand = "A";
                try{
            while (true){
                Thread.sleep(1000);
                usbService.write(inputCommand.getBytes());
            }

        }catch(InterruptedException e){ }


    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;

                    parseData(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }


        public void parseData(String data){
            /**
            * split data into the individual values
             * data = A150,275,85,21,1
             * A = Anzeige
             * 150 = Drehzahl
             * 275 = Spannung
             * 85 = Temperatur
             * 21 = Vorzündung
             * 1 = angewählte Kurve
            **/


            //TODO: diese LOGIC muss noch überarbeitet werden !!!

            //A - ANZEIGE
            if(data.startsWith("A") ){
                String[] values = data.substring(1).split(",");
                if(values.length == 5){

                    //split the first valaue - to get the input letter
                    try{

                        rpm = Integer.parseInt(values[0]) * 10;
                        voltage = Double.parseDouble(values[1]) / 10;
                        temperature = Integer.parseInt(values[2]);
                        primerIgnition = Integer.parseInt(values[3]);
                        selectedCurve = Integer.parseInt(values[4].trim());

                        //Set UI Elements
                        rpmTextView.setText(rpm);
                        voltageTextView.setText(String.valueOf(voltage) + " V");
                        temperatureTextView.setText(String.valueOf(temperature) + " °C");
                        primerIgnitionTextView.setText(String.valueOf(primerIgnition) + " °C");
                        selectedCurveTextView.setText(selectedCurve);

                    }catch (Exception e){ }
                }
            }

            //k - Kurve 1

            //l - Kurve 2

            //V - Version

            //d - dwell us

            //p - anzeige alle parameter permanent

            //7 - Read A0
        }
    }

}
