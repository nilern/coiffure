package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;

import java.util.List;

@TruffleLanguage.Registration(id = Language.ID, name = "Clojure", implementationName = "Coiffure",
        defaultMimeType = Language.MIME_TYPE, characterMimeTypes = Language.MIME_TYPE,
        contextPolicy = TruffleLanguage.ContextPolicy.EXCLUSIVE, fileTypeDetectors = FileTypeDetector.class)
public final class Language extends TruffleLanguage<Context> {
    public static final String ID = "coiffure";
    public static final String MIME_TYPE = "application/clojure";

    @Override
    protected Context createContext(Env env) {
        return new Context(env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        List<String> argNames = request.getArgumentNames(); // FIXME: ignored

        PeekableReader reader = new PeekableReader(source.getReader());
        
        Expr expr = null;
        FrameDescriptor locals = new FrameDescriptor();
        while (true) {
            Object form = Parser.tryRead(reader);
            if (form == Parser.EOF) { break; }
            expr = Analyzer.analyze(locals, form);
        }

        // FIXME: Make a CallTarget that runs all forms:
        return Truffle.getRuntime().createCallTarget(new RootNode(this, locals, expr));
    }
}
