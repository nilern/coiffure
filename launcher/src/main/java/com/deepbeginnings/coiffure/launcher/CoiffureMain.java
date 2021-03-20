package com.deepbeginnings.coiffure.launcher;

import java.io.IOException;
import java.io.InputStreamReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

public final class CoiffureMain {
    private static final String LANG = "coiffure";

    public static void main(String[] args) throws IOException {
        final Context context = Context.newBuilder(LANG)
                .in(System.in).out(System.out)
                .build();

        while (true) {
            System.out.print("> ");
            final Source source = Source.newBuilder(LANG, new InputStreamReader(System.in), "<stdin>").build();
            System.out.println(context.eval(source));
        }
    }
}
