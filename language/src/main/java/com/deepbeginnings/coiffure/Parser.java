package com.deepbeginnings.coiffure;

import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentList;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.ListIterator;

final class Parser {
    public static final Object EOF = new Object();

    public static Object read(PushbackReader input) throws IOException { return new Parser(input).read(); }

    public static Object tryRead(PushbackReader input) throws IOException { return new Parser(input).tryRead(); }

    private final PushbackReader input;

    private Parser(PushbackReader input) { this.input = input; }

    private Object read() throws IOException {
        final Object form = tryRead();
        if (form != EOF) {
            return form;
        } else {
            throw new IOException("EOF while reading");
        }
    }

    private Object tryRead() throws IOException {
        int c;

        do {
            c = input.read();
            if (c == -1) { return EOF; }
        } while (Character.isWhitespace(c));

        switch (c) {
        case '(': return readList();
        default:
            if (Character.isDigit(c)) {
                long n = Character.digit(c, 10);
                c = input.read();
                while (Character.isDigit(c)) {
                    n = 10 * n + Character.digit(c, 10);
                    c = input.read();
                }
                if (c != -1) { input.unread(c); }
                return n;
            } else {
                throw new IOException("TODO: " + c);
            }
        }
    }

    private IPersistentCollection readList() throws IOException {
        ArrayList<Object> forms = new ArrayList<>();

        for (int c = input.read(); c != ')'; c = input.read()) {
            if (c != -1) { input.unread(c); }
            forms.add(read());
        }

        IPersistentCollection coll = PersistentList.EMPTY;
        for (final ListIterator<Object> formsIt = forms.listIterator(forms.size()); formsIt.hasPrevious(); ) {
            coll = coll.cons(formsIt.previous());
        }
        return coll;
    }
}
