package org.cryptimeleon.math.structures.groups.counting;

import org.cryptimeleon.math.serialization.Representation;
import org.cryptimeleon.math.serialization.annotations.ReprUtil;
import org.cryptimeleon.math.serialization.annotations.Represented;
import org.cryptimeleon.math.structures.groups.GroupElement;
import org.cryptimeleon.math.structures.groups.HashIntoGroup;
import org.cryptimeleon.math.structures.groups.lazy.HashIntoLazyGroup;

import java.util.Objects;

/**
 * Allows hashing a byte array to {@link CountingGroup}.
 */
public class HashIntoCountingGroup implements HashIntoGroup {

    @Represented
    private HashIntoLazyGroup totalHashIntoGroup;
    @Represented
    private HashIntoLazyGroup expMultiExpHashIntoGroup;

    public HashIntoCountingGroup(HashIntoLazyGroup totalHashIntoStructure,
                                 HashIntoLazyGroup expMultiExpHashIntoStructure) {
        this.totalHashIntoGroup = totalHashIntoStructure;
        this.expMultiExpHashIntoGroup = expMultiExpHashIntoStructure;
    }

    public HashIntoCountingGroup(Representation repr) {
        new ReprUtil(this).deserialize(repr);
    }

    @Override
    public GroupElement hash(byte[] x) {
        return new CountingGroupElement(
                new CountingGroup(totalHashIntoGroup.getTarget(), expMultiExpHashIntoGroup.getTarget()),
                totalHashIntoGroup.hash(x),
                expMultiExpHashIntoGroup.hash(x)
        );
    }

    @Override
    public Representation getRepresentation() {
        return ReprUtil.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        HashIntoCountingGroup that = (HashIntoCountingGroup) other;
        return Objects.equals(totalHashIntoGroup, that.totalHashIntoGroup)
                && Objects.equals(expMultiExpHashIntoGroup, that.expMultiExpHashIntoGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalHashIntoGroup, expMultiExpHashIntoGroup);
    }
}
