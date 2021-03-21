package com.deepbeginnings.coiffure;

import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

final class Parser {
    public static final Object EOF = new Object();

    public static Object read(PeekableReader input) throws IOException { return new Parser(input).read(); }

    public static Object tryRead(PeekableReader input) throws IOException { return new Parser(input).tryRead(); }

    private final PeekableReader input;

    private Parser(PeekableReader input) { this.input = input; }

    private Object read() throws IOException {
        final Object form = tryRead();
        if (form != EOF) {
            return form;
        } else {
            throw new IOException("EOF while reading");
        }
    }

    private Object tryRead() throws IOException {
        while (Character.isWhitespace(input.peek())) { input.read(); }

        switch (input.peek()) {
        case -1: return EOF;

        case '(': return readList();

        default:
            if (Character.isDigit(input.peek())) {
                long n = 0;
                while (Character.isDigit(input.peek())) {
                    n = 10 * n + Character.digit(input.read(), 10);
                }
                return n;
            } else {
                throw new IOException("TODO: parser lookahead '" + (char) input.peek() + "'");
            }
        }
    }

    private IPersistentCollection readList() throws IOException {
        ArrayList<Object> forms = new ArrayList<>();

        input.read(); // discard '('
        while (input.peek() != ')') { forms.add(read()); }
        input.read(); // discard ')'

        IPersistentCollection coll = PersistentList.EMPTY;
        for (final ListIterator<Object> formsIt = forms.listIterator(forms.size()); formsIt.hasPrevious(); ) {
            coll = coll.cons(formsIt.previous());
        }
        return coll;
    }
}
