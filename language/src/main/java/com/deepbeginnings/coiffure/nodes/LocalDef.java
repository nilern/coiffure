package com.deepbeginnings.coiffure.nodes;

import com.deepbeginnings.coiffure.nodes.Expr;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild(value = "expr", type = Expr.class)
public abstract class LocalDef extends Expr {
    protected abstract FrameSlot getSlot();

    @Specialization
    protected Object write(final VirtualFrame frame, final Object value) {
        frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);

        frame.setObject(getSlot(), value);
        return value;
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);
}
