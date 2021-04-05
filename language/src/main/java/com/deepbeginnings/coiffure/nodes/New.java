package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Reflector;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

public final class New extends Expr {
    private final Class<?> klass;
    @Node.Children private Expr[] args;
    
    public static Expr create(Class<?> klass, Expr[] args) { return new New(klass, args); }
    
    private New(Class<?> klass, Expr[] args) {
        this.klass = klass;
        this.args = args;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        final Object[] argVals = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            argVals[i] = args[i].execute(frame);
        }

        return Reflector.invokeConstructor(klass, argVals);
    }
}
