package com.deepbeginnings.coiffure;

import clojure.lang.IPersistentVector;
import clojure.lang.ISeq;
import clojure.lang.Symbol;
import clojure.lang.Util;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.ArrayList;

final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");
    private static final Symbol LETS = Symbol.intern("let*");

    public static Expr analyze(FrameDescriptor locals, Object form) {
        if (form instanceof ISeq) {
            ISeq coll = (ISeq) form;

            if (Util.equiv(coll.first(), DO)) {
                return analyzeDo(locals, coll.next());
            } else if (Util.equiv(coll.first(), IF)) {
                return analyzeIf(locals, coll.next());
            } else if (Util.equiv(coll.first(), LETS)) {
                return analyzeLet(locals, coll.next());
            } else {
                throw new RuntimeException("TODO");
            }
        } else if (form == null
                || form instanceof Boolean
                || form instanceof Long) {
            return new Const(form);
        } else {
            throw new RuntimeException("TODO: analyze " + form);
        }
    }

    private static Expr analyzeDo(FrameDescriptor locals, ISeq args) {
        if (args != null) {
            final ArrayList<Expr> stmts = new ArrayList<>();
            Expr expr = analyze(locals, args.first());

            while ((args = args.next()) != null) {
                stmts.add(expr);
                expr = analyze(locals, args.first());
            }

            return stmts.isEmpty() ? expr : new Do(stmts.toArray(new Expr[0]), expr);
        } else {
            return new Const(null);
        }
    }

    private static Expr analyzeIf(FrameDescriptor locals, ISeq args) {
        if (args != null) {
            final Object cond = args.first();

            if ((args = args.next()) != null) {
                final Object conseq = args.first();

                if ((args = args.next()) != null) {
                    final Object alt = args.first();

                    if (args.next() == null) {
                        return new If(analyze(locals, cond), analyze(locals, conseq), analyze(locals, alt));
                    } else {
                        throw new RuntimeException("Too many arguments to if");
                    }
                } else {
                    return new If(analyze(locals, cond), analyze(locals, conseq), new Const(null));
                }
            }
        }

        throw new RuntimeException("Too few arguments to if");
    }

    private static Expr analyzeLet(FrameDescriptor locals, ISeq args) {
        if (args != null) {
            final Object bindingsForm = args.first();
            if (bindingsForm instanceof IPersistentVector) {
                final IPersistentVector bindings = (IPersistentVector) bindingsForm;
                return analyzeLetTail(locals, bindings, 0, args.next());
            } else {
                throw new RuntimeException("Bad binding form, expected vector");
            }
        } else {
            throw new RuntimeException("let* missing bindings");
        }
    }

    private static Expr analyzeLetTail(
            FrameDescriptor locals, IPersistentVector bindings, int bindingIndex, ISeq body
    ) {
        if (bindingIndex < bindings.count()) {
            final Object binder = bindings.nth(bindingIndex);
            if (binder instanceof Symbol) {
                ++bindingIndex;
                if (bindingIndex < bindings.count()) {
                    final Expr expr = analyze(locals, bindings.nth(bindingIndex));
                    FrameSlot slot = locals.addFrameSlot(binder);
                    // OPTIMIZE:
                    return new Do(new Expr[] {LetNodeGen.create(expr, slot)},
                            analyzeLetTail(locals, bindings, ++bindingIndex, body));
                } else {
                    throw new RuntimeException("Binder " + binder + " missing value expression");
                }
            } else {
                throw new RuntimeException("Bad binding form, expected symbol, got: " + binder);
            }
        } else {
            return analyzeDo(locals, body);
        }
    }
}
