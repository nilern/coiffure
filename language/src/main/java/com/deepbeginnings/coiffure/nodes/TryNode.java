package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class TryNode extends Expr {
    @Child private Expr body;
    @Children private CatchNode[] catches;
    @Child private Expr finallyExpr;

    public static Expr create(final Expr body, final CatchNode[] catches, final Expr finallyExpr) {
        if (catches.length == 0 && finallyExpr == null) {
            return body;
        } else {
            return new TryNode(body, catches, finallyExpr);
        }
    }

    private TryNode(final Expr body, final CatchNode[] catches, final Expr finallyExpr) {
        this.body = body;
        this.catches = catches;
        this.finallyExpr = finallyExpr;
    }

    @ExplodeLoop
    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (final Throwable t) { // NOTE: Can't recur across try so RecurException requires no special treatment.
            for (final CatchNode catchNode : catches) {
                if (catchNode.matches(t)) {
                    return catchNode.execute(frame, t);
                }
            }
            
            throw t; // No matching catches, so rethrow
        } finally {
            if (finallyExpr != null) {
                finallyExpr.execute(frame);
            }
        }
    }
}
