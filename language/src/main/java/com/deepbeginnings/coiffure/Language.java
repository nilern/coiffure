package com.deepbeginnings.coiffure;

import clojure.lang.*;

import com.deepbeginnings.coiffure.nodes.MethodNode;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@TruffleLanguage.Registration(id = Language.ID, name = "Clojure", implementationName = "Coiffure",
        defaultMimeType = Language.MIME_TYPE, characterMimeTypes = Language.MIME_TYPE,
        contextPolicy = TruffleLanguage.ContextPolicy.EXCLUSIVE, fileTypeDetectors = FileTypeDetector.class)
public final class Language extends TruffleLanguage<Context> {
    public static final String ID = "coiffure";
    public static final String MIME_TYPE = "application/clojure";

    static Language getCurrentLanguage() { return getCurrentLanguage(Language.class); }

    @Override
    protected Context createContext(final Env env) {
        return new Context(env);
    }

    @Override
    protected CallTarget parse(final ParsingRequest request) throws Exception {
        final Source source = request.getSource();

        final List<String> argNames = request.getArgumentNames();
        final Object[] args = new Object[argNames.size()];
        {
            int i = 0;
            for (final String argName : argNames) {
                args[i++] = Symbol.intern(argName);
            }
        }

        final PeekableReader reader = new PeekableReader(source.getReader());

        final List<Object> forms = new ArrayList<>();
        while (true) {
            final Object form = Parser.tryRead(reader);
            if (form == Parser.EOF) { break; }
            forms.add(form);
        }

        IPersistentCollection method = PersistentList.EMPTY;
        for (final ListIterator<Object> formsIt = forms.listIterator(forms.size()); formsIt.hasPrevious(); ) {
            method = method.cons(formsIt.previous());
        }
        method = method.cons(RT.vector(args));

        final MethodNode methodNode = Analyzer.analyzeToplevel((ISeq) method);
        return Truffle.getRuntime().createCallTarget(methodNode);
    }
}
