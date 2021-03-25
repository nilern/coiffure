package com.deepbeginnings.coiffure;

import clojure.lang.*;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.ArrayList;

final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");
    private static final Symbol LETS = Symbol.intern("let*");
    private static final Symbol DEF = Symbol.intern("def");

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

    private static Expr analyzeSymbol(final Env locals, final Symbol name) {
        FrameSlot slot = locals.get(name);
        if (slot != null) {
            return LocalUseNodeGen.create(slot);
        } else {
            throw new RuntimeException("TODO: UseGlobal");
        }
    }

    private static Expr analyzeDo(Env locals, ISeq args) {
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
                final ArrayList<LocalDef> defs = new ArrayList<>();

                for (int i = 0; i < bindings.count(); ++i) {
                    final Object binder = bindings.nth(i);
                    if (binder instanceof Symbol) {
                        ++i;
                        if (i < bindings.count()) {
                            final Expr expr = analyze(locals, bindings.nth(i));
                            NestedEnv locals_ = locals.push((Symbol) binder);
                            locals = locals_;
                            defs.add(LocalDefNodeGen.create(expr, locals_.topSlot()));
                        } else {
                            throw new RuntimeException("Binder " + binder + " missing value expression");
                        }
                    } else {
                        throw new RuntimeException("Bad binding form, expected symbol, got: " + binder);
                    }
                }

                Expr body = analyzeDo(locals, args.next());
                return defs.isEmpty() ? body : new Do(defs.toArray(new LocalDef[0]), body);
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
                            return Def.create(var, analyze(locals, init));
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
}
