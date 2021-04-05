package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class Const extends Expr {
    private final Object value;

    public Const(Object value) { this.value = value; }

    @Override
    public Object execute(VirtualFrame frame) { return this.value; }
}
