package com.deepbeginnings.coiffure;

final class Analyzer {
    public static Expr analyze(Object form) {
        if (form instanceof Long) {
            return Const.create(form);
        } else {
            throw new RuntimeException("TODO");
        }
    }
}
