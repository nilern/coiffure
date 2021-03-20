package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import clojure.lang.Compiler;
import clojure.lang.LispReader;

import java.io.PushbackReader;
import java.util.List;

@TruffleLanguage.Registration(id = Language.ID, name = "Clojure", implementationName = "Coiffure",
        defaultMimeType = Language.MIME_TYPE, characterMimeTypes = Language.MIME_TYPE,
        contextPolicy = TruffleLanguage.ContextPolicy.EXCLUSIVE, fileTypeDetectors = FileTypeDetector.class)
public final class Language extends TruffleLanguage<Context> {
    public static final String ID = "coiffure";
    public static final String MIME_TYPE = "application/clojure";

    static Object shallow_macroexpand(Object form) {
        Object form_ = Compiler.macroexpand1(form);
        return (form_ != form) ? shallow_macroexpand(form_) : form_;
    }


    @Override
    protected Context createContext(Env env) {
        return new Context(env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        List<String> argNames = request.getArgumentNames();

        PushbackReader reader = new PushbackReader(source.getReader());
        Object form = LispReader.read(reader, null);

        form = shallow_macroexpand(form);

        Expr expr = Analyzer.analyze(form);

        return Truffle.getRuntime().createCallTarget(new RootNode(this, expr));
    }
}
