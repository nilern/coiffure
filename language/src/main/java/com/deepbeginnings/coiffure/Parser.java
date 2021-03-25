package com.deepbeginnings.coiffure;

import clojure.lang.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

final class Parser {
    public static final Object EOF = new Object();

    private static final Symbol VAR = Symbol.intern("var");

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
        case '[': return readVector();

        case '#': return readHashy();

        default: return readAtom();
        }
    }

    private Object readHashy() throws IOException {
        input.read(); // discard '#'

        switch (input.peek()) {
        case '\'':
            input.read(); // discard '\''
            return RT.list(VAR, read());

        default: throw new AssertionError("TODO");
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
            cs.append((char) input.read());

            while (isSymbolPart(input.peek())) { cs.append((char) input.read()); }

            return cs.toString();
        } else {
            throw new IOException("Invalid symbol start: '" + (char) input.peek() + "'");
        }
    }

    private static boolean isSymbolStart(int c) { return isSymbolPart(c) && !Character.isDigit(c); }

    private static boolean isSymbolPart(int c) {
        return c != -1
                && !Character.isWhitespace(c)
                && "()[]#".indexOf(c) == -1;
    }

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

    private IPersistentCollection readVector() throws IOException {
        ITransientCollection coll = PersistentVector.EMPTY.asTransient();

        input.read(); // discard '['
        while (true) {
            skipWhitespace();

            if (input.peek() == ']') {
                input.read(); // discard ']'
                break;
            } else {
                coll = coll.conj(read());
            }
        }

        return coll.persistent();
    }

    private Object readAtom() throws IOException {
        final String name = readIdentifier();

        switch (name) {
        case "nil": return null;
        case "true": return Boolean.TRUE;
        case "false": return Boolean.FALSE;
        default: return Symbol.intern(name);
        }
    }
}
