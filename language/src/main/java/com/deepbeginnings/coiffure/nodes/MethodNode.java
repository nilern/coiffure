package com.deepbeginnings.coiffure.nodes;

import com.deepbeginnings.coiffure.Language;
import com.oracle.truffle.api.frame.FrameDescriptor;

public final class MethodNode extends RootNode {
    private final int minArity;
    private final boolean isVariadic;

    public MethodNode(final Language lang, final FrameDescriptor locals, final int minArity, final boolean isVariadic,
                      final Expr body
    ) {
        super(lang, locals, body);
        this.minArity = minArity;
        this.isVariadic = isVariadic;
    }

    public int getMinArity() { return minArity; }

    public boolean isVariadic() { return isVariadic; }
}
