package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Var;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "var", type = Var.class)
public abstract class GlobalUse extends Expr {
    protected abstract Var getVar();

    @Specialization
    protected Object readObject(final VirtualFrame frame) {
        final Var var = getVar();
        return var.isDynamic() ? var.get() : var.getRawRoot();
    }
}
