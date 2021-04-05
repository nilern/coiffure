/* Originating from code in the original Clojure implementation:
 *
 * Copyright (c) Rich Hickey. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package com.deepbeginnings.coiffure;

import clojure.lang.*;

final class Namespaces {
    private Namespaces() { throw new AssertionError(); } // "module" class; only static members

    private static final Symbol NS = Symbol.intern("ns");
    private static final Symbol IN_NS = Symbol.intern("in-ns");
    private static final Namespace CLOJURE_NS = Namespace.findOrCreate(Symbol.intern("clojure.core"));
    private static final Var IN_NS_VAR = Var.intern(CLOJURE_NS, NS, false);
    private static final Var NS_VAR = Var.intern(CLOJURE_NS, IN_NS, false);
    private final static Var ALLOW_UNRESOLVED_VARS =
            Var.intern(CLOJURE_NS, Symbol.intern("*allow-unresolved-vars*"), false)
                    .setDynamic();

    private static Namespace currentNS() { return (Namespace) RT.CURRENT_NS.deref(); }

    private static Namespace namespaceFor(Symbol sym) { return namespaceFor(currentNS(), sym); }

    private static Namespace namespaceFor(Namespace inns, Symbol sym) {
        //note, presumes non-nil sym.ns
        Symbol nsSym = Symbol.intern(sym.getNamespace());
        Namespace ns = inns.lookupAlias(nsSym);
        return (ns != null) ? ns : Namespace.find(nsSym);
    }

    static Var lookupVar(Symbol sym, boolean internNew) {
        Var var = null;

        //note - ns-qualified vars in other namespaces must already exist
        if (sym.getNamespace() != null) {
            Namespace ns = namespaceFor(sym);
            if (ns == null) { return null; }
            //throw Util.runtimeException("No such namespace: " + sym.ns);
            Symbol name = Symbol.intern(sym.getName());
            if (internNew && ns == currentNS()) {
                var = currentNS().intern(name);
            } else {
                var = ns.findInternedVar(name);
            }
        } else if (sym.equals(NS)) {
            var = NS_VAR;
        } else if (sym.equals(IN_NS)) {
            var = IN_NS_VAR;
        } else {
            //is it mapped?
            Object o = currentNS().getMapping(sym);
            if (o == null) {
                //introduce a new var in the current ns
                if (internNew) { var = currentNS().intern(Symbol.intern(sym.getName())); }
            } else if (o instanceof Var) {
                var = (Var) o;
            } else {
                throw Util.runtimeException("Expecting var, but " + sym + " is mapped to " + o);
            }
        }

        return var;
    }

    static Object resolve(Symbol sym) { return resolveIn(currentNS(), sym, false); }

    private static Object resolveIn(Namespace n, Symbol sym, boolean allowPrivate) {
        //note - ns-qualified vars must already exist
        if (sym.getNamespace() != null) {
            Namespace ns = namespaceFor(n, sym);
            if (ns == null) { throw Util.runtimeException("No such namespace: " + sym.getNamespace()); }

            Var v = ns.findInternedVar(Symbol.intern(sym.getName()));
            if (v == null) {
                throw Util.runtimeException("No such var: " + sym);
            } else if (v.ns != currentNS() && !v.isPublic() && !allowPrivate) {
                throw new IllegalStateException("var: " + sym + " is not public");
            }
            return v;
        } else if (sym.getName().indexOf('.') > 0 || sym.getName().charAt(0) == '[') {
            return RT.classForName(sym.getName());
        } else if (sym.equals(NS)) {
            return NS_VAR;
        } else if (sym.equals(IN_NS)) {
            return IN_NS_VAR;
        } else {
            // HACK(nilern): comment out: if (Util.equals(sym, COMPILE_STUB_SYM.get())) { return COMPILE_STUB_CLASS.get(); }
            Object o = n.getMapping(sym);
            if (o == null) {
                if (RT.booleanCast(ALLOW_UNRESOLVED_VARS.deref())) {
                    return sym;
                } else {
                    throw Util.runtimeException("Unable to resolve symbol: " + sym + " in this context");
                }
            }
            return o;
        }
    }

    public static Class<?> maybeClass(Object form, boolean stringOk) {
        if (form instanceof Class) { return (Class<?>) form; }

        Class<?> c = null;

        if (form instanceof Symbol) {
            Symbol sym = (Symbol) form;
            if (sym.getNamespace() == null) {//if ns-qualified can't be classname
                // HACK(nilern): comment out: if (Util.equals(sym, COMPILE_STUB_SYM.get())) { return (Class) COMPILE_STUB_CLASS.get(); }
                if (sym.getName().indexOf('.') > 0 || sym.getName().charAt(0) == '[') {
                    c = RT.classForNameNonLoading(sym.getName());
                } else {
                    Object o = currentNS().getMapping(sym);
                    if (o instanceof Class) {
                        c = (Class<?>) o;
                    } /* HACK(nilern): comment out: else if (LOCAL_ENV.deref() != null && ((java.util.Map) LOCAL_ENV.deref()).containsKey(form)) {
                        return null;
                    }*/ else {
                        try {
                            c = RT.classForNameNonLoading(sym.getName());
                        } catch (Exception e) {
                            // aargh
                            // leave c set to null -> return null
                        }
                    }
                }
            }
        } else if (stringOk && form instanceof String) {
            c = RT.classForNameNonLoading((String) form);
        }

        return c;
    }
}
