package com.unige.enrique_coronado.nepwearami;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


/**
 * Listener activity
 */
public class ListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        final String message = new String(messageEvent.getData());
        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra("message", message);

        //Acceleration
        if(messageEvent.getPath().equals("/motion1")) {
            messageIntent.putExtra("type", "1");
        }

        //Gyroscope
        if(messageEvent.getPath().equals("/motion4")) {
            messageIntent.putExtra("type", "4");
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    }
}
