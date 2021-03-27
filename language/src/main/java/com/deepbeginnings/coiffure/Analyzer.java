package com.deepbeginnings.coiffure;

import clojure.lang.*;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.ArrayList;
import java.util.List;

final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");
    private static final Symbol LETS = Symbol.intern("let*");
    private static final Symbol DEF = Symbol.intern("def");
    private static final Symbol VAR = Symbol.intern("var");
    private static final Symbol DOT = Symbol.intern(".");

    public static abstract class Env {
        protected final IPersistentMap namedSlots;

        public static Env root(FrameDescriptor fd) { return new RootEnv(fd); }

        private Env(IPersistentMap namedSlots) { this.namedSlots = namedSlots; }

        private FrameSlot get(Symbol name) { return (FrameSlot) namedSlots.valAt(name); }

        abstract protected RootEnv getRoot();

        private NestedEnv push(Symbol name) {
            RootEnv root = getRoot();
            FrameSlot slot = getRoot().addSlot();
            return new NestedEnv(namedSlots, root, name, slot);
        }
    }

    private static final class RootEnv extends Env {
        private final FrameDescriptor frameDescriptor;
        private int localsCount;

        private RootEnv(FrameDescriptor fd) {
            super(PersistentHashMap.EMPTY);
            this.frameDescriptor = fd;
            this.localsCount = 0;
        }

        @Override
        protected RootEnv getRoot() { return this; }

        private FrameSlot addSlot() { return frameDescriptor.addFrameSlot(localsCount++); }
    }

    private static final class NestedEnv extends Env {
        private final RootEnv root;
        private final FrameSlot slot;

        private NestedEnv(IPersistentMap namedSlots, RootEnv root, Symbol name, FrameSlot slot) {
            super(namedSlots.assoc(name, slot));
            this.root = root;
            this.slot = slot;
        }

        @Override
        protected RootEnv getRoot() { return root; }

        private FrameSlot topSlot() { return slot; }
    }

    public static Expr analyze(Env locals, Object form) {
        if (form instanceof Symbol) {
            return analyzeSymbol(locals, (Symbol) form);
        } else if (form instanceof ISeq) {
            ISeq coll = (ISeq) form;

            if (Util.equiv(coll.first(), DO)) {
                return analyzeDo(locals, coll.next());
            } else if (Util.equiv(coll.first(), IF)) {
                return analyzeIf(locals, coll.next());
            } else if (Util.equiv(coll.first(), LETS)) {
                return analyzeLet(locals, coll.next());
            } else if (Util.equiv(coll.first(), DEF)) {
                return analyzeDef(locals, coll.next());
            } else if (Util.equiv(coll.first(), VAR)) {
                return analyzeVar(coll.next());
            } else if (Util.equiv(coll.first(), DOT)) {
                return analyzeDot(locals, coll.next());
            } else {
                throw new RuntimeException("TODO: " + coll.first());
            }
        } else if (form == null
                || form instanceof Boolean
                || form instanceof Long) {
            return new Const(form);
        } else {
            throw new RuntimeException("TODO: analyze " + form);
        }
    }

    private static Expr analyzeSymbol(final Env locals, final Symbol name) {
        FrameSlot slot = locals.get(name);
        if (slot != null) {
            return LocalUseNodeGen.create(slot);
        } else {
            Object v = Namespaces.resolve(name);
            if (v instanceof Var) {
                return GlobalUseNodeGen.create((Var) v);
            } else {
                throw new AssertionError("TODO");
            }
        }
    }

    private static Expr analyzeVar(ISeq args) {
        if (args != null) {
            Object nameForm = args.first();

            if (args.next() == null) {
                if (nameForm instanceof Symbol) {
                    Symbol name = (Symbol) nameForm;
                    Var var = Namespaces.lookupVar(name, false);
                    if (var != null) {
                        return new Const(var);
                    } else {
                        throw new RuntimeException("Unable to resolve var: " + name + " in this context");
                    }
                } else {
                    throw new RuntimeException("var argument must be a symbol");
                }
            } else {
                throw new RuntimeException("Too many arguments to var");
            }
        } else {
            throw new RuntimeException("Too few arguments to var");
        }
    }

    private static Expr analyzeDo(Env locals, ISeq args) {
        final ArrayList<Expr> stmts = new ArrayList<>();

        for (; args != null; args = args.next()) {
            stmts.add(analyze(locals, args.first()));
        }

        return Do.create(stmts.toArray(new Expr[0]));
    }

    private static Expr analyzeIf(Env locals, ISeq args) {
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

    private static Expr analyzeLet(Env locals, ISeq args) {
        if (args != null) {
            final Object bindingsForm = args.first();
            if (bindingsForm instanceof IPersistentVector) {
                final IPersistentVector bindings = (IPersistentVector) bindingsForm;
                final ArrayList<Expr> stmts = new ArrayList<>();

                for (int i = 0; i < bindings.count(); ++i) {
                    final Object binder = bindings.nth(i);
                    if (binder instanceof Symbol) {
                        ++i;
                        if (i < bindings.count()) {
                            final Expr expr = analyze(locals, bindings.nth(i));
                            NestedEnv locals_ = locals.push((Symbol) binder);
                            locals = locals_;
                            stmts.add(LocalDefNodeGen.create(expr, locals_.topSlot()));
                        } else {
                            throw new RuntimeException("Binder " + binder + " missing value expression");
                        }
                    } else {
                        throw new RuntimeException("Bad binding form, expected symbol, got: " + binder);
                    }
                }

                stmts.add(analyzeDo(locals, args.next()));
                return Do.create(stmts.toArray(new Expr[0]));
            } else {
                throw new RuntimeException("Bad binding form, expected vector");
            }
        } else {
            throw new RuntimeException("let* missing bindings");
        }
    }

    private static Expr analyzeDef(Env locals, ISeq args) {
        if (args != null) {
            final Object nameForm = args.first();

            if ((args = args.next()) != null) {
                final Object init = args.first();

                if (args.next() == null) {
                    if (nameForm instanceof Symbol) {
                        final Symbol name = (Symbol) nameForm;

                        final Var var = Namespaces.lookupVar(name, true);
                        if (var != null) {
                            return GlobalDef.create(var, analyze(locals, init));
                        } else {
                            throw new RuntimeException("Can't def a non-pre-existing qualified var");
                        }
                    } else {
                        throw new RuntimeException("First argument to def must be a symbol");
                    }
                } else {
                    throw new RuntimeException("Too many arguments to def");
                }
            } else {
                throw new AssertionError("TODO");
            }
        } else {
            throw new RuntimeException("Too few arguments to def");
        }
    }

    private static Expr analyzeDot(final Env locals, ISeq argForms) {
        if (argForms != null) {
            final Expr receiver = analyze(locals, argForms.first());

            if ((argForms = argForms.next()) != null) {
                final Object msgForm = argForms.first();

                if (msgForm instanceof Symbol) {
                    final String methodName = ((Symbol) msgForm).getName();

                    final List<Expr> args = new ArrayList<>();
                    while ((argForms = argForms.next()) != null) {
                        args.add(analyze(locals, argForms.first()));
                    }

                    return InvokeInstance.create(receiver, methodName, args.toArray(new Expr[0]));
                } else {
                    throw new AssertionError("TODO");
                }
            }
        }

        throw new RuntimeException("Too few arguments to .");
    }
}
