import java.io.InputStreamReader
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source

fun main(args: Array<String>) {
    val context = Context.newBuilder("coiffure").`in`(System.`in`).`out`(System.`out`).build()

    // val reader = PushbackReader(InputStreamReader(System.`in`))

    while (true) {
        print("> ")
        val source = Source.newBuilder("coiffure", InputStreamReader(System.`in`), "<stdin>").build()
        println(context.eval(source))

        /*var form: Any? = LispReader.read(reader, null)
        form = shallow_macroexpand(form)
        println(form.toString() + " : " + form?.javaClass)*/
    }
}
