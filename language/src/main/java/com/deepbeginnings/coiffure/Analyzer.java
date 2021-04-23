package com.deepbeginnings.coiffure;

import clojure.lang.*;

import com.deepbeginnings.coiffure.nodes.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public final class Analyzer {
    private static final Symbol DO = Symbol.intern("do");
    private static final Symbol IF = Symbol.intern("if");
    private static final Symbol THROW = Symbol.intern("throw");
    private static final Symbol TRY = Symbol.intern("try");
    private static final Symbol CATCH = Symbol.intern("catch");
    private static final Symbol FINALLY = Symbol.intern("finally");
    // NOTE: It was more convenient to implement `locking` as a special form directly:
    private static final Symbol LOCKING = Symbol.intern("locking");
    private static final Symbol LETS = Symbol.intern("let*");
    private static final Symbol LOOP = Symbol.intern("loop");
    private static final Symbol RECUR = Symbol.intern("recur");
    private static final Symbol FNS = Symbol.intern("fn*");
    private static final Symbol DEF = Symbol.intern("def");
    private static final Symbol SET_BANG_ = Symbol.intern("set!");
    private static final Symbol VAR = Symbol.intern("var");
    private static final Symbol NEW = Symbol.intern("new");
    private static final Symbol DOT = Symbol.intern(".");
    private static final Symbol _AMP_ = Symbol.intern("&");

    private static final Set<Symbol> SPECIAL_FORMS = Stream.of(
            DO, IF, THROW, TRY, CATCH, FINALLY, LOCKING, LETS, LOOP, RECUR, FNS, DEF, SET_BANG_, VAR, NEW, DOT, _AMP_
    ).collect(Collectors.toCollection(HashSet::new));

    private static boolean isSpecialForm(final Object op) {
        return op instanceof Symbol && SPECIAL_FORMS.contains((Symbol) op);
    }

    public static final int MAX_POSITIONAL_ARITY = 20;

    static final Symbol CLASS = Symbol.intern("Class");

    static final Keyword TAG_KEY = Keyword.intern(null, "tag");

    // # Env

    private static abstract class Env {
        protected abstract Expr get(Symbol name);

        protected abstract Optional<Var> macroVar(Symbol name);

        final Optional<Var> macroVar(final Object op) {
            Optional<Var> optVar = (op instanceof Var) ? Optional.of((Var) op)
                    : (op instanceof Symbol) ? macroVar((Symbol) op)
                    : Optional.empty();

            optVar = optVar.filter(Var::isMacro);

            optVar.ifPresent(var -> {
                if (var.ns != Namespaces.currentNS() && !var.isPublic()) {
                    throw new IllegalStateException("var: " + var + " is not public");
                }
            });

            return optVar;
        }

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

        @Override
        protected Optional<Var> macroVar(final Symbol name) {
            return Optional.ofNullable(Namespaces.lookupVar(name, false));
        }
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

        @Override
        protected Optional<Var> macroVar(final Symbol name) {
            return closings.containsKey(name) ? Optional.empty() : parent.macroVar(name);
        }
    }

    private static abstract class FrameEnv extends Env {
        protected final IPersistentMap namedSlots;

        private FrameEnv(final IPersistentMap namedSlots) {
            super();
            this.namedSlots = namedSlots;
        }

        abstract protected MethodEnv getFrameRoot();

        private NestedEnv push(final Symbol name) { return push(name, FrameSlotKind.Illegal); }

        private NestedEnv push(final Symbol name, final FrameSlotKind slotKind) {
            final MethodEnv root = getFrameRoot();
            final FrameSlot slot = root.addSlot(slotKind);
            return new NestedEnv(namedSlots, root, name, slot);
        }

        protected FrameSlot getSlot(final Symbol name) { return (FrameSlot) namedSlots.valAt(name); }

        @Override
        protected Expr get(final Symbol name) {
            final FrameSlot slot = getSlot(name);
            return (slot != null) ? LocalUseNodeGen.create(slot) : getFrameRoot().parent.get(name);
        }

        @Override
        protected Optional<Var> macroVar(final Symbol name) {
            return namedSlots.containsKey(name) ? Optional.empty() : getFrameRoot().parent.macroVar(name);
        }
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
        protected MethodEnv getFrameRoot() { return this; }

        private FrameSlot addSlot(final FrameSlotKind slotKind) {
            return frameDescriptor.addFrameSlot(localsCount++, slotKind);
        }

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

            final Object form_ = macroexpand1(locals, coll);
            // NOTE: Use recursion so that runaway macroexpansion causes a stack overflow:
            if (form_ != form) { return analyze(locals, ctx, form_); }

            if (Util.equiv(coll.first(), DO)) {
                return analyzeDo(locals, ctx, coll.next());
            } else if (Util.equiv(coll.first(), IF)) {
                return analyzeIf(locals, ctx, coll.next());
            } else if (Util.equiv(coll.first(), THROW)) {
                return analyzeThrow(locals, coll.next());
            } else if (Util.equiv(coll.first(), TRY)) {
                return analyzeTry(locals, coll.next());
            } else if (Util.equiv(coll.first(), LOCKING)) {
                return analyzeLocking(locals, coll.next());
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
            } else if (Util.equiv(coll.first(), SET_BANG_)) {
                return analyzeAssign(locals, coll.next());
            } else if (coll.count() > 0) {
                return analyzeCall(locals, coll.first(), coll.next());
            } else {
                throw new RuntimeException("TODO: analyze " + form);
            }
        } else if (form instanceof IPersistentVector) {
            return analyzeVector(locals, (IPersistentVector) form);
        } else if (form instanceof IPersistentMap) {
            return analyzeMap(locals, (IPersistentMap) form);
        } else if (form == null
                || form instanceof Boolean
                || form instanceof Long
                || /*HACK:*/ form instanceof Namespace) {
            return new Const(form);
        } else {
            throw new RuntimeException("TODO: analyze " + form + ": " + form.getClass());
        }
    }

    private static Object macroexpand1(final FrameEnv env, final ISeq form) {
        final Object op = form.first();
        if (!isSpecialForm(op)) { // NOTE: Prevents overriding special forms
            final Optional<Var> optMacroVar = env.macroVar(op);
            if (optMacroVar.isPresent()) {
                final Var macroVar = optMacroVar.get();

                final ISeq args = RT.cons(form, RT.cons(/* FIXME: */ null, form.next()));
                return macroVar.applyTo(args);
            } else if (op instanceof Symbol) {
                final Symbol opSym = (Symbol) op;
                final String name = opSym.getName();

                if (name.charAt(0) == '.') { // (.foo bar baz) -> (. bar foo baz)
                    ISeq args = form.next();
                    if (args != null) {
                        Object receiver = args.first();
                        args = args.next();

                        final Symbol methodName = Symbol.intern(name.substring(1));
                        if (Namespaces.maybeClass(receiver, false) != null) {
                            receiver = ((IObj) RT.list(DO, receiver)).withMeta(RT.map(TAG_KEY, CLASS));
                        }
                        return preserveTag(form, RT.listStar(DOT, receiver, methodName, args));
                    } else {
                        throw new IllegalArgumentException(
                                "Malformed member expression, expecting (.member target ...)");
                    }
                } else if (opSym.getNamespace() != null && Namespaces.namespaceFor(opSym) == null) {
                    // (Foo/bar baz) -> (. Foo bar baz)
                    final Symbol receiver = Symbol.intern(opSym.getNamespace());

                    if (Namespaces.maybeClass(receiver, false) != null) {
                        final Symbol methodName = Symbol.intern(opSym.getName());
                        return preserveTag(form, RT.listStar(DOT, receiver, methodName, form.next()));
                    }
                } else if (name.charAt(name.length() - 1) == '.') { // (Foo. bar) -> (new Foo bar)
                    return RT.listStar(NEW, Symbol.intern(name.substring(0, name.length() - 1)), form.next());
                }
            }
        }

        return form;
    }

    public static Object preserveTag(final ISeq src, final Object dst) {
        final Symbol tag = tagOf(src);
        if (tag != null && dst instanceof IObj) {
            return ((IObj) dst).withMeta((IPersistentMap) RT.assoc(RT.meta(dst), TAG_KEY, tag));
        }
        return dst;
    }

    private static Symbol tagOf(final Object o) {
        final Object tag = RT.get(RT.meta(o), TAG_KEY);
        return (tag instanceof Symbol) ? (Symbol) tag
                : (tag instanceof String) ? Symbol.intern(null, (String) tag)
                : null;
    }

    private static Expr analyzeSymbol(final FrameEnv locals, final Symbol name) {
        // Local?:
        final Expr expr = locals.get(name);
        if (expr != null) {
            return expr;
        }

        // Static field?:
        final String ns = name.getNamespace();
        if (ns != null) {
            final Class<?> klass = Namespaces.maybeClass(Symbol.intern(ns), false);
            if (klass != null) {
                return GetStatic.create(klass, name.getName())
                        .orElseThrow(() -> new RuntimeException(
                                "Unable to find static field: " + name.getName() + " in " + klass
                        ));
            }
        }

        final Object v = Namespaces.resolve(name);
        if (v instanceof Var) { // Global
            return GlobalUseNodeGen.create((Var) v);
        } else if (v instanceof Class) { // Class as value
            return new Const(v);
        } else {
            throw new AssertionError("TODO");
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

    private static Expr analyzeThrow(final FrameEnv locals, final ISeq args) {
        if (args != null) {
            final Object exnForm = args.first();

            if (args.next() == null) {
                return new ThrowNode(analyze(locals, Context.NONTAIL, exnForm));
            } else {
                throw new RuntimeException("Too many arguments to throw");
            }
        } else {
            throw new RuntimeException("Too few arguments to throw");
        }
    }

    private static Expr analyzeTry(final FrameEnv env, ISeq args) {
        final List<Expr> body = new ArrayList<>();
        for (; args != null; args = args.next()) {
            final Object argForm = args.first();

            if (argForm instanceof ISeq) {
                final Object argOp = ((ISeq) argForm).first();
                if (Util.equiv(argOp, CATCH) || Util.equiv(argOp, FINALLY)) { break; }
            }

            body.add(analyze(env, Context.NONTAIL, argForm));
        }

        final List<CatchNode> catches = new ArrayList<>();
        for (; args != null; args = args.next()) {
            final Object argForm = args.first();

            if (argForm instanceof ISeq) {
                final ISeq argSeq = (ISeq) argForm;
                final Object argOp = argSeq.first();

                if (Util.equiv(argOp, CATCH)) {
                    catches.add(analyzeCatch(env, argSeq.next()));
                    continue;
                } else if (Util.equiv(argOp, FINALLY)) {
                    break;
                }
            }

            throw new RuntimeException("Only catch or finally clause can follow catch in try expression");
        }

        Expr finallyExpr = null;
        if (args != null) {
            final Object argForm = args.first();

            if (args.next() == null) {
                final ISeq argSeq = (ISeq) argForm;
                finallyExpr = analyzeDo(env, Context.NONTAIL, argSeq.next());
            } else {
                throw new RuntimeException("finally clause must be last in try expression");
            }
        }

        return TryNode.create(Do.create(body.toArray(new Expr[0])), catches.toArray(new CatchNode[0]), finallyExpr);
    }

    private static CatchNode analyzeCatch(FrameEnv env, ISeq args) {
        if (args != null) {
            final Object classForm = args.first();

            if ((args = args.next()) != null) {
                final Object paramForm = args.first();

                final Class<?> klass = Namespaces.maybeClass(classForm, false);
                if (klass != null) {
                    if (Throwable.class.isAssignableFrom(klass)) {
                        @SuppressWarnings("unchecked") // checked with `isAssignableFrom` directly above
                        final Class<? extends Throwable> catcheeClass = (Class<? extends Throwable>) klass;

                        if (paramForm instanceof Symbol) {
                            final NestedEnv env_ = env.push((Symbol) paramForm, FrameSlotKind.Object);
                            env = env_;
                            return new CatchNode(catcheeClass, env_.topSlot(), analyzeDo(env, Context.NONTAIL, args));
                        } else {
                            throw new IllegalArgumentException("Bad binding form, expected symbol, got: " + paramForm);
                        }
                    } else {
                        throw new RuntimeException(klass + " is not a subclass of Throwable");
                    }
                } else {
                    throw new RuntimeException("Unable to resolve classname: " + classForm);
                }
            }
        }

        throw new RuntimeException("Too few arguments to catch");
    }
    
    private static Expr analyzeLocking(final FrameEnv env, final ISeq args) {
        if (args != null) {
            final Expr lockExpr = analyze(env, Context.NONTAIL, args.first());
            final Expr body = analyzeDo(env, Context.NONTAIL, args.next());
            return new LockingNode(lockExpr, body);
        } else {
            throw new RuntimeException("Too few arguments to locking");
        }
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

    private static Expr analyzeAssign(final FrameEnv locals, ISeq argForms) {
        if (argForms != null) {
            final Object lvalue = argForms.first();

            if ((argForms = argForms.next()) != null) {
                final Object rvalue = argForms.first();

                if (argForms.next() == null) {
                    return analyzeLRValues(locals, lvalue, rvalue);
                } else {
                    throw new RuntimeException("Too many arguments to set!");
                }
            }
        }

        throw new RuntimeException("Too few arguments to set!");
    }

    private static Expr analyzeLRValues(final FrameEnv locals, final Object lForm, final Object rForm) {
        if (lForm instanceof Symbol) {
            return analyzeSymbolLRValues(locals, (Symbol) lForm, rForm);
        } else if (lForm instanceof ISeq) {
            final ISeq lColl = (ISeq) lForm;

            if (Util.equiv(lColl.first(), DOT)) {
                throw new AssertionError("TODO");
            } else {
                throw new RuntimeException("TODO: analyzeLRValues " + lForm);
            }
        } else {
            throw new RuntimeException("Invalid assignment target");
        }
    }

    private static Expr analyzeSymbolLRValues(final FrameEnv locals, final Symbol name, final Object rForm) {
        final Expr expr = locals.get(name);
        if (expr == null) {
            final Object v = Namespaces.resolve(name);
            if (v instanceof Var) {
                return new GlobalSet((Var) v, analyze(locals, Context.NONTAIL, rForm));
            } else {
                throw new AssertionError("TODO");
            }
        } else {
            throw new RuntimeException("Can't set! a local variable: " + name);
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

    private static Expr analyzeVector(final FrameEnv env, final IPersistentVector vec) {
        final Expr[] elems = new Expr[vec.count()];
        boolean constant = true;

        for (int i = 0; i < vec.count(); ++i) {
            final Expr elem = analyze(env, Context.NONTAIL, vec.nth(i));
            elems[i] = elem;
            constant = constant && elem instanceof Const;
        }

        if (vec instanceof IObj && ((IObj) vec).meta() != null) {
            throw new AssertionError("TODO");
        } else if (constant) {
            ITransientCollection constVals = PersistentVector.EMPTY.asTransient();
            for (final Expr elem : elems) {
                constVals = constVals.conj(((Const) elem).getValue());
            }
            return new Const(constVals.persistent());
        } else {
            return new VectorNode(elems);
        }
    }

    // FIXME: Check key uniqueness and optimize with `RT.mapUniqueKeys` based on that:
    private static Expr analyzeMap(final FrameEnv env, final IPersistentMap map) {
        final Expr[] kvs = new Expr[2 * map.count()];
        boolean constant = true;

        ISeq entries = RT.seq(map);
        for (int i = 0, j = 1; entries != null; entries = entries.next(), i += 2, j += 2) {
            final IMapEntry kv = (IMapEntry) entries.first();
            final Expr k = analyze(env, Context.NONTAIL, kv.key());
            final Expr v = analyze(env, Context.NONTAIL, kv.val());
            kvs[i] = k;
            kvs[j] = v;
            constant = constant && k instanceof Const && v instanceof Const;
        }

        if (map instanceof IObj && ((IObj) map).meta() != null) {
            throw new AssertionError("TODO");
        } else if (constant) {
            ITransientMap constMap = PersistentArrayMap.EMPTY.asTransient();
            for (int i = 0, j = 1; j < kvs.length; i += 2, j += 2) {
                constMap = constMap.assoc(((Const) kvs[i]).getValue(), ((Const) kvs[j]).getValue());
            }
            return new Const(constMap.persistent());
        } else {
            return new MapNode(kvs);
        }
    }
}
