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
            Object cond = args.first();

            if ((args = args.next()) != null) {
                Object conseq = args.first();

                if ((args = args.next()) != null) {
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
