package com.eissler.micha.hbgvertretungsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Received the Intent!");

        Intent forwardIntent = new Intent(context, CheckForNewDataService.class);
        context.startService(forwardIntent);
    }
}