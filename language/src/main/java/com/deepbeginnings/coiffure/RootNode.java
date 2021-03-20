package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;

final class RootNode extends com.oracle.truffle.api.nodes.RootNode {
    @Child
    Expr body;

    RootNode(Language lang, Expr body) {
        super(lang);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) { return body.eval(frame); }
}
