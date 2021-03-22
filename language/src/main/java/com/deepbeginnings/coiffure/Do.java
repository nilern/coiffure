package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

final class Do extends Expr {
    @Node.Children
    Expr[] stmts;
    @Node.Child
    Expr expr;
    
    public Do(Expr[] stmts, Expr expr) {
        this.stmts = stmts;
        this.expr = expr;
    }
    
    @Override
    public Object execute(VirtualFrame frame) {
        for (Expr stmt : stmts) { stmt.execute(frame); }
        return expr.execute(frame);
    }
}
