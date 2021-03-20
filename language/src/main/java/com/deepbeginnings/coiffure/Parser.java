package com.deepbeginnings.coiffure;

import java.io.IOException;
import java.io.PushbackReader;

class Parser {
    private static final int EOF = -1;
    
    public static Object read(PushbackReader input) throws IOException {
        return new Parser(input).read();
    }
    
    private final PushbackReader input;

    private Parser(PushbackReader input) { this.input = input; }
    
    private Object read() throws IOException {
        int c;

        do {
            c = input.read();
            if (c == EOF) { throw new IOException("EOF while reading"); }
        } while (Character.isWhitespace(c));
        
        if (Character.isDigit(c)) {
            long n = Character.digit(c, 10);
            c = input.read();
            while (Character.isDigit(c)) {
                n = 10 * n + Character.digit(c, 10);
                c = input.read();
            }
            input.unread(c);
            return Long.valueOf(n);
        } else {
            throw new IOException("TODO");
        }
    }
}
