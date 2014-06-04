//
// Copyright 2014 Emerald Associates, Inc.  All Rights Reserved.
//
// Use of this file other than by Emerald Associates, Inc. is forbidden
// unless otherwise authorized by a separate written license agreement.
//
// $Id$
//
package com.raviaw.weirdstuff;

import android.util.Log;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:raviw@emerald-associates.com">Ravi Wallau</a>
 */
public class MultiIndexRotatingArray<T>
{
    private final String tag = getClass().getSimpleName();

    private final int capacity;
    private final AtomicInteger writePointer = new AtomicInteger();
    private final AtomicInteger rotatablePointer1 = new AtomicInteger( 0 );
    private final AtomicInteger rotatablePointer2 = new AtomicInteger( 0 );
    private final AtomicInteger resettablePointer = new AtomicInteger( 0 );
    private final AtomicInteger maxIndex = new AtomicInteger();

    private final List<T> list;
    private final T firstFill;

    public MultiIndexRotatingArray( final int capacity, final T fillWith )
    {
        this.capacity = capacity;
        this.firstFill = fillWith;
        this.list = Lists.newArrayList( Collections.<T>nCopies( capacity, fillWith ) );
    }

    public void add( final T element )
    {
        list.set( nextFromWriteCounter( writePointer ), element );
    }

    public void resetEverything()
    {
        rotatablePointer1.set( 0 );
        rotatablePointer2.set( 0 );
        resettablePointer.set( 0 );
        maxIndex.set( 0 );
        writePointer.set( 0 );

/*
        for( int i = 0; i < list.size(); i++ ) {
            list.set( i, firstFill );
        }
*/
    }

    public T sameList1()
    {
        return list.get( rotatablePointer1.get() );
    }

    public T nextList1()
    {
        return list.get( nextFromReadCounter( rotatablePointer1 ) );
    }

    public T nextList2()
    {
        return list.get( nextFromReadCounter( rotatablePointer2 ) );
    }

    public void resetPointer()
    {
        resettablePointer.set( 0 );
    }

    public int capacity()
    {
        return capacity;
    }

    public T nextResettablePointer()
    {
        final int next = resettablePointer.getAndIncrement();
        if( next < maxIndex.get() ) {
            return list.get( next );
        } else {
            return null;
        }
    }

    private int nextFromReadCounter( final AtomicInteger counter )
    {
        final int position = counter.getAndIncrement();
        if( position < maxIndex.get() ) {
            Log.v( tag, "Returning position " + position );
            return position;
        } else {
            counter.set( 0 );
            return 0;
        }
    }

    private int nextFromWriteCounter( final AtomicInteger counter )
    {
        final int position = counter.getAndIncrement();
        if( position < capacity ) {
            if( maxIndex.get() < position ) {
                maxIndex.set( position );
            }
            return position;
        } else {
            counter.set( 0 );
            return 0;
        }
    }

    public int list1Position()
    {
        return rotatablePointer1.get();
    }
}
