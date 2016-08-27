package com.nikdi.stepsbattle;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


public class StepsService extends Service implements SensorEventListener {

    SensorManager sensorManager;
    private float   mLimit = 10;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;

    @Override
    public void onCreate() {
        Log.d("Log", "service start");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        int h = 480;
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));

        super.onCreate();
    }

    public void setSensitivity(int sensitivity) {
        float sens = 1.97f;
        switch(sensitivity){// 1.97  2.96  4.44  6.66  10.00  15.00  22.50  33.75  50.62
            case 1:
                sens = 1.97f;
                break;
            case 2:
                sens = 2.96f;
                break;
            case 3:
                sens = 4.44f;
                break;
            case 4:
                sens = 6.66f;
                break;
            case 5:
                sens = 10.00f;
                break;
            case 6:
                sens = 15.00f;
                break;
            case 7:
                sens = 22.50f;
                break;
            case 8:
                sens = 33.75f;
                break;
            case 9:
                sens = 50.62f;
                break;
        }

        mLimit = sens;
    }

    @Override
    public void onDestroy() {
        Log.d("Log", "service stop");
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float vSum = 0;
                for (int i=0 ; i<3 ; i++) {
                    final float v = mYOffset + event.values[i] * mScale[1];
                    vSum += v;
                }
                int k = 0;
                float v = vSum / 3;

                float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                if (direction == - mLastDirections[k]){
                    int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                    mLastExtremes[extType][k] = mLastValues[k];
                    float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                    if (diff > mLimit) {

                        boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
                        boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
                        boolean isNotContra = (mLastMatch != 1 - extType);

                        if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                            //increase steps
                            SharedPreferences settings = getSharedPreferences("steps", 0);
                            SharedPreferences.Editor editor = settings.edit();
                            int count = settings.getInt("count", 0);
                            editor.putInt("count", ++count);
                            editor.commit();

                            //send message
                            Intent intent = new Intent("com.nikdi.stepsbattle");
                            sendBroadcast(intent);

                            mLastMatch = extType;
                        }
                        else {
                            mLastMatch = -1;
                        }
                    }
                    mLastDiff[k] = diff;
                }
                mLastDirections[k] = direction;
                mLastValues[k] = v;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {return null;}
}
