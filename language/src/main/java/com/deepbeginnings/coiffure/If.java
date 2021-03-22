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

    public If(Expr cond, Expr conseq, Expr alt) {
        this.cond = cond;
        this.conseq = conseq;
        this.alt = alt;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object condv = cond.execute(frame);

        if (condv != null && condv != Boolean.FALSE) {
            return conseq.execute(frame);
        } else  {
            return alt.execute(frame);
        }
    }
}
