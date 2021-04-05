package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Reflector;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

public final class InvokeInstance extends Expr {
    @Node.Child private Expr receiver;
    private final String methodName;
    @Node.Children private Expr[] args;

    public static Expr create(Expr receiver, String methodName, Expr[] args) {
        return new InvokeInstance(receiver, methodName, args);
    }

    private InvokeInstance(Expr receiver, String methodName, Expr[] args) {
        this.receiver = receiver;
        this.methodName = methodName;
        this.args = args;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        final Object recVal = receiver.execute(frame);

        final Object[] argVals = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            argVals[i] = args[i].execute(frame);
        }

        return Reflector.invokeInstanceMethod(recVal, methodName, argVals);
    }
}
