package com.deepbeginnings.coiffure;

import clojure.lang.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

final class Parser {
    private final PeekableReader input;

    public static final Object EOF = new Object();

    private static final Symbol QUOTE = Symbol.intern("quote");
    private static final Symbol VAR = Symbol.intern("var");
    private static final Keyword TAG_KEY = Keyword.intern(null, "tag");

    public static Object read(final PeekableReader input) throws IOException { return new Parser(input).read(); }

    public static Object tryRead(final PeekableReader input) throws IOException { return new Parser(input).tryRead(); }

    private Parser(final PeekableReader input) { this.input = input; }

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
        case '{': return readMap();

        case '"': return readString();
        case ':': return readKeyword();
        case '\'': return readQuoted();

        case '^': return readWithMeta();

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
        while (true) {
            final int c = input.peek();
            if (Character.isWhitespace(c)) {
                input.read();
            } else if (c == ';') {
                input.read(); // skip ';'

                int cc;
                do {
                    cc = input.read();
                } while (cc != '\n' && cc != '\r' && cc != -1);
            } else {
                break;
            }
        }
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

    private String readString() throws IOException {
        input.read(); // discard '"'
        final StringBuilder cs = new StringBuilder();

        while (true) {
            int c = input.read();

            switch (c) {
            case -1: throw new RuntimeException("EOF while reading string");

            case '"': return cs.toString();

            case '\\':
                c = input.read();
                switch (c) {
                case 'n': c = '\n';
                    break;
                case 't': c = '\t';
                    break;
                case 'r': c = '\r';
                    break;
                case 'b': c = '\b';
                    break;
                case 'f': c = '\f';
                    break;

                case '\\': case '"': break;

                case 'u': default: throw new AssertionError("TODO");
                }
            }

            cs.append((char) c);
        }
    }

    private String readIdentifier() throws IOException {
        if (isSymbolStart(input.peek())) {
            final StringBuilder cs = new StringBuilder();
            cs.append((char) input.read());

            while (isSymbolPart(input.peek())) { cs.append((char) input.read()); }

            return cs.toString();
        } else {
            throw new IOException("Invalid symbol start: '" + (char) input.peek() + "'");
        }
    }

    private static boolean isSymbolStart(final int c) { return isSymbolPart(c) && !Character.isDigit(c); }

    private static boolean isSymbolPart(final int c) {
        return c != -1
                && !Character.isWhitespace(c)
                && "()[]{}'#".indexOf(c) == -1;
    }

    private Keyword readKeyword() throws IOException {
        input.read(); // discard ':'
        return Keyword.intern(Symbol.intern(readIdentifier()));
    }

    private IPersistentCollection readList() throws IOException {
        final ArrayList<Object> forms = new ArrayList<>();

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

    private IPersistentMap readMap() throws IOException {
        final List<Object> kvs = new ArrayList<>();

        input.read(); // discard '{'
        while (true) {
            skipWhitespace();

            if (input.peek() == '}') {
                input.read(); // discard '}'
                break;
            } else {
                kvs.add(read()); // k
                kvs.add(read()); // v
            }
        }

        return RT.map(kvs.toArray(new Object[0]));
    }

    private Object readQuoted() throws IOException {
        input.read(); // discard '\''
        return RT.list(QUOTE, read());
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

    private Object readWithMeta() throws IOException {
        input.read(); // discard '^'
        Object meta = read();
        final Object v = read();

        meta = (meta instanceof Symbol || meta instanceof String) ? RT.map(TAG_KEY, meta)
                : (meta instanceof Keyword) ? RT.map(meta, true)
                : meta;

        if (meta instanceof IPersistentMap) {
            if (v instanceof IReference) {
                return ((IReference) v).resetMeta((IPersistentMap) meta);
            } else if (v instanceof IObj) {
                final IObj obj = (IObj) v;

                Object vMeta = obj.meta();
                for (ISeq kvs = RT.seq(meta); kvs != null; kvs = kvs.next()) {
                    final IMapEntry kv = (IMapEntry) kvs.first();
                    vMeta = RT.assoc(vMeta, kv.key(), kv.val());
                }
                return obj.withMeta((IPersistentMap) vMeta);
            } else {
                throw new RuntimeException("Cannot apply metadata to " + v);
            }
        } else {
            throw new IllegalArgumentException("Metadata must be Symbol, Keyword, String or Map");
        }
    }
}
