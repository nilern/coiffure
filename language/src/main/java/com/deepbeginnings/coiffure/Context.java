package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.TruffleLanguage;

final class Context {
    private static TruffleLanguage.Env env;

    public Context(final TruffleLanguage.Env env) { this.env = env; }
}

