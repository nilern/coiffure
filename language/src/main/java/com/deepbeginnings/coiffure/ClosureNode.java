package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

final class ClosureNode extends Expr {
    @Children private MethodNode[] methods;
    @Children private Expr[] closings;

    public ClosureNode(final MethodNode[] methods, final Expr[] closings) {
        this.methods = methods;
        this.closings = closings;
    }

    @ExplodeLoop
    @Override
    public Object execute(final VirtualFrame frame) {
        final Object[] clovers = new Object[closings.length];

        for (int i = 0; i < closings.length; ++i) {
            clovers[i] = closings[i].execute(frame);
        }

        return new Closure(methods, clovers);
    }
}
