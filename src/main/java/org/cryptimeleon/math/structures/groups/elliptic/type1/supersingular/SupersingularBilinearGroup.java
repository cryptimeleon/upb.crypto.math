package org.cryptimeleon.math.structures.groups.elliptic.type1.supersingular;

import org.cryptimeleon.math.serialization.Representation;
import org.cryptimeleon.math.structures.groups.lazy.LazyBilinearGroup;

/**
 * Offers a less verbose way to instantiate a Supersingular bilinear group which uses lazy evaluation.
 * <p>
 * Essentially just a {@link LazyBilinearGroup} wrapper around {@link SupersingularTateGroupImpl}.
 *
 * @see SupersingularTateGroupImpl
 */
public class SupersingularBilinearGroup extends LazyBilinearGroup {

    public SupersingularBilinearGroup(int securityParameter) {
        super(new SupersingularTateGroupImpl(securityParameter));
    }

    public SupersingularBilinearGroup(Representation repr) {
        super(repr);
    }
}
