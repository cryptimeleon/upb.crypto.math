package org.cryptimeleon.math.structures.groups.counting;

import org.cryptimeleon.math.serialization.Representation;
import org.cryptimeleon.math.serialization.annotations.ReprUtil;
import org.cryptimeleon.math.serialization.annotations.Represented;
import org.cryptimeleon.math.structures.groups.GroupElementImpl;
import org.cryptimeleon.math.structures.groups.mappings.impl.GroupHomomorphismImpl;

import java.util.Objects;

/**
 * Implements an isomorphism between two {@link CountingGroupImpl}s.
 */
public class CountingIsomorphismImpl implements GroupHomomorphismImpl {
    @Represented
    private CountingGroupImpl src;
    @Represented
    private CountingGroupImpl target;

    public CountingIsomorphismImpl(CountingGroupImpl src, CountingGroupImpl target) {
        this.src = src;
        this.target = target;
    }

    public CountingIsomorphismImpl(Representation repr) {
        new ReprUtil(this).deserialize(repr);
    }

    @Override
    public Representation getRepresentation() {
        return ReprUtil.serialize(this);
    }

    @Override
    public GroupElementImpl apply(GroupElementImpl groupElement) {
        if (!groupElement.getStructure().equals(src))
            throw new IllegalArgumentException("Tried to apply isomorphism on wrong group (argument was from " + groupElement.getStructure() + ")");
        return new CountingGroupElementImpl(target, ((CountingGroupElementImpl) groupElement).elem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountingIsomorphismImpl that = (CountingIsomorphismImpl) o;
        return Objects.equals(src, that.src) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, target);
    }
}
