package com.deepbeginnings.coiffure.nodes;

import clojure.lang.RT;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class VectorNode extends Expr {
    @Children private Expr[] elems;
    
    public VectorNode(final Expr[] elems) { this.elems = elems; }

    @Override
    public Object execute(final VirtualFrame frame) {
        final Object[] elemVals = new Object[elems.length];
        
        for (int i = 0; i < elems.length; ++i) {
            elemVals[i] = elems[i].execute(frame);
        }
        
        return RT.vector(elemVals);
    }
}
