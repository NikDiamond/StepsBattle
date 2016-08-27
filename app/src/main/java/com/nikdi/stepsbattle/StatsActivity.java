package com.nikdi.stepsbattle;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StatsActivity extends AppCompatActivity {

    NotificationManager notifyManager;
    BroadcastReceiver rv;
    int stepsCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        StartService(findViewById(R.id.startButton));
        ReloadSteps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StartReceiver();
        ReloadSteps();
    }

    public void ReloadSteps(){
        SharedPreferences settings = getSharedPreferences("steps", 0);
        int count = settings.getInt("count", 0);

        TextView counter = (TextView) findViewById(R.id.StepsCounter);
        counter.setText(count+"");
    }

    public void StartReceiver(){
        if(rv != null) return;

        rv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ReloadSteps();
            }
        };
        IntentFilter filt = new IntentFilter("com.nikdi.stepsbattle");
        registerReceiver(rv, filt);
    }

    public void StartService(View v){
        if(!isMyServiceRunning(StepsService.class)) {
            StartReceiver();

            //service
            startService(new Intent(this, StepsService.class));
            ShowNotification();
        }

        Button b = (Button) v;
        b.setText(getResources().getString(R.string.stop_b));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                StopService(v);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Log", "result " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void StopService(View v){
        if(isMyServiceRunning(StepsService.class)) {
            if(rv != null)
                unregisterReceiver(rv);

            stopService(new Intent(this, StepsService.class));
            HideNotification();
        }

        Button b = (Button) v;
        b.setText(getResources().getString(R.string.start_b));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                StartService(v);
            }
        });
    }

    public void ShowNotification(){
        notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, StatsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pintent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification n  = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Whip is On!")
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true)
                .setContentIntent(pintent)
                .build();
        notifyManager.notify(0, n);
    }

    public void HideNotification(){
        if(notifyManager == null)
            notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notifyManager.cancel(0);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
