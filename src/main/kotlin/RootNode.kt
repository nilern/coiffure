import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.RootNode as TruffleRootNode

class RootNode(lang: Coiffure, @Child val body: Expr) : TruffleRootNode(lang) {
    override fun execute(frame: VirtualFrame): Any? = body.eval(frame)
}
