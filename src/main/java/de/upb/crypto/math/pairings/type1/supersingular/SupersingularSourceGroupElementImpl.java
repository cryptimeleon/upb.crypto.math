package de.upb.crypto.math.pairings.type1.supersingular;

import de.upb.crypto.math.interfaces.structures.FieldElement;
import de.upb.crypto.math.pairings.generic.PairingSourceGroupElement;

/**
 * An element of the source group (G1 and G2) of the supersingular pairing.
 *
 * @see SupersingularSourceGroupImpl
 */
public class SupersingularSourceGroupElementImpl extends PairingSourceGroupElement {

    public SupersingularSourceGroupElementImpl(SupersingularSourceGroupImpl curve, FieldElement x, FieldElement y) {
        super(curve, x, y);
    }

    /**
     * Instantiates the neutral element
     *
     * @param curve the source curve the element should be on
     */
    public SupersingularSourceGroupElementImpl(SupersingularSourceGroupImpl curve) {
        super(curve);
    }
}