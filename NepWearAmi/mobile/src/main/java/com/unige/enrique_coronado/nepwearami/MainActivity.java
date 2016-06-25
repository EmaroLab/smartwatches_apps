/**
 * Android app for transmission of data from a smartwatch to a computer
 *
 * @author Luis Enrique Coronado Zuñiga
 * @date   June, 2016
 */

package com.unige.enrique_coronado.nepwearami;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * Main function
 */
public class MainActivity extends Activity implements SensorEventListener {

    //Client variable
    Socket client = null;
    PrintStream oos = null;

    //Sensor variables
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    //Interface variables
    private CheckBox checkBoxData;
    private CheckBox checkBoxS;

    float[] acc_value = {0,0,0}; //!< Value where the accelerometer data are stored to show in the screen
    String ipaddress ="130.251.13.125"; //!< IP address variable
    int port = 8080; //!< Port number ex: 8080


    ArrayList<String> toRec = new ArrayList<String>();
    String lastMessageMov = "";
    long time_mov = 0;
    String type_mov = "";
    int i = 0;
    int l = 0;

    @Override
    /**
     * Function that are executed in the beginning of the program
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Here the tools of the interface are defined
        checkBoxData = (CheckBox) findViewById(R.id.checkBoxData);
        checkBoxS = (CheckBox) findViewById(R.id.checkBoxS);

        //Here the instances to obtain data from sensors are defined
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //An accelerometer sensor is defined
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //New Listener of the accelerometer sensor.
        //SensorManager define the frequency, Options: SENSOR_DELAY_NORMAL, SENSOR_DELAY_NORMAL, SENSOR_DELAY_GAME, SENSOR_DELAY_UI
        senSensorManager.registerListener((SensorEventListener) this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //For connection with the computer (client side)
        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        googleApiClient.connect();

        Wearable.MessageApi.addListener(googleApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {

            }
        });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

    }


    /**
     * Use to tranfer the data from smartwatch to the pc
     */
    public class MessageReceiver extends BroadcastReceiver {

        @Override
        /**
         *This function is executed when data from the smartwatch are received
         */
        public void onReceive(Context context, Intent intent) {

            final String message = intent.getStringExtra("message");

            //A new thread is executed (this for no block the main loop)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //Socket client connection
                        if(client==null || client.isClosed()) {
                            EditText editText = (EditText) findViewById(R.id.ipText);
                            ipaddress = editText.getText().toString();
                            client = new Socket(ipaddress,port);
                            oos = new PrintStream(client.getOutputStream());
                        }

                        //Send the data of the smartwatch
                        if (checkBoxS.isChecked()) {
                            oos.println(message);
                        }
                        //The robot is not allow to move
                        else
                        {
                            oos.println("a;000;0.0;0.0;0.0");
                        }
                        oos.flush();


                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }).start();

            //If data is acceleration, type == 1
            if(intent.getStringExtra("type")=="1") {
                StringTokenizer info = new StringTokenizer(message, "\n");
                while (info.hasMoreTokens()) {
                    toRec.add(info.nextToken());
                }

                lastMessageMov = toRec.get(0);
                //Split message
                StringTokenizer value = new StringTokenizer(lastMessageMov, ";");
                type_mov = value.nextToken();
                time_mov = Long.valueOf(value.nextToken());

                //Convert values to float
                for (i = 0; i < 3; i++) {
                    acc_value[i] = Float.valueOf(value.nextToken());
                }

                //Show in the screen the data of acceleration
                if (checkBoxData.isChecked()) {
                    ((TextView) findViewById(R.id.dato)).setText("IP :" + ipaddress + "\n" + "port :" + port + "\n" + "x: " + acc_value[0] + "\n" + "y: " + acc_value[1] + "\n" + "z: " + acc_value[2]);
                }

                //Reset values (maybe this is not needed)
                for (l = 0; l < 3; l++) {
                    acc_value[l] = 0;
                }
                toRec.clear();
            }
        }
    }

    /**
     * Close and reset the connection
     */
    public void buttonClickReset(View view) throws IOException {
        if(client!=null) {
            client.close();
        }
    }

    /**
     * Send save signal
     */
    public void buttonClickStart(View view) throws IOException {
        checkBoxS.setChecked(true);
        //A new thread is executed (this for no block the main loop)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Socket client connection
                    if(client==null || client.isClosed()) {
                        EditText editText = (EditText) findViewById(R.id.ipText);
                        ipaddress = editText.getText().toString();
                        client = new Socket(ipaddress,port);
                        oos = new PrintStream(client.getOutputStream());
                    }

                    oos.println("start;000;0.0;0.0;0.0");
                    oos.flush();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }


    /**
     * Stop button
     */
    public void buttonClickStop(View view) throws IOException {
        checkBoxS.setChecked(false);
//A new thread is executed (this for no block the main loop)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Socket client connection
                    if(client==null || client.isClosed()) {
                        EditText editText = (EditText) findViewById(R.id.ipText);
                        ipaddress = editText.getText().toString();
                        client = new Socket(ipaddress,port);
                        oos = new PrintStream(client.getOutputStream());
                    }

                    oos.println("stop;000;0.0;0.0;0.0");
                    oos.flush();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    //No used
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    //No used
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //No used
    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    //No used
    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

}
