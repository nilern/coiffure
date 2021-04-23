package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class CatchNode extends Node {
    private final Class<? extends Throwable> catcheeClass;
    private final FrameSlot catcheeSlot;
    @Child private Expr body;

    public CatchNode(final Class<? extends Throwable> catcheeClass, final FrameSlot catcheeSlot, final Expr body) {
        this.catcheeClass = catcheeClass;
        this.catcheeSlot = catcheeSlot;
        this.body = body;
    }

    boolean matches(final Throwable t) { return catcheeClass.isInstance(t); }

    Object execute(final VirtualFrame frame, final Throwable t) {
        frame.setObject(catcheeSlot, t);
        return body.execute(frame);
    }
}
