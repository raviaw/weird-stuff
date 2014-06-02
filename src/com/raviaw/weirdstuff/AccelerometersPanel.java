//
// Copyright 2014 Emerald Associates, Inc.  All Rights Reserved.
//
// Use of this file other than by Emerald Associates, Inc. is forbidden
// unless otherwise authorized by a separate written license agreement.
//
// $Id$
//
package com.raviaw.weirdstuff;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.util.AttributeSet;
import android.util.Log;

import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:raviw@emerald-associates.com">Ravi Wallau</a>
 */
public class AccelerometersPanel extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener
{
    public static final int TRAIL = 30;
    private String tag = getClass().getSimpleName();

    private DrawThread drawThread = null;
    private SurfaceHolder sf;
    private Sensor accelerometer;
    private final AtomicBoolean running = new AtomicBoolean( false );
    private final AtomicInteger circle = new AtomicInteger( 0 );
    private List<Dot> dots = new ArrayList<Dot>();
    int counter = 0;

    final Paint thinGreen = buildPaint( Color.rgb( 0, 127, 0 ), 2.0F );
    final Paint thinBlack = buildPaint( Color.BLACK, 0.0F );
    final Paint thinWhite = buildPaint( Color.WHITE, 1.0F );
    final Paint thinYellow = buildPaint( Color.YELLOW, 1.0F );
    final Paint thinRed = buildPaint( Color.rgb( 127, 0, 0 ), 1.0F );

    float x = 0F;
    float y = 0F;
    float z = 0F;

    int changeCounter = 0;

    private final static Random RANDOM = new Random( System.currentTimeMillis() );

    private UseEventSource useEventSource = UseEventSource.ACCELEROMETER;
    private boolean backToOrigin = false;

    public AccelerometersPanel( final Context context )
    {
        super( context );
        initialize();
    }

    public AccelerometersPanel( final Context context, final AttributeSet attrs )
    {
        super( context, attrs );
        initialize();
    }

    public AccelerometersPanel( final Context context, final AttributeSet attrs, final int defStyle )
    {
        super( context, attrs, defStyle );
        initialize();
    }

    private void initialize()
    {
        Log.i( tag, "Initializing!" );
        sf = AccelerometersPanel.this.getHolder();
        //noinspection ConstantConditions
        getHolder().addCallback( this );
        dots.clear();

        final Random r = new Random( System.currentTimeMillis() );
        final float interval = 0.20F;
        for( float f1 = 0.10F; f1 < 1.0F; f1 += interval ) {
            for( float f2 = 0.10F; f2 < 1.0F; f2 += interval ) {
                final float fl1 = 0.04F - 0.08F * ( 1000F / r.nextInt( 1000 ) );
                final float fl2 = 0.04F - 0.08F * ( 1000F / r.nextInt( 1000 ) );
                dots.add( new Dot( f1, f2, fl1, fl2, 0.0F, 0.0F ) );
            }
        }

        Log.d( tag, "Dots: " + dots.size() );
    }

    @Override
    protected void onMeasure( final int widthMeasureSpec, final int heightMeasureSpec )
    {
        super.onMeasure( widthMeasureSpec, heightMeasureSpec );
    }

    private void drawIt( final Canvas canvas )
    {
        if( canvas == null ) {
            return; // Nothing to do!
        }

        Log.v( tag, "drawing it!" );

        canvas.drawColor( 0, PorterDuff.Mode.CLEAR );

        //drawLocationCue( canvas );
        drawTheDamnLines( canvas );
    }

    private void drawTheDamnLines( final Canvas canvas )
    {
        final EvictingQueue<Float> xBezier = EvictingQueue.create( 3 );
        final EvictingQueue<Float> yBezier = EvictingQueue.create( 3 );

        //final Dot firstDot = dots.get( 0 );
        //path.moveTo( x( firstDot.x ), y( firstDot.y ) );
        for( final Dot dot : dots ) {
            final Path path = new Path();
            final int size = Math.min( dot.xQueue.size(), dot.yQueue.size() );
            final Iterator<Float> xIter = dot.xQueue.iterator();
            final Iterator<Float> yIter = dot.yQueue.iterator();

            float currentX = 0.0F;
            float currentY = 0.0F;

            for( int i = 0; i < size; i++ ) {
                currentX = xIter.next();
                currentY = yIter.next();
                xBezier.add( currentX );
                yBezier.add( currentY );
                if( i >= 3 ) {
                    final Iterator<Float> xBezierIter = xBezier.iterator();
                    final Float x1 = xBezierIter.next();
                    final Float x2 = xBezierIter.next();
                    final Float x3 = xBezierIter.next();
                    final Iterator<Float> yBezierIter = yBezier.iterator();
                    final Float y1 = yBezierIter.next();
                    final Float y2 = yBezierIter.next();
                    final Float y3 = yBezierIter.next();
                    path.moveTo( x( x1 ), y( y1 ) );
                    path.quadTo( x( x2 ), y( y2 ), x( x3 ), y( y3 ) );
                    path.moveTo( x( x3 ), y( y3 ) );
                }
            }
            if( !path.isEmpty() ) {
                canvas.drawPath( path, dot.p );
            }

            if( size > 0 ) {
                canvas.drawCircle( x( currentX ), y( currentY ), 4.0F, dot.p );
            }
            dot.progressDot();
        }
    }

/*    private void drawLocationCue( final Canvas canvas )
    {
        canvas.drawLine( x( 0.0F ), y( y ), x( 1.0F ), y( y ), thinGreen );
        canvas.drawLine( x( x ), y( 0.0F ), x( x ), y( 1.0F ), thinGreen );

        float width = circle.getAndAdd( -10 );
        if( width < 0 ) {
            circle.set( 100 );
            width = 0;
        }

        canvas.drawCircle( x( x ), y( y ), width, thinRed );
    }
*/
    private float absoluteMin( final float value, final float absoluteMinSpeed )
    {
        if( Math.abs( value ) < absoluteMinSpeed ) {
            if( value < 0 ) {
                return absoluteMinSpeed * -1F;
            } else {
                return absoluteMinSpeed;
            }
        } else {
            return value;
        }
    }

    private float absoluteMax( final float value, final float absoluteMaxSpeed )
    {
        if( Math.abs( value ) > absoluteMaxSpeed ) {
            if( value < 0 ) {
                return absoluteMaxSpeed * -1F;
            } else {
                return absoluteMaxSpeed;
            }
        } else {
            return value;
        }
    }

/*    private float absoluteRange( final float value, final float absoluteMinSpeed, final float absoluteMaxSpeed )
    {
        if( Math.abs( value ) < absoluteMinSpeed ) {
            if( value < 0 ) {
                return absoluteMinSpeed * -1F;
            } else {
                return absoluteMinSpeed;
            }
        } else if( Math.abs( value ) > absoluteMaxSpeed ) {
            if( value < 0 ) {
                return absoluteMaxSpeed * -1F;
            } else {
                return absoluteMaxSpeed;
            }
        } else {
            return value;
        }
    }
*/
    private static Paint buildPaint( final int color, final float width )
    {
        final Paint paint = new Paint();
        paint.setColor( color );
        paint.setStyle( Paint.Style.STROKE );
        paint.setStrokeWidth( width );
        return paint;
    }

    private float x( final float value )
    {
        return proportion( 1.0F - value, 0, 1, 0, getWidth() - ( getPaddingLeft() + getPaddingRight() ) );
    }

    private float y( final float value )
    {
        return proportion( value, 0, 1, 0, getHeight() - ( getPaddingTop() + getPaddingBottom() ) );
    }

    private float unscaleGravity( final float sensorValue )
    {
        return proportion( sensorValue, -10F, 10F, 0.0F, 1.0F );
    }

    private float unscaleWidth( final float value )
    {
        int[] location = new int[ 2 ];
        getLocationOnScreen( location );
        return proportion( value - location[ 0 ], 0, getWidth(), 0.0F, 1.0F );
    }

    private float unscaleHeight( final float value )
    {
        int[] location = new int[ 2 ];
        getLocationOnScreen( location );
        return proportion( value - location[ 1 ], 0, getHeight(), 0.0F, 1.0F );
    }

    private float proportion( final float value, final float min, final float max, final float rangeMin, final float rangeMax )
    {
        if( value < min ) {
            return rangeMin;
        } else if( value > max ) {
            return rangeMax;
        } else {
            final float applied = ( value - min ) / ( max - min );
            return ( rangeMax - rangeMin ) * applied + rangeMin;
        }
    }

    @Override
    public void surfaceCreated( final SurfaceHolder holder )
    {
        running.set( true );
        startThread();
    }

    @Override
    public void surfaceChanged( final SurfaceHolder holder, final int format, final int width, final int height )
    {

    }

    @Override
    public void surfaceDestroyed( final SurfaceHolder holder )
    {
        running.set( false );
        stopThread();
    }

    private void startThread()
    {
        if( drawThread != null ) {
            drawThread.interrupt();
        }
        drawThread = new DrawThread();
        drawThread.start();
    }

    private void stopThread()
    {
        if( drawThread != null ) {
            drawThread.interrupt();
            drawThread = null;
        }
    }

    public void setAccelerometer( final Sensor accelerometer )
    {
        this.accelerometer = accelerometer;
    }

    public Sensor getAccelerometer()
    {
        return accelerometer;
    }

    @Override
    public void onSensorChanged( final SensorEvent event )
    {
        if( useEventSource == UseEventSource.ACCELEROMETER ) {
            if( !backToOrigin ) {
                x = unscaleGravity( event.values[ 0 ] );
                y = unscaleGravity( event.values[ 1 ] );
            }
            changeCounter++;
            if( changeCounter > 100 ) {
                backToOrigin = !backToOrigin;
                changeCounter = 0;
            }
        }
        z = unscaleGravity( event.values[ 2 ] );
    }

    public boolean touchEvent( final MotionEvent event )
    {
        switch( event.getAction() & MotionEvent.ACTION_MASK ) {
            case MotionEvent.ACTION_UP:
                useEventSource = UseEventSource.ACCELEROMETER;
                backToOrigin = false;
                return true;
            case MotionEvent.ACTION_DOWN:
                backToOrigin = false;
                useEventSource = UseEventSource.TOUCH;
                x = 1.0F - unscaleWidth( event.getX() );
                y = unscaleHeight( event.getY() );
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                backToOrigin = true;
                return true;
            case MotionEvent.ACTION_POINTER_UP: // WRONG but it is okay
                backToOrigin = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                useEventSource = UseEventSource.TOUCH;
                x = 1.0F - unscaleWidth( event.getX() );
                y = unscaleHeight( event.getY() );
                return true;
        }

        return false;
    }

    @Override
    public void onAccuracyChanged( final Sensor sensor, final int accuracy )
    {

    }

    private class DrawThread extends Thread
    {
        @Override
        public void run()
        {
            Log.i( tag, "NEW THREAD!" );
            Canvas c;
            while( running.get() && !isInterrupted() ) {
                c = null;
                try {
                    c = sf.lockCanvas();
                    synchronized( sf ) {
                        drawIt( c );
                        Thread.sleep( 25 );
                    }
                } catch( final InterruptedException e ) {
                    System.out.println( "I was interrupted!" );
                    break;
                } finally {
                    if( c != null ) {
                        sf.unlockCanvasAndPost( c );
                    }
                }
            }
        }
    }

    private class Dot
    {
        private final float ox;
        private final float oy;
        private float x;
        private float y;
        private float xSpeed;
        private float ySpeed;
        private float xAcceleration;
        private float yAcceleration;
        private Queue<Float> xQueue = EvictingQueue.create( TRAIL );
        private Queue<Float> yQueue = EvictingQueue.create( TRAIL );
        private final Paint p =
            buildPaint(
                Color.rgb( 100 + RANDOM.nextInt( 155 ), 100 + RANDOM.nextInt( 155 ), 100 + RANDOM.nextInt( 155 ) ),
                1.0F
            );

        private Dot(
            final float x,
            final float y,
            final float xSpeed,
            final float ySpeed,
            final float xAcceleration,
            final float yAcceleration
        )
        {
            this.ox = x;
            this.oy = y;
            setX( x );
            setY( y );
            this.xSpeed = xSpeed;
            this.ySpeed = ySpeed;
            this.xAcceleration = xAcceleration;
            this.yAcceleration = yAcceleration;
        }

        private void progressDot()
        {
            final float toX;
            final float toY;
            if( backToOrigin ) {
                toX = ox;
                toY = oy;
            } else {
                toX = AccelerometersPanel.this.x;
                toY = AccelerometersPanel.this.y;
            }
            final float xd = toX - this.x;
            final float yd = toY - this.y;
            final float xf = absoluteMin( xd * 0.05F, 0.02F );
            final float yf = absoluteMin( yd * 0.05F, 0.02F );
            this.xSpeed = absoluteMax( this.xSpeed + xf, 0.04F );
            this.ySpeed = absoluteMax( this.ySpeed + yf, 0.04F );
            this.setX( this.x + this.xSpeed );
            this.setY( this.y + this.ySpeed );

            Log.v( tag, "xd: " + xd + ", yd: " + yd + ", xfactor: " + xf + ", yfactor: " + yf + ", dot: " + this );
        }

        private void setX( final float x )
        {
            this.x = x;
            xQueue.add( x );
        }

        private void setY( final float y )
        {
            this.y = y;
            yQueue.add( y );
        }

        @Override
        public String toString()
        {
            return
                "Dot{" +
                "x=" + x +
                ", y=" + y +
                ", xSpeed=" + xSpeed +
                ", ySpeed=" + ySpeed +
                ", xAcceleration=" + xAcceleration +
                ", yAcceleration=" + yAcceleration +
                '}';
        }
    }

    private enum UseEventSource
    {
        ACCELEROMETER, TOUCH
    }
}
