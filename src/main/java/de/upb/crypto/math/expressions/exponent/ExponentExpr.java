package de.upb.crypto.math.expressions.exponent;

import de.upb.crypto.math.expressions.Expression;
import de.upb.crypto.math.expressions.ValueBundle;
import de.upb.crypto.math.expressions.bool.ExponentEqualityExpr;
import de.upb.crypto.math.structures.zn.Zn;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

public interface ExponentExpr extends Expression {
    BigInteger evaluate();
    Zn.ZnElement evaluate(Zn zn);

    default BigInteger evaluate(Function<String, Expression> substitutionMap) {
        return substitute(substitutionMap).evaluate();
    }

    default Zn.ZnElement evaluate(Zn zn, Function<String, Expression> substitutionMap) {
        return substitute(substitutionMap).evaluate(zn);
    }

    @Override
    ExponentExpr substitute(Function<String, Expression> substitutionMap);

    @Override
    ExponentExpr substitute(ValueBundle variableValues);

    @Override
    ExponentExpr precompute();

    default ExponentExpr negate() {
        return new ExponentNegExpr(this);
    }

    default ExponentExpr add(ExponentExpr other) {
        return new ExponentSumExpr(this, other);
    }

    default ExponentExpr mul(ExponentExpr other) {
        return new ExponentMulExpr(this, other);
    }

    default ExponentEqualityExpr isEqualTo(ExponentExpr other) {
        return new ExponentEqualityExpr(this, other);
    }

    default ExponentEqualityExpr isEqualTo(Zn.ZnElement other) {
        return new ExponentEqualityExpr(this, other.asExponentExpression());
    }

    default ExponentEqualityExpr isEqualTo(BigInteger other) {
        return new ExponentEqualityExpr(this, new ExponentConstantExpr(other));
    }
}
