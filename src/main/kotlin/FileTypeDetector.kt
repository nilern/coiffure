import com.oracle.truffle.api.TruffleFile
import java.nio.charset.Charset

class FileTypeDetector : TruffleFile.FileTypeDetector {
    override fun findMimeType(file: TruffleFile?): String? {
        val name = file?.name
        return if (name != null && (name.endsWith(".clj") || name.endsWith(".cljc"))) {
            Coiffure.MIME_TYPE
        } else {
            null
        }
    }

    override fun findEncoding(file: TruffleFile?): Charset? = null
}
