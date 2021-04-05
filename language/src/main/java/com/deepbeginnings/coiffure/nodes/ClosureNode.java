package com.deepbeginnings.coiffure.nodes;

import com.deepbeginnings.coiffure.Closure;
import com.deepbeginnings.coiffure.RestClosure;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class ClosureNode extends Expr {
    @Children private MethodNode[] methods;
    private final MethodNode variadicMethod;
    @Children private Expr[] closings;

    public ClosureNode(final MethodNode[] methods, final MethodNode variadicMethod, final Expr[] closings) {
        this.methods = methods;
        this.variadicMethod = variadicMethod;
        this.closings = closings;
    }

    @ExplodeLoop
    @Override
    public Object execute(final VirtualFrame frame) {
        final Object[] clovers = new Object[closings.length];

        for (int i = 0; i < closings.length; ++i) {
            clovers[i] = closings[i].execute(frame);
        }

        return (variadicMethod != null)
                ? new RestClosure(methods, variadicMethod, clovers)
                : new Closure(methods, clovers);
    }
}
