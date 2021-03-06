package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.TruffleFile;

import java.nio.charset.Charset;

public final class FileTypeDetector implements TruffleFile.FileTypeDetector {
    @Override
    public String findMimeType(final TruffleFile file) {
        final String name = file.getName();
        return (name != null && (name.endsWith(".clj") || name.endsWith(".cljc")))
                ? Language.MIME_TYPE
                : null;
    }

    @Override
    public Charset findEncoding(final TruffleFile file) {
        return null;
    }
}
