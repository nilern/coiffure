package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Var;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class GlobalDef extends Expr {
    private final Var var;
    @Node.Child
    private Expr init;
    
    public static Expr create(Var var, Expr init) { return new GlobalDef(var, init); }

    private GlobalDef(Var var, Expr init) {
        this.var = var;
        this.init = init;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        var.bindRoot(init.execute(frame));
        return null;
    }
}
