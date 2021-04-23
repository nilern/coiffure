package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Util;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ThrowNode extends Expr {
    @Child private Expr exn;
    
    public ThrowNode(final Expr exn) { this.exn = exn; }

    @Override
    public Object execute(final VirtualFrame frame) { return Util.sneakyThrow((Throwable) exn.execute(frame)); }
}
