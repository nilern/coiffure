package com.deepbeginnings.coiffure;

import clojure.lang.ArraySeq;
import clojure.lang.IFn;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class CallNode extends Expr {
    @Child private Expr callee;
    @Children private Expr[] args;

    public static Expr create(final Expr callee, final Expr[] args) { return new CallNode(callee, args); }

    private CallNode(final Expr callee, final Expr[] args) {
        this.callee = callee;
        this.args = args;
    }

    @ExplodeLoop
    @Override
    public Object execute(final VirtualFrame frame) {
        final IFn calleeVal = (IFn) callee.execute(frame);

        final Object[] argVals = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            argVals[i] = args[i].execute(frame);
        }

        return calleeVal.applyTo(ArraySeq.create(argVals));
    }
}
