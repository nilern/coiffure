import com.oracle.truffle.api.TruffleLanguage

class Coiffure : TruffleLanguage<Unit>() {
    override fun createContext(env: Env?): Unit = Unit
}
