package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Var;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class GlobalSet extends Expr {
    private final Var var;
    @Child private Expr init;

    public GlobalSet(final Var var, final Expr init) {
        this.var = var;
        this.init = init;
    }

    @Override
    public Object execute(final VirtualFrame frame) { return var.set(init.execute(frame)); }
}
