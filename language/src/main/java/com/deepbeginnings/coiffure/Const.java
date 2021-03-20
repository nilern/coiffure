package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;

class Const extends Expr {
    private final Object value;

    Const(Object value) { this.value = value; }

    public static Expr create(Object value) { return new Const(value); }

    @Override
    public Object eval(VirtualFrame frame) { return this.value; }
}
