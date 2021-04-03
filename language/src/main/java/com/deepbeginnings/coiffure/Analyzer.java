package com.deepbeginnings.coiffure;

import clojure.lang.*;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.*;

final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");
    private static final Symbol LETS = Symbol.intern("let*");
    private static final Symbol FNS = Symbol.intern("fn*");
    private static final Symbol DEF = Symbol.intern("def");
    private static final Symbol VAR = Symbol.intern("var");
    private static final Symbol NEW = Symbol.intern("new");
    private static final Symbol DOT = Symbol.intern(".");
    private static final Symbol _AMP_ = Symbol.intern("&");

    private static final int MAX_POSITIONAL_ARITY = 20;

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

        private static MethodEnv create(final Env parent, final FrameDescriptor frameDescriptor,
                                        final Iterable<Symbol> args
        ) {
            ITransientMap namedSlots = PersistentHashMap.EMPTY.asTransient();
            int localsCount = 0;

            for (final Symbol arg : args) {
                namedSlots = namedSlots.assoc(arg, frameDescriptor.findOrAddFrameSlot(localsCount++));
            }

            return new MethodEnv(namedSlots.persistent(), parent, frameDescriptor, localsCount);
        }

        private MethodEnv(final IPersistentMap namedSlots, final Env parent,
                          final FrameDescriptor frameDescriptor, final int localsCount
        ) {
            super(namedSlots);
            this.parent = parent;
            this.frameDescriptor = frameDescriptor;
            this.localsCount = localsCount;
        }

        @Override
        protected Expr get(final Symbol name) {
            final FrameSlot slot = getSlot(name);
            return (slot != null) ? LocalUseNodeGen.create(slot) : parent.get(name);
        }

        @Override
        protected MethodEnv getFrameRoot() { return this; }

        private FrameSlot addSlot() { return frameDescriptor.addFrameSlot(localsCount++); }
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

    public static MethodNode analyzeToplevel(final ISeq form) {
        return analyzeMethod(true, new ToplevelEnv(), form);
    }

    private static Expr analyze(final FrameEnv locals, final Object form) {
        if (form instanceof Symbol) {
            return analyzeSymbol(locals, (Symbol) form);
        } else if (form instanceof ISeq) {
            final ISeq coll = (ISeq) form;

            if (Util.equiv(coll.first(), DO)) {
                return analyzeDo(locals, coll.next());
            } else if (Util.equiv(coll.first(), IF)) {
                return analyzeIf(locals, coll.next());
            } else if (Util.equiv(coll.first(), LETS)) {
                return analyzeLet(locals, coll.next());
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
            } else if (coll.count() > 0) {
                return analyzeCall(locals, coll.first(), coll.next());
            } else{
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

    private static Expr analyzeDo(final FrameEnv locals, ISeq args) {
        final ArrayList<Expr> stmts = new ArrayList<>();

        for (; args != null; args = args.next()) {
            stmts.add(analyze(locals, args.first()));
        }

        return Do.create(stmts.toArray(new Expr[0]));
    }

    private static Expr analyzeIf(final FrameEnv locals, ISeq args) {
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

    private static Expr analyzeLet(FrameEnv locals, final ISeq args) {
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

                stmts.add(analyzeDo(locals, args.next())); // OPTIMIZE: Unnest Do:s
                return Do.create(stmts.toArray(new Expr[0]));
            } else {
                throw new RuntimeException("Bad binding form, expected vector");
            }
        } else {
            throw new RuntimeException("let* missing bindings");
        }
    }

    private static Expr analyzeFn(final FrameEnv locals, ISeq args) {
        final MethodNode[] methods = new MethodNode[MAX_POSITIONAL_ARITY + 1];
        final ClosureEnv env = locals.pushFn();

        {
            boolean seenVariadic = false;
            for (int i = 0; args != null; args = args.next(), ++i) {
                final MethodNode method = analyzeMethod(false, env, args.first());

                if (method.isVariadic) {
                    if (!seenVariadic) {
                        seenVariadic = true;
                    } else {
                        throw new RuntimeException("Can't have more than 1 variadic overload");
                    }
                }

                if (methods[method.minArity] == null) {
                    methods[method.minArity] = method;
                } else {
                    throw new RuntimeException("Can't have more than 1 overload with arity " + method.minArity);
                }
            }
        }

        return new ClosureNode(methods, env.closings.values().toArray(new Expr[0]));
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
                stmts.add(analyzeDo(locals, bodySeq)); // OPTIMIZE: Unnest Do:s

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
        final Expr callee = analyze(locals, calleeForm);
        
        final List<Expr> args = new ArrayList<>();
        for (; argForms != null; argForms = argForms.next()) {
            args.add(analyze(locals, argForms.first()));
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

    private static Expr analyzeNew(final FrameEnv locals, ISeq argForms) {
        if (argForms != null) {
            final Object classname = argForms.first();
            final Class<?> klass = (classname instanceof Symbol && locals.get((Symbol) classname) != null)
                    ? null
                    : Namespaces.maybeClass(classname, false);
            if (klass == null) { throw new RuntimeException("Unable to resolve classname: " + classname); }

            final ArrayList<Expr> args = new ArrayList<>();
            while ((argForms = argForms.next()) != null) {
                args.add(analyze(locals, argForms.first()));
            }

            return New.create(klass, args.toArray(new Expr[0]));
        } else {
            throw new RuntimeException("New expression missing class");
        }
    }

    private static Expr analyzeDot(final FrameEnv locals, ISeq argForms) {
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
