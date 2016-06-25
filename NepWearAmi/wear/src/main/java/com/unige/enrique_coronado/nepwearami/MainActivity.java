/**
 * Android wear app to obtain and send sensor information
 *
 * @author Luis Enrique Coronado Zuñiga
 * @date   June, 2016
 */

package com.unige.enrique_coronado.nepwearami;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.CheckBox;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main function
 */
public class MainActivity extends Activity implements SensorEventListener {

    //Interface variables
    private TextView mTextView;
    private TextView mButton;
    private CheckBox checkBox1;

    GoogleApiClient client;

    boolean writeEnabled=false;
    long timestamp = 0;
    int inizioreg=1;
    String nodeId;

    //Sensor variables
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyro;

    float[] acc ={0,0,0};
    float[] gyro ={0,0,0};

    ArrayList<String> toSendMov1 = new ArrayList<String>();
    ArrayList<String> toSendMov4 = new ArrayList<String>();

    int packetSize = 10;

    String dataAcc = "";
    String dataGyro = "";
    String strSendAcc = "";
    String strSendGyro = "";

    List<Node> nodes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.textT);
                mButton = (Button) stub.findViewById(R.id.btnWearStart);
                checkBox1 = (CheckBox) findViewById(R.id.checkBox1);

                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeEnabled=true;
                        inizioreg=0;
                        timestamp = System.currentTimeMillis();
                        ((TextView) findViewById(R.id.textT)).setText("Sending");
                        Log.d("mess", "Start Write");
                    }
                });

                ((Button) stub.findViewById(R.id.btnWearStop)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeEnabled = false;
                        timestamp = 0;
                        toSendMov1.clear();
                        toSendMov4.clear();
                        ((TextView)findViewById(R.id.textT)).setText("Waiting");
                        Log.d("mess", "Stop Write");
                    }
                });


            }
        });

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener((SensorEventListener) this, senAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        senGyro = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener((SensorEventListener) this, senGyro, SensorManager.SENSOR_DELAY_GAME);

        retrieveDeviceNode();

    }

    //Functions for the communication
    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    private void retrieveDeviceNode() {
        final GoogleApiClient client = getGoogleApiClient(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(150, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                nodes = result.getNodes();
                if (nodes.size() > 0) {
                    for(Node node : nodes) {
                        if (node.isNearby())
                            nodeId = node.getId();
                    }
                }
                client.disconnect();
            }
        }).start();
    }


    /**
     * Function that send the message
     */
    private void sendMessage(final String mess, final int type_mex, final int type_sensor) {
        client = getGoogleApiClient(this);
        if(writeEnabled==true) {
            if (nodeId != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        client.blockingConnect(150, TimeUnit.MILLISECONDS);
                        if (type_mex == 1) {
                            Wearable.MessageApi.sendMessage(client, nodeId, "/motion1", mess.getBytes());
                        }
                        if (type_mex == 4 && checkBox1.isChecked()) {
                            Wearable.MessageApi.sendMessage(client, nodeId, "/motion4", mess.getBytes());
                        }
                        client.disconnect();
                    }
                }).start();
            }
        }

    }

    /**
     * This event runs when a sensor produce data
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            acc[0] = sensorEvent.values[0];
            acc[1] = sensorEvent.values[1];
            acc[2] = sensorEvent.values[2];


            dataAcc = "a" + ";" + sensorEvent.timestamp + ";" + acc[0] + ";" + acc[1] + ";" + acc[2];
            toSendMov1.add(dataAcc);

        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro[0] = sensorEvent.values[0];
            gyro[1] = sensorEvent.values[1];
            gyro[2] = sensorEvent.values[2];

            dataGyro = "y" + ";" + sensorEvent.timestamp +";"+gyro[0]+";"+gyro[1]+";"+gyro[2];
            toSendMov4.add(dataGyro);
        }

        if (toSendMov1.size() == packetSize) {
            for (int i = 0; i < packetSize; i++)
                strSendAcc += toSendMov1.get(i) + "\n";

            sendMessage(strSendAcc,1, 1);
            toSendMov1.clear();
            strSendAcc = "";
        }

        if(toSendMov4.size() == packetSize)
        {
            for(int i=0; i<packetSize; i++)
                strSendGyro+=toSendMov4.get(i)+"\n";
            sendMessage(strSendGyro,4, 4);
            toSendMov4.clear();
            strSendGyro = "";
        }
    }

    //Not used
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
