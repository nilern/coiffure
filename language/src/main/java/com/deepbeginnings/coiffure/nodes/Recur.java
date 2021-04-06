package com.deepbeginnings.coiffure.nodes;

import com.deepbeginnings.coiffure.RecurException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class Recur extends Expr {
    @Children private Expr[] argDefs;

    public Recur(final Expr[] argDefs) { this.argDefs = argDefs; }

    @ExplodeLoop
    @Override
    public Object execute(final VirtualFrame frame) {
        for (final Expr argDef : argDefs) { argDef.execute(frame); }
        throw RecurException.INSTANCE;
    }
}
