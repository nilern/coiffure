package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.TruffleLanguage;

public final class Context {
    public static TruffleLanguage.Env env;

    public Context(TruffleLanguage.Env env) { this.env = env; }
}

