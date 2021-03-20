import clojure.lang.LispReader
import java.io.InputStreamReader
import java.io.PushbackReader
import clojure.lang.Compiler as ClojureCompiler

/*import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source*/

fun shallow_macroexpand(form: Any?): Any? {
    val form_ = ClojureCompiler.macroexpand1(form)
    return if (form_ !== form) { shallow_macroexpand(form_) } else { form_ }
}

fun main(args: Array<String>) {
    /*val context = Context.newBuilder("coiffure").`in`(System.`in`).`out`(System.`out`).build()
    val source = Source.newBuilder("coiffure", InputStreamReader(System.`in`), "<stdin>").build()*/

    val reader = PushbackReader(InputStreamReader(System.`in`))

    while (true) {
        print("> ")
        var form: Any? = LispReader.read(reader, null)
        form = shallow_macroexpand(form)
        println(form.toString() + " : " + form?.javaClass)
    }
}
