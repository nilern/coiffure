package com.deepbeginnings.coiffure.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class Expr extends Node {
    abstract Object execute(VirtualFrame frame);
}
