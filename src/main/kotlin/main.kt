import clojure.lang.LispReader
import java.io.InputStreamReader
import java.io.PushbackReader

fun main(args: Array<String>) {
    val reader = PushbackReader(InputStreamReader(System.`in`))

    while (true) {
        print("> ")
        val form: Any = LispReader.read(reader, null)
        println(form)
    }
}
