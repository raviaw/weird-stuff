//
// Copyright 2014 Emerald Associates, Inc.  All Rights Reserved.
//
// Use of this file other than by Emerald Associates, Inc. is forbidden
// unless otherwise authorized by a separate written license agreement.
//
// $Id$
//
package com.raviaw.weirdstuff;

/**
* @author <a href="mailto:raviw@emerald-associates.com">Ravi Wallau</a>
*/
class Dot
{
    public final float x;
    public final float y;

    Dot( final float x, final float y )
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString()
    {
        return "Dot{" +
            "x=" + x +
            ", y=" + y +
            '}';
    }
}
