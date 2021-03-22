package com.deepbeginnings.coiffure;

import clojure.lang.ISeq;
import clojure.lang.Symbol;
import clojure.lang.Util;

import java.util.ArrayList;

final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");

    public static Expr analyze(Object form) {
        if (form instanceof ISeq) {
            ISeq coll = (ISeq) form;

            if (Util.equiv(coll.first(), DO)) {
                return analyzeDo(coll.next());
            } else if (Util.equiv(coll.first(), IF)) {
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
    
    private static Expr analyzeDo(ISeq args) {
        if (args != null) {
            final ArrayList<Expr> stmts = new ArrayList<>();
            Expr expr = analyze(args.first());

            while ((args = args.next()) != null) {
                stmts.add(expr);
                expr = analyze(args.first());
            }

            return new Do(stmts.toArray(new Expr[0]), expr);
        } else {
            return new Const(null);
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
