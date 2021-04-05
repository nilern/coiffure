package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Var;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class GlobalDef extends Expr {
    private final Var var;
    @Child private Expr init;
    
    public static Expr create(final Var var, final Expr init) { return new GlobalDef(var, init); }

    private GlobalDef(final Var var, final Expr init) {
        this.var = var;
        this.init = init;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        var.bindRoot(init.execute(frame));
        return null;
    }
}
