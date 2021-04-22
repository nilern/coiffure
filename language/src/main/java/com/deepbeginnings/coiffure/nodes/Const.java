package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class Const extends Expr {
    private final Object value;

    public Const(final Object value) { this.value = value; }
    
    public Object getValue() { return value; }

    @Override
    public Object execute(final VirtualFrame frame) { return this.value; }
}
