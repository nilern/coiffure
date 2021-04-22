package com.deepbeginnings.coiffure.nodes;

import clojure.lang.RT;
import com.oracle.truffle.api.frame.VirtualFrame;

public class MapNode extends Expr {
    @Children private Expr[] kvs;
    
    public MapNode(final Expr[] kvs) {
        if ((kvs.length & 1) == 1) {
            throw new IllegalArgumentException("Map literal must contain an even number of forms");
        }
        
        this.kvs = kvs;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        final Object[] kvVals = new Object[kvs.length];
        
        for (int i = 0, j = 1; j < kvs.length; i += 2, j += 2) {
            kvVals[i] = kvs[i].execute(frame);
            kvVals[j] = kvs[j].execute(frame);
        }
        
        return RT.map(kvVals);
    }
}
