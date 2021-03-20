import com.oracle.truffle.api.TruffleLanguage

class Coiffure : TruffleLanguage<Context>() {
    override fun createContext(env: Env?): Context = Context(env!!)
}
