package com.deepbeginnings.coiffure;

import clojure.lang.*;

import com.deepbeginnings.coiffure.nodes.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.*;

public final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");
    private static final Symbol LETS = Symbol.intern("let*");
    private static final Symbol LOOP = Symbol.intern("loop");
    private static final Symbol RECUR = Symbol.intern("recur");
    private static final Symbol FNS = Symbol.intern("fn*");
    private static final Symbol DEF = Symbol.intern("def");
    private static final Symbol VAR = Symbol.intern("var");
    private static final Symbol NEW = Symbol.intern("new");
    private static final Symbol DOT = Symbol.intern(".");
    private static final Symbol _AMP_ = Symbol.intern("&");

    public static final int MAX_POSITIONAL_ARITY = 20;

    // # Env

    private static abstract class Env {
        protected abstract Expr get(Symbol name);

        protected ClosureEnv pushFn() { return new ClosureEnv(this); }
    }

    private static abstract class MethodsEnv extends Env {
        protected MethodEnv pushMethod(final Iterable<Symbol> args) {
            return MethodEnv.create(this, new FrameDescriptor(), args);
        }
    }

    private static final class ToplevelEnv extends MethodsEnv {
        @Override
        protected Expr get(final Symbol name) { return null; }
    }

    private static final class ClosureEnv extends MethodsEnv {
        private final Env parent;
        private FrameSlot self;
        protected final Map<Symbol, Expr> closings;

        private ClosureEnv(final Env parent) {
            super();
            this.parent = parent;
            this.self = null;
            this.closings = new HashMap<>();
        }

        @Override
        protected MethodEnv pushMethod(final Iterable<Symbol> args) {
            final FrameDescriptor frameDescriptor = new FrameDescriptor();
            self = frameDescriptor.addFrameSlot(0);
            return MethodEnv.create(this, frameDescriptor, args);
        }

        @Override
        protected Expr get(final Symbol name) {
            Expr closing = closings.get(name);
            if (closing != null) { return closing; }

            closing = parent.get(name);
            if (closing != null) {
                final int cloverIndex = closings.size();
                closings.put(name, closing);
                return CloverUseNodeGen.create(self, cloverIndex);
            } else {
                return null;
            }
        }
    }

    private static abstract class FrameEnv extends Env {
        protected final IPersistentMap namedSlots;

        private FrameEnv(final IPersistentMap namedSlots) {
            super();
            this.namedSlots = namedSlots;
        }

        abstract protected MethodEnv getFrameRoot();

        private NestedEnv push(final Symbol name) {
            final MethodEnv root = getFrameRoot();
            final FrameSlot slot = root.addSlot();
            return new NestedEnv(namedSlots, root, name, slot);
        }

        protected FrameSlot getSlot(final Symbol name) { return (FrameSlot) namedSlots.valAt(name); }
    }

    private static final class MethodEnv extends FrameEnv {
        private final Env parent;
        private final FrameDescriptor frameDescriptor;
        private int localsCount;
        private final List<FrameSlot> paramSlots;

        private static MethodEnv create(final Env parent, final FrameDescriptor frameDescriptor,
                                        final Iterable<Symbol> args
        ) {
            ITransientMap namedSlots = PersistentHashMap.EMPTY.asTransient();
            int localsCount = 0;
            final List<FrameSlot> paramSlots = new ArrayList<>();

            for (final Symbol arg : args) {
                final FrameSlot slot = frameDescriptor.findOrAddFrameSlot(localsCount++);
                namedSlots = namedSlots.assoc(arg, slot);
                paramSlots.add(slot);
            }

            return new MethodEnv(namedSlots.persistent(), parent, frameDescriptor, localsCount, paramSlots);
        }

        private MethodEnv(final IPersistentMap namedSlots, final Env parent,
                          final FrameDescriptor frameDescriptor, final int localsCount, final List<FrameSlot> paramSlots
        ) {
            super(namedSlots);
            this.parent = parent;
            this.frameDescriptor = frameDescriptor;
            this.localsCount = localsCount;
            this.paramSlots = paramSlots;
        }

        @Override
        protected Expr get(final Symbol name) {
            final FrameSlot slot = getSlot(name);
            return (slot != null) ? LocalUseNodeGen.create(slot) : parent.get(name);
        }

        @Override
        protected MethodEnv getFrameRoot() { return this; }

        private FrameSlot addSlot() { return frameDescriptor.addFrameSlot(localsCount++); }

        protected List<FrameSlot> paramSlots() { return paramSlots; }
    }

    private static final class NestedEnv extends FrameEnv {
        private final MethodEnv root;
        private final FrameSlot slot;

        private NestedEnv(final IPersistentMap namedSlots, final MethodEnv root, final Symbol name, final FrameSlot slot
        ) {
            super(namedSlots.assoc(name, slot));
            this.root = root;
            this.slot = slot;
        }

        @Override
        protected Expr get(final Symbol name) {
            final FrameSlot slot = getSlot(name);
            return (slot != null) ? LocalUseNodeGen.create(slot) : root.parent.get(name);
        }

        @Override
        protected MethodEnv getFrameRoot() { return root; }

        private FrameSlot topSlot() { return slot; }
    }

    // # Context

    private static abstract class Context {
        protected static final Context NONTAIL = new NonTail();

        protected static Tail tail(final List<FrameSlot> params) { return new Tail(params); }

        private static final class NonTail extends Context {}

        private static final class Tail extends Context {
            public final List<FrameSlot> params;
            public boolean recurred;

            private Tail(final List<FrameSlot> params) { this.params = params; }
        }
    }

    // # Analysis

    public static MethodNode analyzeToplevel(final ISeq form) {
        return analyzeMethod(true, new ToplevelEnv(), form);
    }

    private static Expr analyze(final FrameEnv locals, final Context ctx, final Object form) {
        if (form instanceof Symbol) {
            return analyzeSymbol(locals, (Symbol) form);
        } else if (form instanceof ISeq) {
            final ISeq coll = (ISeq) form;

            if (Util.equiv(coll.first(), DO)) {
                return analyzeDo(locals, ctx, coll.next());
            } else if (Util.equiv(coll.first(), IF)) {
                return analyzeIf(locals, ctx, coll.next());
            } else if (Util.equiv(coll.first(), LETS)) {
                return analyzeLet(locals, ctx, coll.next());
            } else if (Util.equiv(coll.first(), FNS)) {
                return analyzeFn(locals, coll.next());
            } else if (Util.equiv(coll.first(), DEF)) {
                return analyzeDef(locals, coll.next());
            } else if (Util.equiv(coll.first(), VAR)) {
                return analyzeVar(coll.next());
            } else if (Util.equiv(coll.first(), NEW)) {
                return analyzeNew(locals, coll.next());
            } else if (Util.equiv(coll.first(), DOT)) {
                return analyzeDot(locals, coll.next());
            } else if (Util.equiv(coll.first(), LOOP)) {
                return analyzeLoop(locals, coll.next());
            } else if (Util.equiv(coll.first(), RECUR)) {
                return analyzeRecur(locals, ctx, coll.next());
            } else if (coll.count() > 0) {
                return analyzeCall(locals, coll.first(), coll.next());
            } else {
                throw new RuntimeException("TODO: analyze " + form);
            }
        } else if (form == null
                || form instanceof Boolean
                || form instanceof Long) {
            return new Const(form);
        } else {
            throw new RuntimeException("TODO: analyze " + form);
        }

    }

    private static Expr analyzeSymbol(final FrameEnv locals, final Symbol name) {
        final Expr expr = locals.get(name);
        if (expr != null) {
            return expr;
        } else {
            final Object v = Namespaces.resolve(name);
            if (v instanceof Var) {
                return GlobalUseNodeGen.create((Var) v);
            } else {
                throw new AssertionError("TODO");
            }
        }
    }

    private static Expr analyzeVar(final ISeq args) {
        if (args != null) {
            final Object nameForm = args.first();

            if (args.next() == null) {
                if (nameForm instanceof Symbol) {
                    final Symbol name = (Symbol) nameForm;
                    final Var var = Namespaces.lookupVar(name, false);
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

    private static Expr analyzeDo(final FrameEnv locals, final Context ctx, ISeq args) {
        final ArrayList<Expr> stmts = new ArrayList<>();

        while (args != null) {
            final Object stmt = args.first();
            args = args.next();

            final Context stmtCtx = (args != null) ? Context.NONTAIL : ctx;
            stmts.add(analyze(locals, stmtCtx, stmt));
        }

        return Do.create(stmts.toArray(new Expr[0]));
    }

    private static Expr analyzeIf(final FrameEnv locals, final Context ctx, ISeq args) {
        if (args != null) {
            final Expr cond = analyze(locals, Context.NONTAIL, args.first());

            if ((args = args.next()) != null) {
                final Expr conseq = analyze(locals, ctx, args.first());

                if ((args = args.next()) != null) {
                    final Expr alt = analyze(locals, ctx, args.first());

                    if (args.next() == null) {
                        return new If(cond, conseq, alt);
                    } else {
                        throw new RuntimeException("Too many arguments to if");
                    }
                } else {
                    return new If(cond, conseq, new Const(null));
                }
            }
        }

        throw new RuntimeException("Too few arguments to if");
    }

    private static Expr analyzeLet(FrameEnv locals, final Context ctx, final ISeq args) {
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
                            final Expr expr = analyze(locals, Context.NONTAIL, bindings.nth(i));
                            final NestedEnv locals_ = locals.push((Symbol) binder);
                            locals = locals_;
                            stmts.add(LocalDefNodeGen.create(expr, locals_.topSlot()));
                        } else {
                            throw new RuntimeException("Binder " + binder + " missing value expression");
                        }
                    } else {
                        throw new RuntimeException("Bad binding form, expected symbol, got: " + binder);
                    }
                }

                stmts.add(analyzeDo(locals, ctx, args.next())); // OPTIMIZE: Unnest Do:s
                return Do.create(stmts.toArray(new Expr[0]));
            } else {
                throw new RuntimeException("Bad binding form, expected vector");
            }
        } else {
            throw new RuntimeException("let* missing bindings");
        }
    }

    // TODO: Factor out bindings part in common with `let*`:
    private static Expr analyzeLoop(FrameEnv locals, final ISeq args) {
        if (args != null) {
            final Object bindingsForm = args.first();
            if (bindingsForm instanceof IPersistentVector) {
                final IPersistentVector bindings = (IPersistentVector) bindingsForm;

                final List<FrameSlot> paramSlots = new ArrayList<>();
                final ArrayList<Expr> stmts = new ArrayList<>();

                for (int i = 0; i < bindings.count(); ++i) {
                    final Object binder = bindings.nth(i);
                    if (binder instanceof Symbol) {
                        ++i;
                        if (i < bindings.count()) {
                            final Expr expr = analyze(locals, Context.NONTAIL, bindings.nth(i));
                            final NestedEnv locals_ = locals.push((Symbol) binder);
                            locals = locals_;
                            final FrameSlot slot = locals_.topSlot();
                            paramSlots.add(slot);
                            stmts.add(LocalDefNodeGen.create(expr, slot));
                        } else {
                            throw new RuntimeException("Binder " + binder + " missing value expression");
                        }
                    } else {
                        throw new RuntimeException("Bad binding form, expected symbol, got: " + binder);
                    }
                }

                final Context.Tail ctx = Context.tail(paramSlots);
                final Expr body = analyzeDo(locals, ctx, args.next());
                if (ctx.recurred) {
                    stmts.add(new Loop(body));
                } else {
                    // TODO: Warn about non-recursing `loop`?
                    stmts.add(body); // OPTIMIZE: Unnest Do:s
                }
                return Do.create(stmts.toArray(new Expr[0]));
            } else {
                throw new RuntimeException("Bad binding form, expected vector");
            }
        } else {
            throw new RuntimeException("let* missing bindings");
        }
    }

    private static Expr analyzeRecur(final FrameEnv env, final Context ctx, ISeq argsForm) {
        if (ctx instanceof Context.Tail) {
            final Context.Tail tailCtx = (Context.Tail) ctx;

            final List<Expr> argDefs = new ArrayList<>();
            int argc = 0;
            for (final Iterator<FrameSlot> slotsIt = tailCtx.params.iterator();
                 slotsIt.hasNext(); argsForm = argsForm.next()
            ) {
                if (argsForm != null) {
                    ++argc;
                    final Expr arg = analyze(env, Context.NONTAIL, argsForm.first());
                    final FrameSlot slot = slotsIt.next();
                    argDefs.add(LocalDefNodeGen.create(arg, slot));
                } else {
                    throw new IllegalArgumentException(
                            String.format("Mismatched argument count to recur, expected: %d args, got: %d",
                                    tailCtx.params.size(), argc));
                }
            }

            if (argsForm == null) {
                tailCtx.recurred = true;
                return new Recur(argDefs.toArray(new Expr[0]));
            } else {
                throw new IllegalArgumentException(
                        String.format("Mismatched argument count to recur, expected: %d args, got: %d",
                                tailCtx.params.size(), argc));
            }
        } else {
            throw new UnsupportedOperationException("Can only recur from tail position");
        }
    }

    private static Expr analyzeFn(final FrameEnv locals, ISeq args) {
        final MethodNode[] methods = new MethodNode[MAX_POSITIONAL_ARITY + 1];
        MethodNode variadicMethod = null;
        final ClosureEnv env = locals.pushFn();

        for (int i = 0; args != null; args = args.next(), ++i) {
            final MethodNode method = analyzeMethod(false, env, args.first());

            if (method.isVariadic()) {
                if (variadicMethod == null) {
                    variadicMethod = method;
                } else {
                    throw new RuntimeException("Can't have more than 1 variadic overload");
                }
            }

            if (methods[method.getMinArity()] == null) {
                methods[method.getMinArity()] = method;
            } else {
                throw new RuntimeException("Can't have more than 1 overload with arity " + method.getMinArity());
            }
        }

        if (variadicMethod != null) {
            for (int arity = variadicMethod.getMinArity() + 1; arity < methods.length; ++arity) {
                if (methods[arity] == null) {
                    methods[arity] = variadicMethod;
                } else {
                    throw new RuntimeException(
                            "Can't have fixed arity function with more params than variadic function");
                }
            }
        }

        return new ClosureNode(methods, variadicMethod, env.closings.values().toArray(new Expr[0]));
    }

    private static MethodNode analyzeMethod(final boolean isStatic, final MethodsEnv env, final Object methodForm) {
        if (methodForm instanceof ISeq) {
            final ISeq methodSeq = (ISeq) methodForm;

            final Object paramsObj = methodSeq.first();
            if (paramsObj instanceof IPersistentVector) {
                final IPersistentVector paramsVec = (IPersistentVector) paramsObj;
                final ISeq bodySeq = methodSeq.next();

                final List<Symbol> params = new ArrayList<>();
                if (!isStatic) { params.add(Symbol.intern("self" + RT.nextID())); }

                boolean isVariadic = false;

                {
                    int i = 0;
                    for (; i < paramsVec.count(); ++i) {
                        final Object paramObj = paramsVec.nth(i);
                        if (paramObj instanceof Symbol) {
                            final Symbol param = (Symbol) paramObj;
                            if (param.getNamespace() != null) {
                                throw new RuntimeException("Can't use qualified name as parameter: " + param);
                            }

                            if (Util.equiv(paramObj, _AMP_)) {
                                isVariadic = true;
                                ++i;
                                break;
                            } else {
                                params.add(param);
                            }
                        } else {
                            throw new RuntimeException("Non-symbol fn param: " + paramObj);
                        }
                    }

                    if (isVariadic) {
                        if (i < paramsVec.count()) {
                            final Object paramObj = paramsVec.nth(i);
                            if (paramObj instanceof Symbol) {
                                final Symbol param = (Symbol) paramObj;
                                if (param.getNamespace() != null) {
                                    throw new RuntimeException("Can't use qualified name as parameter: " + param);
                                }

                                if (Util.equiv(paramObj, _AMP_)) {
                                    throw new RuntimeException("Invalid parameter list: extra &");
                                } else {
                                    params.add(param);
                                    ++i;
                                }

                                if (i != paramsVec.count()) {
                                    throw new RuntimeException("Invalid parameter list: extra param after rest param");
                                }
                            } else {
                                throw new RuntimeException("Non-symbol fn param: " + paramObj);
                            }
                        } else {
                            throw new RuntimeException("Missing rest param name");
                        }
                    }
                }

                final MethodEnv locals = env.pushMethod(params);
                int minArity = params.size();
                if (!isStatic) { --minArity; }
                if (isVariadic) { --minArity; }

                final List<Expr> stmts = new ArrayList<>();
                {
                    int i = 0;
                    for (final Symbol param : params) {
                        stmts.add(LocalDefNodeGen.create(new ArgUse(i), locals.getSlot(param)));
                        ++i;
                    }
                }

                // TODO: Merge with similar code in `analyzeLoop`:
                List<FrameSlot> paramSlots = locals.paramSlots();
                if (!isStatic) { paramSlots = paramSlots.subList(1, paramSlots.size()); }
                final Context.Tail ctx = Context.tail(paramSlots);
                final Expr body = analyzeDo(locals, ctx, bodySeq);
                if (ctx.recurred) {
                    stmts.add(new Loop(body));
                } else {
                    stmts.add(body); // OPTIMIZE: Unnest Do:s
                }

                return new MethodNode(Language.getCurrentLanguage(), locals.frameDescriptor, minArity, isVariadic,
                        Do.create(stmts.toArray(new Expr[0])));
            } else {
                throw new RuntimeException("fn missing params vector");
            }
        } else {
            throw new RuntimeException("Invalid fn method " + methodForm);
        }
    }

    private static Expr analyzeCall(final FrameEnv locals, final Object calleeForm, ISeq argForms) {
        final Expr callee = analyze(locals, Context.NONTAIL, calleeForm);

        final List<Expr> args = new ArrayList<>();
        for (; argForms != null; argForms = argForms.next()) {
            args.add(analyze(locals, Context.NONTAIL, argForms.first()));
        }

        return CallNode.create(callee, args.toArray(new Expr[0]));
    }

    private static Expr analyzeDef(final FrameEnv locals, ISeq args) {
        if (args != null) {
            final Object nameForm = args.first();

            if ((args = args.next()) != null) {
                final Object init = args.first();

                if (args.next() == null) {
                    if (nameForm instanceof Symbol) {
                        final Symbol name = (Symbol) nameForm;

                        final Var var = Namespaces.lookupVar(name, true);
                        if (var != null) {
                            return GlobalDef.create(var, analyze(locals, Context.NONTAIL, init));
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

    private static Expr analyzeNew(final FrameEnv locals, ISeq argForms) {
        if (argForms != null) {
            final Object classname = argForms.first();
            final Class<?> klass = (classname instanceof Symbol && locals.get((Symbol) classname) != null)
                    ? null
                    : Namespaces.maybeClass(classname, false);
            if (klass == null) { throw new RuntimeException("Unable to resolve classname: " + classname); }

            final ArrayList<Expr> args = new ArrayList<>();
            while ((argForms = argForms.next()) != null) {
                args.add(analyze(locals, Context.NONTAIL, argForms.first()));
            }

            return New.create(klass, args.toArray(new Expr[0]));
        } else {
            throw new RuntimeException("New expression missing class");
        }
    }

    private static Expr analyzeDot(final FrameEnv locals, ISeq argForms) {
        if (argForms != null) {
            final Expr receiver = analyze(locals, Context.NONTAIL, argForms.first());

            if ((argForms = argForms.next()) != null) {
                final Object msgForm = argForms.first();

                if (msgForm instanceof Symbol) {
                    final String methodName = ((Symbol) msgForm).getName();

                    final List<Expr> args = new ArrayList<>();
                    while ((argForms = argForms.next()) != null) {
                        args.add(analyze(locals, Context.NONTAIL, argForms.first()));
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
