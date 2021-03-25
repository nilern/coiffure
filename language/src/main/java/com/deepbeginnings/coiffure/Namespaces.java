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

class Namespaces {
    private Namespaces() { throw new AssertionError(); } // "module" class; only static members

    private static final Symbol NS = Symbol.intern("ns");
    private static final Symbol IN_NS = Symbol.intern("in-ns");
    private static final Namespace CLOJURE_NS = Namespace.findOrCreate(Symbol.intern("clojure.core"));
    private static final Var IN_NS_VAR = Var.intern(CLOJURE_NS, NS, false);
    private static final Var NS_VAR = Var.intern(CLOJURE_NS, IN_NS, false);

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
}
