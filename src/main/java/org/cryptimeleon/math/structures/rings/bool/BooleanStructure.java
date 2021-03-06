package org.cryptimeleon.math.structures.rings.bool;

import org.cryptimeleon.math.random.RandomGenerator;
import org.cryptimeleon.math.serialization.Representation;
import org.cryptimeleon.math.serialization.annotations.ReprUtil;
import org.cryptimeleon.math.structures.rings.Ring;
import org.cryptimeleon.math.structures.rings.RingElement;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Implements a boolean ring structure where addition acts like XOR and multiplication like AND, so \(\mathbb{Z}_2\).
 */
public class BooleanStructure implements Ring {

    // These constructors only exist to appease the standalone tests.
    public BooleanStructure() { }

    public BooleanStructure(Representation repr) { new ReprUtil(this).deserialize(repr);}

    @Override
    public BigInteger sizeUnitGroup() throws UnsupportedOperationException {
        return BigInteger.ONE;
    }

    @Override
    public RingElement getZeroElement() {
        return BooleanElement.FALSE;
    }

    @Override
    public RingElement getOneElement() {
        return BooleanElement.TRUE;
    }

    @Override
    public RingElement restoreElement(Representation repr) {
        return new BooleanElement(repr);
    }

    @Override
    public Optional<Integer> getUniqueByteLength() {
        return Optional.of(1);
    }

    @Override
    public BigInteger size() throws UnsupportedOperationException {
        return BigInteger.valueOf(2);
    }

    @Override
    public RingElement getUniformlyRandomElement() throws UnsupportedOperationException {
        return RandomGenerator.getRandomBit() ? BooleanElement.TRUE : BooleanElement.FALSE;
    }

    @Override
    public BigInteger getCharacteristic() throws UnsupportedOperationException {
        return BigInteger.valueOf(2);
    }

    @Override
    public RingElement getElement(BigInteger i) {
        return i.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO) ? BooleanElement.FALSE : BooleanElement.TRUE;
    }

    @Override
    public boolean isCommutative() {
        return true;
    }

    @Override
    public Representation getRepresentation() {
        return ReprUtil.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        return this.getClass() == other.getClass();
    }

    @Override
    public int hashCode() {
        return this.getClass().toString().hashCode();
    }

}
