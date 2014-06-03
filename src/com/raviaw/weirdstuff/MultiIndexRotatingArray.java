//
// Copyright 2014 Emerald Associates, Inc.  All Rights Reserved.
//
// Use of this file other than by Emerald Associates, Inc. is forbidden
// unless otherwise authorized by a separate written license agreement.
//
// $Id$
//
package com.raviaw.weirdstuff;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:raviw@emerald-associates.com">Ravi Wallau</a>
 */
public class MultiIndexRotatingArray<T>
{
    private final int capacity;
    private final AtomicInteger writePointer = new AtomicInteger();
    private final AtomicInteger rotatablePointer1 = new AtomicInteger( 0 );
    private final AtomicInteger rotatablePointer2 = new AtomicInteger( 0 );
    private final AtomicInteger resettablePointer = new AtomicInteger( 0 );

    private final List<T> list;

    public MultiIndexRotatingArray( final int capacity, final T fillWith )
    {
        this.capacity = capacity;
        list = Lists.newArrayList( Collections.<T>nCopies( capacity, fillWith ) );
    }

    public void add( final T element )
    {
        list.set( nextFromCounter( writePointer ), element );
    }

    private int nextFromCounter( final AtomicInteger counter )
    {
        final int position = counter.getAndIncrement();
        if( position < capacity ) {
            return position;
        } else {
            counter.set( 0 );
            return 0;
        }
    }

    public T nextList1()
    {
        return list.get( nextFromCounter( rotatablePointer1 ) );
    }

    public T nextList2()
    {
        return list.get( nextFromCounter( rotatablePointer2 ) );
    }

    public void resetPointer()
    {
        resettablePointer.set( 0 );
    }

    public T nextResettablePointer()
    {
        final int next = resettablePointer.getAndIncrement();
        if( next < capacity ) {
            return list.get( next );
        } else {
            return null;
        }
    }

    public int capacity()
    {
        return capacity;
    }
}
