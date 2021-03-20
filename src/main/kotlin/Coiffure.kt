/*import clojure.lang.LispReader
import com.oracle.truffle.api.CallTarget*/
import com.oracle.truffle.api.TruffleLanguage
/*import com.oracle.truffle.api.source.Source
import java.io.PushbackReader
import java.io.StringReader*/

@TruffleLanguage.Registration(id = "coiffure", name = "Clojure", implementationName = "Coiffure",
    defaultMimeType = Coiffure.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED, fileTypeDetectors = [FileTypeDetector::class])
class Coiffure : TruffleLanguage<Context>() {
    companion object {
        const val MIME_TYPE = "application/clojure"
    }

    override fun createContext(env: Env?): Context = Context(env!!)

    /*override fun parse(request: ParsingRequest?): CallTarget {
        val source: Source? = request?.source
        val argNames: List<String>? = request?.argumentNames
        val reader = PushbackReader(source?.reader)
        val form: Any = LispReader.read(reader, null)

    }*/
}
