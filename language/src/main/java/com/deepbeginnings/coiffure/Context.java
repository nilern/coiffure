package com.deepbeginnings.coiffure;

import clojure.lang.RT;
import clojure.lang.Var;

import com.oracle.truffle.api.TruffleLanguage;

final class Context {
    private static TruffleLanguage.Env env;

    public Context(final TruffleLanguage.Env env) {
        this.env = env;

        // HACK: RT.init() does not work yet, so:
        Var.pushThreadBindings(RT.mapUniqueKeys(RT.CURRENT_NS, RT.CURRENT_NS.deref()));
    }
}

