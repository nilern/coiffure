package com.deepbeginnings.coiffure;

import clojure.lang.IFn;
import clojure.lang.Util;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

public class CallNode extends Expr {
    @Child private Expr callee;
    @Children private Expr[] args;
    private final Method ifnInvoke;
    
    private static final Method[] IFN_INVOKES = Arrays.stream(IFn.class.getMethods())
            .filter(method -> method.getName().equals("invoke"))
            .sorted(Comparator.comparing(Method::getParameterCount))
            .toArray(Method[]::new);

    public static Expr create(final Expr callee, final Expr[] args) { return new CallNode(callee, args); }

    private CallNode(final Expr callee, final Expr[] args) {
        this.callee = callee;
        this.args = args;
        this.ifnInvoke = IFN_INVOKES[Integer.min(args.length, Analyzer.MAX_POSITIONAL_ARITY + 1)];
    }

    @ExplodeLoop
    @Override
    public Object execute(final VirtualFrame frame) {
        final IFn calleeVal = (IFn) callee.execute(frame);
        final Object[] argVals;

        if (args.length <= Analyzer.MAX_POSITIONAL_ARITY) {
            argVals = new Object[args.length];

            for (int i = 0; i < args.length; ++i) {
                argVals[i] = args[i].execute(frame);
            }
        } else {
            argVals = new Object[Analyzer.MAX_POSITIONAL_ARITY + 1];
            final Object[] varArgs = new Object[args.length - Analyzer.MAX_POSITIONAL_ARITY];
            int i = 0;

            for (; i < Analyzer.MAX_POSITIONAL_ARITY; ++i) {
                argVals[i] = args[i].execute(frame);
            }

            for (int j = 0; i < args.length; ++i, ++j) {
                varArgs[j] = args[i].execute(frame);
            }

            argVals[Analyzer.MAX_POSITIONAL_ARITY] = varArgs;
        }

        try {
            return ifnInvoke.invoke(calleeVal, argVals);
        } catch (final InvocationTargetException exn) {
            throw Util.sneakyThrow(exn.getCause());
        } catch (final IllegalAccessException | IllegalArgumentException exn) {
            throw Util.sneakyThrow(exn);
        }
    }
}
