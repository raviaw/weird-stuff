package com.raviaw.weirdstuff;

import android.app.Activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.os.Bundle;

import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

/**
 */
public class MovingLines extends Activity implements SensorEventListener
{
    private AccelerometersPanel panel;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_NO_TITLE );
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );

        setContentView( R.layout.activity_moving_lines );

        panel = ( AccelerometersPanel )findViewById( R.id.view );
    }

    @Override
    public boolean onTouchEvent( final MotionEvent event )
    {
        return panel.touchEvent( event );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public void onSensorChanged( final SensorEvent event )
    {
        panel.invalidate();
    }

    @Override
    public void onAccuracyChanged( final Sensor sensor, final int accuracy )
    {

    }
}
