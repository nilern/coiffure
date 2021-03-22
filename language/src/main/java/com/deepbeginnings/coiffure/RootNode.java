package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;

final class RootNode extends com.oracle.truffle.api.nodes.RootNode {
    @Child
    Expr body;

    public RootNode(Language lang, FrameDescriptor locals, Expr body) {
        super(lang, locals);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) { return body.execute(frame); }
}
