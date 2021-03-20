import com.oracle.truffle.api.frame.VirtualFrame

class Const(private val value: Any?) : Expr() {
    companion object {
        fun create(value: Any?): Expr = Const(value)
    }

    override fun eval(frame: VirtualFrame): Any? = this.value
}
