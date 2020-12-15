package de.upb.crypto.math.pairings.debug;

import de.upb.crypto.math.interfaces.structures.group.impl.GroupImpl;
import de.upb.crypto.math.interfaces.structures.group.impl.GroupElementImpl;
import de.upb.crypto.math.serialization.BigIntegerRepresentation;
import de.upb.crypto.math.serialization.ObjectRepresentation;
import de.upb.crypto.math.serialization.Representation;
import de.upb.crypto.math.serialization.StringRepresentation;
import de.upb.crypto.math.serialization.annotations.v2.ReprUtil;
import de.upb.crypto.math.serialization.annotations.v2.Represented;
import de.upb.crypto.math.structures.groups.exp.MultiExpTerm;
import de.upb.crypto.math.structures.groups.exp.Multiexponentiation;
import de.upb.crypto.math.structures.groups.exp.SmallExponentPrecomputation;
import de.upb.crypto.math.structures.zn.Zn;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A group used for debugging purposes. Really fast, but DLOG is trivial in this group.
 * <p>
 * Concretely, the group is (Zn, +).
 * This group does support a bilinear map, namely e(a,b) = a*b.
 */
public class DebugGroupImpl implements GroupImpl {
    @Represented
    protected String name;
    @Represented
    protected Zn zn;

    @Represented
    protected Boolean enableExpCounting;
    @Represented
    protected Boolean enableMultiExpCounting;
    protected long numInversions;
    protected long numOps;
    protected long numSquarings;
    protected long numExps;

    /**
     * Number of retrieved representations for elements of this group.
     */
    protected long numRetrievedRepresentations;
    /**
     * Contains number of terms for each multi-exponentiation performed.
     */
    protected List<Integer> multiExpTermNumbers;

    /**
     * Instantiates the debug group (Zn,+).
     *
     * @param name a unique name for this group. Group operations only work between Groups with the same name
     *             (and same n)
     * @param n    the size of Zn
     */
    public DebugGroupImpl(String name, BigInteger n) {
        this(name, n, false, false);
    }

    /**
     * Instantiates the debug group (Zn,+)
     *
     * @param name a unique name for this group. Group operations only work between Groups with the same name
     *             (and same n)
     * @param n    the size of Zn
     * @param enableExpCounting if {@code true}, {@link de.upb.crypto.math.structures.groups.lazy.LazyGroup}
     *                          will count exponentiations as single units, else contained ops will be counted
     * @param enableMultiExpCounting if {@code true}, {@link de.upb.crypto.math.structures.groups.lazy.LazyGroup}
     *                               will track multi-exponentiations, else contained ops will be counted
     */
    public DebugGroupImpl(String name, BigInteger n, boolean enableExpCounting, boolean enableMultiExpCounting) {
        zn = new Zn(n);
        this.name = name;
        this.enableExpCounting = enableExpCounting;
        this.enableMultiExpCounting = enableMultiExpCounting;
        numInversions = 0;
        numOps = 0;
        numSquarings = 0;
        numExps = 0;
        multiExpTermNumbers = new LinkedList<>();
        numRetrievedRepresentations = 0;
    }

    public DebugGroupImpl(Representation repr) {
        new ReprUtil(this).deserialize(repr);
        numInversions = 0;
        numOps = 0;
        numSquarings = 0;
        numExps = 0;
        multiExpTermNumbers = new LinkedList<>();
        numRetrievedRepresentations = 0;
    }

    @Override
    public Representation getRepresentation() {
        return ReprUtil.serialize(this);
    }

    @Override
    public GroupElementImpl getNeutralElement() {
        return wrap(zn.getZeroElement());
    }

    @Override
    public GroupElementImpl getUniformlyRandomElement() throws UnsupportedOperationException {
        return wrap(zn.getUniformlyRandomElement());
    }

    @Override
    public GroupElementImpl getUniformlyRandomNonNeutral() throws UnsupportedOperationException {
        return wrap(zn.getUniformlyRandomNonzeroElement());
    }

    @Override
    public GroupElementImpl getElement(Representation repr) {
        return wrap(zn.getElement(repr));
    }

    @Override
    public GroupElementImpl getGenerator() throws UnsupportedOperationException {
        return wrap(zn.getOneElement());
    }

    @Override
    public Optional<Integer> getUniqueByteLength() {
        return zn.getUniqueByteLength();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        DebugGroupImpl that = (DebugGroupImpl) other;
        return Objects.equals(name, that.name)
                && Objects.equals(zn, that.zn)
                && Objects.equals(enableExpCounting, that.enableExpCounting)
                && Objects.equals(enableMultiExpCounting, that.enableMultiExpCounting);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isCommutative() {
        return true;
    }

    @Override
    public BigInteger size() throws UnsupportedOperationException {
        return zn.size();
    }

    @Override
    public boolean hasPrimeSize() throws UnsupportedOperationException {
        return zn.hasPrimeSize();
    }

    @Override
    public boolean implementsOwnExp() {
        return enableExpCounting;
    }

    @Override
    public GroupElementImpl exp(GroupElementImpl base, BigInteger exponent, SmallExponentPrecomputation precomputation) {
        return base.pow(exponent); // this method already counts the exponentiation
    }

    @Override
    public boolean implementsOwnMultiExp() {
        return enableMultiExpCounting;
    }

    @Override
    public GroupElementImpl multiexp(Multiexponentiation mexp) {
        DebugGroupElementImpl result = (DebugGroupElementImpl) mexp.getConstantFactor().orElse(getNeutralElement());
        for (MultiExpTerm term : mexp.getTerms()) {
            // Use methods where we can disable counting since we only want to count the multi-exponentiation here
            result = (DebugGroupElementImpl) result
                    .op(((DebugGroupElementImpl) term.getBase()).pow(term.getExponent(),false), false);
        }
        addMultiExpBaseNumber(mexp.getTerms().size());
        return result;
    }

    /**
     * Wraps an RingAdditiveGroupElement into a DebugGroupElement
     */
    public DebugGroupElementImpl wrap(Zn.ZnElement elem) {
        return new DebugGroupElementImpl(this, elem);
    }

    @Override
    public double estimateCostInvPerOp() {
        return 1.6;
    }

    protected void incrementNumOps() {
        ++numOps;
    }

    protected void incrementNumInversions() {
        ++numInversions;
    }

    protected void incrementNumSquarings() {
        ++numSquarings;
    }

    protected void incrementNumExps() {
        ++numExps;
    }

    protected void addMultiExpBaseNumber(int numTerms) {
        if (numTerms > 1) {
            multiExpTermNumbers.add(numTerms);
        }
    }

    protected void incrementNumRetrievedRepresentations() {
        ++numRetrievedRepresentations;
    }

    public long getNumInversions() {
        return numInversions;
    }

    public long getNumOps() {
        return numOps;
    }

    public long getNumSquarings() {
        return numSquarings;
    }

    public long getNumExps() { return numExps; }

    public List<Integer> getMultiExpTermNumbers() {
        return multiExpTermNumbers;
    }

    public long getNumRetrievedRepresentations() {
        return numRetrievedRepresentations;
    }

    public void resetOpsCounter() {
        numOps = 0;
    }

    public void resetInvsCounter() {
        numInversions = 0;
    }

    public void resetSquaringsCounter() {
        numSquarings = 0;
    }

    public void resetExpsCounter() { numExps = 0; }

    public void resetMultiExpTermNumbers() { multiExpTermNumbers = new LinkedList<>(); }

    public void resetRetrievedRepresentationsCounter() {
        numRetrievedRepresentations = 0;
    }

    public void resetCounters() {
        resetOpsCounter();
        resetInvsCounter();
        resetSquaringsCounter();
        resetExpsCounter();
        resetMultiExpTermNumbers();
        resetRetrievedRepresentationsCounter();
    }

}
