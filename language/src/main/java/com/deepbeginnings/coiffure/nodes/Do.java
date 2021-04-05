package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.BlockNode;

public final class Do extends Expr implements BlockNode.ElementExecutor<Expr> {
    @Child private BlockNode<Expr> block;

    public static Expr create(final Expr[] stmts) {
        switch (stmts.length) {
        case 0: return new Const(null);
        case 1: return stmts[0];
        default: return new Do(stmts);
        }
    }

    private Do(final Expr[] stmts) { this.block = BlockNode.create(stmts, this); }

    @Override
    public void executeVoid(final VirtualFrame frame, final Expr stmt, final int index, final int arg) {
        stmt.execute(frame);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame, final Expr expr, final int index, final int arg) {
        return expr.execute(frame);
    }

    @Override
    public Object execute(final VirtualFrame frame) { return block.executeGeneric(frame, 0); }
}
