package com.deepbeginnings.coiffure;

import clojure.lang.AFunction;
import clojure.lang.Util;

import com.oracle.truffle.api.Truffle;

public class Closure extends AFunction implements IClosure {
    private final MethodNode[] methods;
    private final Object[] clovers; // OPTIMIZE: Closure extends DynamicObject?

    public Closure(final MethodNode[] methods, final Object[] clovers) {
        this.methods = methods;
        this.clovers = clovers;
    }

    @Override
    public Object clover(final int index) { return clovers[index]; }

    @Override
    public Object invoke() {
        final int argc = 0;

        final MethodNode method = methods[argc];
        if (method != null) {
            return Truffle.getRuntime().createCallTarget(method)
                    .call(this);
        } else {
            return throwArity(argc);
        }
    }

    @Override
    public Object invoke(Object arg) {
        final int argc = 1;

        final MethodNode method = methods[argc];
        if (method != null) {
            return Truffle.getRuntime().createCallTarget(method)
                    .call(this, Util.ret1(arg, arg = null));
        } else {
            return throwArity(argc);
        }
    }
}
