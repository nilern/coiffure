package com.deepbeginnings.coiffure;

import clojure.lang.RestFn;
import clojure.lang.Util;

import com.oracle.truffle.api.Truffle;

public class RestClosure extends RestFn implements IClosure {
    private final MethodNode[] methods;
    private final MethodNode variadicMethod;
    private final Object[] clovers; // OPTIMIZE: RestClosure extends DynamicObject?

    public RestClosure(final MethodNode[] methods, final MethodNode variadicMethod, final Object[] clovers) {
        this.methods = methods;
        this.variadicMethod = variadicMethod;
        this.clovers = clovers;
    }

    @Override
    public Object clover(final int index) { return clovers[index]; }

    @Override
    public int getRequiredArity() { return variadicMethod.minArity; }

    @Override
    public Object doInvoke(final Object args) {
        final int argc = 0;

        final MethodNode method = methods[argc];
        if (method != null) {
            if (method != variadicMethod) {
                return Truffle.getRuntime().createCallTarget(method)
                        .call(this);
            } else {
                return Truffle.getRuntime().createCallTarget(variadicMethod)
                        .call(this, args);
            }
        } else {
            return throwArity(argc);
        }
    }

    @Override
    public Object doInvoke(Object arg, final Object args) {
        final int argc = 1;

        final MethodNode method = methods[argc];
        if (method != null) {
            if (method != variadicMethod) {
                return Truffle.getRuntime().createCallTarget(method)
                        .call(this, Util.ret1(arg, arg = null));
            } else {
                return Truffle.getRuntime().createCallTarget(variadicMethod)
                        .call(this, Util.ret1(arg, arg = null), args);
            }
        } else {
            return throwArity(argc);
        }
    }
}
