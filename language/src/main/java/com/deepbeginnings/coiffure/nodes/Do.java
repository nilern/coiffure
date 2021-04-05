package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.BlockNode;
import com.oracle.truffle.api.nodes.Node;

public final class Do extends Expr implements BlockNode.ElementExecutor<Expr> {
    @Node.Child
    private BlockNode<Expr> block;

    public static Expr create(Expr[] stmts) {
        switch (stmts.length) {
        case 0: return new Const(null);
        case 1: return stmts[0];
        default: return new Do(stmts);
        }
    }

    private Do(Expr[] stmts) { this.block = BlockNode.create(stmts, this); }

    @Override
    public void executeVoid(VirtualFrame frame, Expr stmt, int index, int arg) { stmt.execute(frame); }

    @Override
    public Object executeGeneric(VirtualFrame frame, Expr expr, int index, int arg) { return expr.execute(frame); }

    @Override
    public Object execute(VirtualFrame frame) { return block.executeGeneric(frame, 0); }
}
