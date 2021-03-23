package com.deepbeginnings.coiffure;

import clojure.lang.*;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.ArrayList;

final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");
    private static final Symbol LETS = Symbol.intern("let*");

    public static final class LocalEnv {
        private final FrameDescriptor frameDescriptor;
        private final IPersistentStack slots;

        public static LocalEnv root(FrameDescriptor fd) { return new LocalEnv(fd); }

        private LocalEnv(FrameDescriptor fd) { this(fd, PersistentList.EMPTY); }

        private LocalEnv(FrameDescriptor fd, IPersistentStack slots) {
            this.frameDescriptor = fd;
            this.slots = slots;
        }

        public LocalEnv push(Symbol name) {
            FrameSlot slot = frameDescriptor.findOrAddFrameSlot(slots.count());
            return new LocalEnv(frameDescriptor, (IPersistentStack) slots.cons(slot));
        }

        public FrameSlot topSlot() { return (FrameSlot) slots.peek(); }
    }

    public static Expr analyze(LocalEnv locals, Object form) {
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

    private static Expr analyzeDo(LocalEnv locals, ISeq args) {
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

    private static Expr analyzeIf(LocalEnv locals, ISeq args) {
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

    private static Expr analyzeLet(LocalEnv locals, ISeq args) {
        if (args != null) {
            final Object bindingsForm = args.first();
            if (bindingsForm instanceof IPersistentVector) {
                final IPersistentVector bindings = (IPersistentVector) bindingsForm;
                final ArrayList<Let> defs = new ArrayList<>();
                
                for (int i = 0; i < bindings.count(); ++i) {
                    final Object binder = bindings.nth(i);
                    if (binder instanceof Symbol) {
                        ++i;
                        if (i < bindings.count()) {
                            final Expr expr = analyze(locals, bindings.nth(i));
                            locals = locals.push((Symbol) binder);
                            defs.add(LetNodeGen.create(expr, locals.topSlot()));
                        } else {
                            throw new RuntimeException("Binder " + binder + " missing value expression");
                        }
                    } else {
                        throw new RuntimeException("Bad binding form, expected symbol, got: " + binder);
                    }
                }
                
                Expr body = analyzeDo(locals, args.next());
                return defs.isEmpty() ? body : new Do(defs.toArray(new Let[0]), body);
            } else {
                throw new RuntimeException("Bad binding form, expected vector");
            }
        } else {
            throw new RuntimeException("let* missing bindings");
        }
    }
}
