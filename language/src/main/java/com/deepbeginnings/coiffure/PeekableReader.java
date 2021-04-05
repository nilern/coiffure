package com.deepbeginnings.coiffure;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

// FIXME: Implement rest of Reader properly
public final class PeekableReader extends FilterReader {
    private boolean closed;
    private int lookahead;
    private boolean haveLookahead;

    public PeekableReader(final Reader in) {
        super(in);
        this.closed = false;
        this.lookahead = 0;
        this.haveLookahead = false;
    }

    private void ensureOpen() throws IOException { if (closed) { throw new IOException("Stream closed"); } }

    public int peek() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (!haveLookahead) {
                lookahead = super.read();
                haveLookahead = true;
            }
            return lookahead;
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (haveLookahead) {
                haveLookahead = false;
                return lookahead;
            } else {
                return super.read();
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            super.close();
            closed = true;
        }
    }
}
