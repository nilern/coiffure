package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "slot", type = FrameSlot.class)
@NodeField(name = "index", type = Integer.class)
abstract class CloverUse extends Expr {
    protected abstract FrameSlot getSlot();

    protected abstract int getIndex();

    @Specialization
    protected Object readObject(final VirtualFrame frame) {
        return ((IClosure) FrameUtil.getObjectSafe(frame, getSlot())).clover(getIndex());
    }
}
