import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.Node

abstract class Expr : Node() {
    abstract fun eval(frame: VirtualFrame): Any?
}
