package com.deepbeginnings.coiffure;

import clojure.lang.Var;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class Def extends Expr {
    private final Var var;
    @Node.Child
    private Expr init;
    
    public static Expr create(Var var, Expr init) { return new Def(var, init); }

    private Def(Var var, Expr init) {
        this.var = var;
        this.init = init;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        var.bindRoot(init.execute(frame));
        return null;
    }
}
