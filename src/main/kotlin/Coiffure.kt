import clojure.lang.LispReader
import com.oracle.truffle.api.CallTarget
import clojure.lang.Compiler
import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.TruffleLanguage
import com.oracle.truffle.api.source.Source
import java.io.PushbackReader

@TruffleLanguage.Registration(
    id = "coiffure", name = "Clojure", implementationName = "Coiffure",
    defaultMimeType = Coiffure.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED, fileTypeDetectors = [FileTypeDetector::class]
)
class Coiffure : TruffleLanguage<Context>() {
    companion object {
        const val MIME_TYPE = "application/clojure"

        fun shallow_macroexpand(form: Any?): Any? {
            val form_ = Compiler.macroexpand1(form)
            return if (form_ !== form) {
                shallow_macroexpand(form_)
            } else {
                form_
            }
        }
    }

    override fun createContext(env: Env?): Context = Context(env!!)

    override fun parse(request: ParsingRequest?): CallTarget {
        val source: Source? = request?.source
        val argNames: List<String>? = request?.argumentNames
        
        val reader = PushbackReader(source?.reader)
        var form: Any? = LispReader.read(reader, null)
        
        form = shallow_macroexpand(form)
        
        val expr = Analyzer.analyze(form)

        return Truffle.getRuntime().createCallTarget(RootNode(this, expr))
    }
}
