package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.FrameDescriptor;

final class MethodNode extends RootNode {
    final int minArity;
    final boolean isVariadic;

    public MethodNode(final Language lang, final FrameDescriptor locals, final int minArity, final boolean isVariadic,
                      final Expr body
    ) {
        super(lang, locals, body);
        this.minArity = minArity;
        this.isVariadic = isVariadic;
    }
}
