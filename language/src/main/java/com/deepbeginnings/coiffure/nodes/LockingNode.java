package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public class LockingNode extends Expr {
    @Child private Expr lockExpr;
    @Child private Expr body;
    
    public LockingNode(final Expr lockExpr, final Expr body) {
        this.lockExpr = lockExpr;
        this.body = body;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        synchronized (lockExpr.execute(frame)) {
            return body.execute(frame);
        }
    }
}
