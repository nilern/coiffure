package com.deepbeginnings.coiffure;

import clojure.lang.ISeq;
import clojure.lang.Symbol;
import clojure.lang.Util;

final class Analyzer {
    private static final Symbol IF = Symbol.intern("if");

    public static Expr analyze(Object form) {
        if (form instanceof Long) {
            return new Const(form);
        } else if (form instanceof ISeq) {
            ISeq coll = (ISeq) form;

            if (Util.equiv(coll.first(), IF)) {
                return analyzeIf(coll.next());
            } else {
                throw new RuntimeException("TODO");
            }
        } else {
            throw new RuntimeException("TODO: analyze " + form);
        }

    }

    private static Expr analyzeIf(ISeq args) {
        if (args != null) {
            Object cond = args.first();

            args = args.next();
            if (args != null) {
                Object conseq = args.first();

                args = args.next();
                if (args != null) {
                    Object alt = args.first();

                    if (args.next() == null) {
                        return new If(Analyzer.analyze(cond), Analyzer.analyze(conseq), Analyzer.analyze(alt));
                    } else {
                        throw new RuntimeException("Too few arguments to if");
                    }
                }
            }
        }

        throw new RuntimeException("Too many arguments to if");
    }
}
