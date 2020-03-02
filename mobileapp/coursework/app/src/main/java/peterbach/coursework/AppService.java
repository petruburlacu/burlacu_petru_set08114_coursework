package peterbach.coursework;

import android.app.Service;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

public class AppService extends Service {

    private boolean recording = false;

    private MediaRecorder mediaRecorder = new MediaRecorder();
    private String myAudioFileName = null;

    private SensorManager sensorManager;
    private Long lastState;
    SensorEventListener listener;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.e("-------------", "Started ON Command"); //checks if start on Start
        sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE); //gets sensorManager
        lastState = System.currentTimeMillis();
        listener = new ShakeListen();
        sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL); //registers the event
        //put reference here
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        Toast.makeText(getApplicationContext(), "Started on Create", Toast.LENGTH_LONG).show();

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        sensorManager.unregisterListener(listener);
        Toast.makeText(this, "Destroy", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    /**
     * SHAKE LISTENER CLASS
     **********************************************************************/
    public class ShakeListen implements SensorEventListener { //is shaken detect

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub

            // https://stackoverflow.com/questions/2317428/android-i-want-to-shake-it
            float appAccelerometer = 0;
            float currentAccel = 0;
            float lastAccel;
            float xCoord = event.values[0];
            float yCoord = event.values[1];
            float zCoord = event.values[2];

            lastAccel = currentAccel;
            currentAccel = (float) Math.sqrt((double) (xCoord * xCoord + yCoord * yCoord + zCoord * zCoord));
            float diff = currentAccel - lastAccel;
            appAccelerometer = appAccelerometer * 0.8f + diff; // perform low-cut filter

            if (appAccelerometer > 12) {
                Log.e("------------", "ACCELEROMETER_DETECTED" + Arrays.toString(event.values));

            }


            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                //getAccelerometer(event); //starts accelerometer event + change name into smth more explicit
                //Log.e("------------", "ACCELEROMETER_DETECTED" + Arrays.toString(event.values));
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.e("------------", "on accuracy change");
            // TODO Auto-generated method stub

        }
    }
}