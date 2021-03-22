package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

abstract class Expr extends Node {
    abstract Object execute(VirtualFrame frame);
}
