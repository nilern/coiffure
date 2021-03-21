package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class If extends Expr {
    @Node.Child
    private Expr cond;
    @Node.Child
    private Expr conseq;
    @Node.Child
    private Expr alt;

    public If(Expr cond, Expr conseq) { this(cond, conseq, null); }

    public If(Expr cond, Expr conseq, Expr alt) {
        this.cond = cond;
        this.conseq = conseq;
        this.alt = alt;
    }

    @Override
    public Object eval(VirtualFrame frame) {
        Object condv = cond.eval(frame);
        if (condv != null && condv != Boolean.FALSE) {
            return conseq.eval(frame);
        } else if (alt != null) {
            return alt.eval(frame);
        } else {
            return null;
        }
    }
}
