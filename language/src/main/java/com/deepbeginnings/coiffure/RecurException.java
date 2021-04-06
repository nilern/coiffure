package com.deepbeginnings.coiffure;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class RecurException extends ControlFlowException {
    public static final RecurException INSTANCE = new RecurException();

    private RecurException() {}
}
