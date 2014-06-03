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

import android.os.SystemClock;

import android.util.AttributeSet;
import android.util.Log;

import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:raviw@emerald-associates.com">Ravi Wallau</a>
 */
public class AccelerometersPanel extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener
{
    public static final int TRAIL_SIZE = 30;
    private String tag = getClass().getSimpleName();

    private DrawThread drawThread = null;
    private SurfaceHolder sf;
    private Sensor accelerometer;
    private final AtomicBoolean running = new AtomicBoolean( false );
    private final List<MovingDot> dots = new ArrayList<MovingDot>();

    private final AtomicInteger touchingFingers = new AtomicInteger();
    private final MultiIndexRotatingArray<TouchingInformation> userTouchingTrail =
        new MultiIndexRotatingArray<TouchingInformation>( 200, null );
    private final Paint thinWhite = buildPaint( Color.WHITE, 2.0F );
    private final Paint textPaint = buildTextPaint( Color.WHITE, 2.0F );

    private final long TOUCH_SAMPLE_RATE_MS = 20;

    private final AtomicLong lastFrameRendered = new AtomicLong( SystemClock.elapsedRealtime() );
    private final AtomicLong lastSample = new AtomicLong( 0 );

/*
    float x = 0F;
    float y = 0F;
    float z = 0F;
*/

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
                dots.add( new MovingDot( f1, f2, fl1, fl2, 0.0F, 0.0F ) );
            }
        }

        Log.d( tag, "Dots: " + dots.size() );

        fillCoordinatesWithCircle( userTouchingTrail.capacity() );
    }

    private void fillCoordinatesWithCircle( final int count )
    {
        for( float angle = 0; angle <= Math.PI * 2.0; angle += ( ( Math.PI * 2.0 ) / ( ( double )count / 2.0 ) ) ) {
            final float x = proportion( ( float )Math.sin( angle ), -1.0F, 1F, 0F, 1F );
            final float y = proportion( ( float )Math.cos( angle ), -1.0F, 1F, 0F, 1F );
            userTouchingTrail.add( new TouchingInformation( 1, Collections.singletonList( new Dot( x, y ) ) ) );
        }
        for( float angle = 0; angle <= Math.PI * 2.0; angle += ( ( Math.PI * 2.0 ) / ( ( double )count / 2.0 ) ) ) {
            final float x = proportion( ( float )Math.sin( angle ), -1.0F, 1F, 0.25F, 0.75F );
            final float y = proportion( ( float )Math.cos( angle ), -1.0F, 1F, 0.25F, 0.75F );
            userTouchingTrail.add( new TouchingInformation( 1, Collections.singletonList( new Dot( x, y ) ) ) );
        }
    }

    @Override
    protected void onMeasure( final int widthMeasureSpec, final int heightMeasureSpec )
    {
        super.onMeasure( widthMeasureSpec, heightMeasureSpec );
    }

    private void progressLines( final Canvas canvas )
    {
        if( canvas == null ) {
            return; // Nothing to do!
        }

        Log.v( tag, "drawing it!" );

        canvas.drawColor( 0, PorterDuff.Mode.CLEAR );

        //drawLocationCue( canvas );
        //drawUserPositionTrail( canvas );
        drawUserPositionTrail( canvas );
        //nextRenderPoint();
        renderLines( canvas );
        recordPerformanceInformation( canvas );
    }

    private void recordPerformanceInformation( final Canvas canvas )
    {
        final long now = SystemClock.elapsedRealtime();
        final long before = lastFrameRendered.getAndSet( now );
        final long time = now - before;
        //Log.v( tag, "Last frame rendered in: " + time );

        canvas.drawText( "FPS: " + ( 1000 / time ), x( 0 ), y( 1 ) - 15F, textPaint );
    }

    private void drawUserPositionTrail( final Canvas canvas )
    {
        userTouchingTrail.resetPointer();
        TouchingInformation ti;
        while( ( ti = userTouchingTrail.nextResettablePointer() ) != null ) {
            for( final Dot dot : ti.getDots() ) {
                final float centerX = x( dot.x );
                final float centerY = y( dot.y );
                canvas.drawLine( centerX, centerY, centerX + 1, centerY + 1, thinWhite );
            }
        }
    }


    private void renderLines( final Canvas canvas )
    {
        // final TouchingInformation touch = userTouchingTrail.poll();
        final TouchingInformation ti = userTouchingTrail.nextList1();
        if( ti == null ) {
            return;
        }
        for( int i = 0; i < dots.size(); i++ ) {
            final MovingDot movingDot = dots.get( i );
            if( ti.getTouchingFingerCount() == 0 ) {
                continue;
            }
            final Dot dotToUse = ti.getDots().get( i % ti.getTouchingFingerCount() );
            movingDot.progressDot( dotToUse );

            final EvictingQueue<Float> xBezier = EvictingQueue.create( 3 );
            final EvictingQueue<Float> yBezier = EvictingQueue.create( 3 );

            final Path path = new Path();
            final int size = Math.min( movingDot.xQueue.size(), movingDot.yQueue.size() );
            final Iterator<Float> xIter = movingDot.xQueue.iterator();
            final Iterator<Float> yIter = movingDot.yQueue.iterator();
            float currentX = 0.0F;
            float currentY = 0.0F;

            for( int j = 0; j < size; j++ ) {
                currentX = xIter.next();
                currentY = yIter.next();
                xBezier.add( currentX );
                yBezier.add( currentY );
                if( j >= 3 ) {
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
                canvas.drawPath( path, movingDot.p );
            }
            if( size > 0 ) {
                canvas.drawCircle( x( currentX ), y( currentY ), 4.0F, movingDot.p );
            }
        }
    }

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

    private static Paint buildPaint( final int color, final float width )
    {
        final Paint paint = new Paint();
        paint.setColor( color );
        paint.setStyle( Paint.Style.STROKE );
        paint.setStrokeWidth( width );
        return paint;
    }

    private static Paint buildTextPaint( final int color, final float width )
    {
        final Paint paint = buildPaint( color, width );
        paint.setTextSize( 15F );
        paint.setTextAlign( Paint.Align.RIGHT );
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
/*
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
*/
    }

    public boolean touchEvent( final MotionEvent event )
    {
        touchingFingers.set( event.getPointerCount() );

        switch( event.getAction() & MotionEvent.ACTION_MASK ) {
            case MotionEvent.ACTION_UP:
                //useEventSource = UseEventSource.ACCELEROMETER;
                //backToOrigin = false;
                return true;
            case MotionEvent.ACTION_DOWN:
                backToOrigin = false;
                useEventSource = UseEventSource.TOUCH;
/*
                x = 1.0F - unscaleWidth( event.getX() );
                y = unscaleHeight( event.getY() );
*/
                queueEventInfo( event );
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                //backToOrigin = true;
                return true;
            case MotionEvent.ACTION_POINTER_UP: // WRONG but it is okay
                //backToOrigin = false;
                return true;
            case MotionEvent.ACTION_MOVE: // How many times a second is this recorded?
                useEventSource = UseEventSource.TOUCH;
                queueEventInfo( event );
                return true;
        }

        return false;
    }

    private void queueEventInfo( final MotionEvent event )
    {
        final long currentTime = System.currentTimeMillis();
        if( currentTime - lastSample.get() > TOUCH_SAMPLE_RATE_MS ) {
            final int pointerCount = event.getPointerCount();
            if( pointerCount > 1 ) {
                final List<Dot> dots = Lists.newArrayList();
                for( int i = 0; i < pointerCount; i++ ) {
                    final MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
                    event.getPointerCoords( i, coords );
                    dots.add( new Dot( 1.0F - unscaleWidth( coords.x ), unscaleHeight( coords.y ) ) );
                }
                userTouchingTrail.add( new TouchingInformation( pointerCount, dots ) );
            } else {
                userTouchingTrail.add(
                    new TouchingInformation(
                        1,
                        Collections.singletonList(
                            new Dot( 1.0F - unscaleWidth( event.getX() ), unscaleHeight( event.getY() ) )
                        )
                    )
                );
            }
            lastSample.set( currentTime );
        }
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
                        progressLines( c );
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

    private class MovingDot
    {
        private final float originalX;
        private final float originalY;
        private float x;
        private float y;
        private float xSpeed;
        private float ySpeed;
        private float xAcceleration;
        private float yAcceleration;
        private Queue<Float> xQueue = EvictingQueue.create( TRAIL_SIZE );
        private Queue<Float> yQueue = EvictingQueue.create( TRAIL_SIZE );
        private final Paint p =
            buildPaint(
                Color.rgb( 100 + RANDOM.nextInt( 155 ), 100 + RANDOM.nextInt( 155 ), 100 + RANDOM.nextInt( 155 ) ),
                1.0F
            );

        private MovingDot(
            final float x,
            final float y,
            final float xSpeed,
            final float ySpeed,
            final float xAcceleration,
            final float yAcceleration
        )
        {
            this.originalX = x;
            this.originalY = y;
            setX( x );
            setY( y );
            this.xSpeed = xSpeed;
            this.ySpeed = ySpeed;
            this.xAcceleration = xAcceleration;
            this.yAcceleration = yAcceleration;
        }

        private void progressDot( final Dot towards )
        {
            final float toX;
            final float toY;
            if( backToOrigin ) {
                toX = originalX;
                toY = originalY;
            } else {
                toX = towards.x;
                toY = towards.y;
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
