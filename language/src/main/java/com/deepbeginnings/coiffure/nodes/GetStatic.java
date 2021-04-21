package com.deepbeginnings.coiffure.nodes;

import clojure.lang.Reflector;
import clojure.lang.Util;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;

import java.lang.reflect.Field;
import java.util.Optional;

@NodeField(name = "field", type = Field.class)
public abstract class GetStatic extends Expr {
    protected abstract Field getField();

    public static Optional<GetStatic> create(final Class<?> klass, final String fieldName) {
        final Field field = Reflector.getField(klass, fieldName, true);
        return (field != null) ? Optional.of(GetStaticNodeGen.create(field)) : Optional.empty();
    }

    @Specialization
    protected Object getStatic() {
        try {
            return getField().get(null); // OPTIMIZE
        } catch (final IllegalAccessException | IllegalArgumentException exn) {
            throw Util.sneakyThrow(exn);
        }
    }
}
