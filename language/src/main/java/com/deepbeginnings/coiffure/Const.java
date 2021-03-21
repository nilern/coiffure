package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;

final class Const extends Expr {
    private final Object value;

    public Const(Object value) { this.value = value; }

    @Override
    public Object eval(VirtualFrame frame) { return this.value; }
}
