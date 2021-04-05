package com.deepbeginnings.coiffure.nodes;

import com.deepbeginnings.coiffure.nodes.Expr;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "slot", type = FrameSlot.class)
public abstract class LocalUse extends Expr {
    protected abstract FrameSlot getSlot();

    @Specialization
    protected Object readObject(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, getSlot());
    }
}
