package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;

final class ArgUse extends Expr {
    final int index;
    
    public ArgUse(final int index) { this.index = index; }
    
    @Override
    public Object execute(final VirtualFrame frame) { return frame.getArguments()[index]; }
}
