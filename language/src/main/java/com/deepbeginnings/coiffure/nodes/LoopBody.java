package com.deepbeginnings.coiffure.nodes;

import com.deepbeginnings.coiffure.RecurException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;

final class LoopBody extends Node implements RepeatingNode {
    @Child private Expr body;

    LoopBody(final Expr body) { this.body = body; }

    @Override
    public Object executeRepeatingWithValue(final VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (final RecurException exn) {
            return CONTINUE_LOOP_STATUS;
        }
    }

    @Override
    public boolean executeRepeating(final VirtualFrame frame) { throw new AssertionError("unreachable"); }
}
