package de.upb.crypto.math.expressions.bool;

import de.upb.crypto.math.expressions.Expression;
import de.upb.crypto.math.expressions.Substitution;

import java.util.function.Consumer;

/**
 * A {@link BooleanExpression} representing a constant Boolean value.
 */
public class BoolConstantExpr implements BooleanExpression {
    /**
     * The constant Boolean value stored in this {@code BoolConstantExpr}.
     */
    protected final boolean value;

    public BoolConstantExpr(boolean value) {
        this.value = value;
    }

    @Override
    public BooleanExpression substitute(Substitution substitutions) {
        return this;
    }

    @Override
    public Boolean evaluate(Substitution substitutions) {
        return value;
    }

    @Override
    public void forEachChild(Consumer<Expression> action) {
        //Nothing to do
    }
}