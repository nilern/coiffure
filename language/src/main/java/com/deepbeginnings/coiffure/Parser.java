package com.deepbeginnings.coiffure;

import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentList;
import clojure.lang.Symbol;

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
        skipWhitespace();

        switch (input.peek()) {
        case -1: return EOF;

        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
            return readInt();

        case '(': return readList();

        default: return Symbol.intern(readIdentifier());
        }
    }

    private void skipWhitespace() throws IOException {
        while (Character.isWhitespace(input.peek())) { input.read(); }
    }

    private long readInt() throws IOException {
        long n = 0;
        loop:
        while (true) {
            switch (input.peek()) {
            case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                n = 10 * n + Character.digit(input.read(), 10);
                break;
            default: break loop;
            }
        }
        return n;
    }

    private String readIdentifier() throws IOException {
        if (isSymbolStart(input.peek())) {
            StringBuilder cs = new StringBuilder();
            cs.append(input.read());

            while (isSymbolPart(input.peek())) { cs.append(input.read()); }

            return cs.toString();
        } else {
            throw new IOException("Invalid symbol start: '" + (char) input.peek() + "'");
        }
    }

    private static boolean isSymbolStart(int c) {
        return c != -1 && !Character.isDigit(c) && !Character.isWhitespace(c);
    }

    private static boolean isSymbolPart(int c) { return c != -1 && !Character.isWhitespace(c); }

    private IPersistentCollection readList() throws IOException {
        ArrayList<Object> forms = new ArrayList<>();

        input.read(); // discard '('
        while (true) {
            skipWhitespace();
            
            if (input.peek() == ')') {
                input.read(); // discard ')'
                break;
            } else {
                forms.add(read());
            }
        }

        IPersistentCollection coll = PersistentList.EMPTY;
        for (final ListIterator<Object> formsIt = forms.listIterator(forms.size()); formsIt.hasPrevious(); ) {
            coll = coll.cons(formsIt.previous());
        }
        return coll;
    }
}
