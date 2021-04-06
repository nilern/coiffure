package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;

public final class Loop extends Expr {
    @Child private LoopNode loopNode;

    public Loop(final Expr body) { this.loopNode = Truffle.getRuntime().createLoopNode(new LoopBody(body)); }

    @Override
    public Object execute(final VirtualFrame frame) { return loopNode.execute(frame); }
}
