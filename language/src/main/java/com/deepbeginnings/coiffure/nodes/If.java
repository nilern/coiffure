package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class If extends Expr {
    @Child private Expr cond;
    @Child private Expr conseq;
    @Child private Expr alt;

    public If(final Expr cond, final Expr conseq, final Expr alt) {
        this.cond = cond;
        this.conseq = conseq;
        this.alt = alt;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        final Object condv = cond.execute(frame);

        if (condv != null && condv != Boolean.FALSE) {
            return conseq.execute(frame);
        } else  {
            return alt.execute(frame);
        }
    }
}
