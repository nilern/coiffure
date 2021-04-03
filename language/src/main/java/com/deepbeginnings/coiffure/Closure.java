package com.deepbeginnings.coiffure;

import clojure.lang.AFunction;
import clojure.lang.ArraySeq;
import com.oracle.truffle.api.Truffle;

public class Closure extends AFunction {
    private final MethodNode[] methods;
    private final Object[] clovers; // OPTIMIZE: Closure extends DynamicObject?

    public Closure(final MethodNode[] methods, final Object[] clovers) {
        this.methods = methods;
        this.clovers = clovers;
    }

    @Override
    public Object invoke(final Object arg) {
        final int arity = 1;

        MethodNode method = methods[arity];
        if (method != null) {
            return Truffle.getRuntime().createCallTarget(method).call(this, arg);
        } else {
            for (int i = arity - 1; i >= 0; --i) { // OPTIMIZE: Direct ref to single variadic method?
                method = methods[i];
                if (method != null && method.isVariadic) {
                    return Truffle.getRuntime().createCallTarget(method).call(this, ArraySeq.create(arg));
                }
            }
        }

        return super.invoke(arg);
    }
    
    public Object clover(final int index) { return clovers[index]; }
}
