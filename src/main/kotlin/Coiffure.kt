import com.oracle.truffle.api.TruffleLanguage

@TruffleLanguage.Registration(id = "coiffure", name = "Clojure", implementationName = "Coiffure",
    defaultMimeType = Coiffure.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED, fileTypeDetectors = [FileTypeDetector::class])
class Coiffure : TruffleLanguage<Context>() {
    companion object {
        const val MIME_TYPE = "application/clojure"
    }
    
    override fun createContext(env: Env?): Context = Context(env!!)
}
