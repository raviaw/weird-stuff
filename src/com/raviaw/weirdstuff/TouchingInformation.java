//
// Copyright 2014 Emerald Associates, Inc.  All Rights Reserved.
//
// Use of this file other than by Emerald Associates, Inc. is forbidden
// unless otherwise authorized by a separate written license agreement.
//
// $Id$
//
package com.raviaw.weirdstuff;

import java.util.List;

/**
* @author <a href="mailto:raviw@emerald-associates.com">Ravi Wallau</a>
*/
class TouchingInformation
{
    private final int touchingFingerCount;
    private final List<Dot> dots;

    TouchingInformation( final int touchingFingerCount, final List<Dot> dots )
    {
        this.touchingFingerCount = touchingFingerCount;
        this.dots = dots;
    }

    public int getTouchingFingerCount()
    {
        return touchingFingerCount;
    }

    public List<Dot> getDots()
    {
        return dots;
    }
}
