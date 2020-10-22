package com.android.flexiblepedometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by AREG on 02.03.2017.
 */

public class PedometerAccelerometer implements SensorEventListener {

    private final static String TAG = "StepDetector";

    private static float mLimit = 10.0f;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    //------------------------------------------------------
    private static final int ACCEL_RING_SIZE = 50;
    private static final int VEL_RING_SIZE = 10;
//    private static final float STEP_THRESHOLD = 4f;
    private static final int STEP_DELAY_NS = 350000000;  //250000000

    private int accelRingCounter = 0;
    private float[] accelRingX = new float[ACCEL_RING_SIZE];
    private float[] accelRingY = new float[ACCEL_RING_SIZE];
    private float[] accelRingZ = new float[ACCEL_RING_SIZE];
    private int velRingCounter = 0;
    private float[] velRing = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;
    private int nprestep;
    //------------------------------------------------------



    private PedometerAccelerometerListener mPedometerAccelerometerListener;

    public interface PedometerAccelerometerListener {
        public void StepHasBeenDone();
    }

    public PedometerAccelerometer(Context context) {

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void PedometerAccelerometerListenerSet(PedometerAccelerometerListener listener) {
        mPedometerAccelerometerListener = listener;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void StartListen() {
        mSensorManager.registerListener(PedometerAccelerometer.this, mSensor, 20000);//SensorManager.SENSOR_DELAY_FASTEST); // SensorManager.SENSOR_DELAY_NORMAL
    }

    public void StopListen() {
        mSensorManager.unregisterListener(PedometerAccelerometer.this);
    }

    public static void setSensitivity(float sensitivity) {
        mLimit = sensitivity;
        Log.d(TAG, "set new sensitivity: " + String.valueOf(mLimit));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        //Log.d(TAG, "new accelerometer value! " + sensor.getType());
        //Log.d(TAG, "accuracy: " + event.accuracy);
        //Log.d(TAG, "values: " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1]) + " "+ String.valueOf(event.values[2]));
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
            }
            else {
                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
                if (j == 1) {
                    updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2]);
                }
            }
        }
    }

    /**
     * Accepts updates from the accelerometer.
     */
    public void updateAccel(long timeNs, float x, float y, float z) {
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        // First step is to update our guess of where the global z vector is.
        accelRingCounter++;
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0];
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1];
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2];

        float[] worldZ = new float[3];
        worldZ[0] = SensorFusionMath.sum(accelRingX) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[1] = SensorFusionMath.sum(accelRingY) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[2] = SensorFusionMath.sum(accelRingZ) / Math.min(accelRingCounter, ACCEL_RING_SIZE);

        float normalization_factor = SensorFusionMath.norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        // Next step is to figure out the component of the current acceleration
        // in the direction of world_z and subtract gravity's contribution
        float currentZ = SensorFusionMath.dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

    //    Log.d(TAG, "currentZ: " + String.valueOf(currentZ));

        float velocityEstimate = SensorFusionMath.sum(velRing);

        if (velocityEstimate > mLimit && oldVelocityEstimate <= mLimit
                && (timeNs - lastStepTimeNs > STEP_DELAY_NS)) {
  //          Log.d(TAG, "lastStepTimeNs: " + String.valueOf(timeNs));
  //          Log.d(TAG, "mLimit: " + String.valueOf(mLimit));
            nprestep ++;
            if((timeNs - lastStepTimeNs) > 2000000000) {nprestep=0;}
            if (nprestep > 4)  {
                mPedometerAccelerometerListener.StepHasBeenDone();
            }
            lastStepTimeNs = timeNs;

        }
        oldVelocityEstimate = velocityEstimate;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /*if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (accuracy != SensorManager.SENSOR_DELAY_FASTEST) {
                if (PedometerService.isListenAccelerometer() == true) {
                    mSensorManager.registerListener(PedometerAccelerometer.this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
                }
            }
        }*/
        Log.d(TAG, "onAccuracyChanged(), sensor:" + String.valueOf(sensor) + " accuracy: " + String.valueOf(accuracy));
    }
}
