package com.deepbeginnings.coiffure;

import clojure.lang.ISeq;
import clojure.lang.Symbol;
import clojure.lang.Util;

final class Analyzer {
    private static final Symbol IF = Symbol.intern("if");

    public static Expr analyze(Object form) {
        if (form instanceof ISeq) {
            ISeq coll = (ISeq) form;

            if (Util.equiv(coll.first(), IF)) {
                return analyzeIf(coll.next());
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

    private static Expr analyzeIf(ISeq args) {
        if (args != null) {
            final Object cond = args.first();

            if ((args = args.next()) != null) {
                final Object conseq = args.first();

                if ((args = args.next()) != null) {
                    final Object alt = args.first();

                    if (args.next() == null) {
                        return new If(analyze(cond), analyze(conseq), analyze(alt));
                    } else {
                        throw new RuntimeException("Too many arguments to if");
                    }
                } else {
                    return new If(analyze(cond), analyze(conseq), new Const(null));
                }
            }
        }

        throw new RuntimeException("Too few arguments to if");
    }
}
