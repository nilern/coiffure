package com.deepbeginnings.coiffure.launcher;

import java.io.IOException;
import java.io.StringReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

public final class CoiffureMain {
    private static final String LANG = "coiffure";

    public static void main(String[] args) throws IOException {
        final Context context = Context.newBuilder(LANG)
                .in(System.in).out(System.out)
                .build();

        while (true) {
            try {
                System.out.print("> ");
                final String line = System.console().readLine();
                final Source source = Source.newBuilder(LANG, new StringReader(line), "<stdin>").build();
                System.out.println(context.eval(source));
            } catch (IOException exn) {
                System.out.println(exn);
                break;
            }
        }
    }
}
