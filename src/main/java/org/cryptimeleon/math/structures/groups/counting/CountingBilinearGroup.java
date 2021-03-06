package org.cryptimeleon.math.structures.groups.counting;

import org.cryptimeleon.math.random.RandomGenerator;
import org.cryptimeleon.math.serialization.Representation;
import org.cryptimeleon.math.serialization.annotations.ReprUtil;
import org.cryptimeleon.math.serialization.annotations.Represented;
import org.cryptimeleon.math.structures.groups.Group;
import org.cryptimeleon.math.structures.groups.HashIntoGroup;
import org.cryptimeleon.math.structures.groups.elliptic.BilinearGroup;
import org.cryptimeleon.math.structures.groups.elliptic.BilinearMap;
import org.cryptimeleon.math.structures.groups.lazy.LazyBilinearGroup;
import org.cryptimeleon.math.structures.groups.mappings.GroupHomomorphism;
import org.cryptimeleon.math.structures.rings.zn.Zn;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;

/**
 * A {@link BilinearGroup} implementing a fast, but insecure pairing over {@link Zn}.
 * Allows for counting group operations and (multi-)exponentiations as well as pairings on the bilinear
 * group level.
 * <p>
 * The counting capability is implemented by wrapping two {@link LazyBilinearGroup}s which contain
 * {@link CountingBilinearGroupImpl}s themselves. All operations are executed in both groups,
 * one counts total group operations and one counts each (multi-)exponentiation as one unit.
 * This allows for tracking both kinds of data.
 *
 */
public class CountingBilinearGroup implements BilinearGroup {

    /**
     * The security level offered by this bilinear group in number of bits.
     */
    @Represented
    protected Integer securityParameter;

    /**
     * The type of pairing this bilinear group should offer.
     */
    @Represented
    protected BilinearGroup.Type pairingType;

    /**
     * The bilinear group responsible for counting total group operations.
     */
    @Represented
    protected LazyBilinearGroup totalBilGroup;

    /**
     * The bilinear group responsible for counting (multi-)exponentiations and group operations outside of those.
     */
    @Represented
    protected LazyBilinearGroup expMultiExpBilGroup;

    /**
     * The underlying bilinear map used for applying the pairing function and counting it.
     */
    protected CountingBilinearMap bilMap;

    /**
     * Initializes this bilinear group with the given security level, pairing type, and group order factoring
     * into the given number of prime factors.
     *
     * @param securityParameter the security level in number of bits
     * @param pairingType the type of pairing that should be offered by this bilinear group
     * @param numPrimeFactors the number of prime factors the group order should have
     */
    public CountingBilinearGroup(int securityParameter, BilinearGroup.Type pairingType, int numPrimeFactors) {
        this.securityParameter = securityParameter;
        this.pairingType = pairingType;

        ArrayList<BigInteger> primeFactors = new ArrayList<>();
        for (int i = 0; i < numPrimeFactors; i++)
            primeFactors.add(RandomGenerator.getRandomPrime(securityParameter));
        
        BigInteger size = primeFactors.stream().reduce(BigInteger.ONE, BigInteger::multiply);
        totalBilGroup = new LazyBilinearGroup(new CountingBilinearGroupImpl(
                securityParameter, pairingType, size, false, false
        ));
        expMultiExpBilGroup = new LazyBilinearGroup(new CountingBilinearGroupImpl(
                securityParameter, pairingType, size, true, true
        ));
        init();
    }

    /**
     *
     * Initializes this bilinear group with the given security level, pairing type and prime group order.
     *
     * @param securityParameter the security level in number of bits
     * @param pairingType the type of pairing that should be offered by this bilinear group
     */
    public CountingBilinearGroup(int securityParameter, BilinearGroup.Type pairingType) {
        this(securityParameter, pairingType, 1);
    }

    /**
     *
     * Initializes this prime order bilinear group of 128 bit size
     *
     * @param pairingType the type of pairing that should be offered by this bilinear group
     */
    public CountingBilinearGroup(BilinearGroup.Type pairingType) {
        this(128, pairingType);
    }

    public CountingBilinearGroup(Representation repr) {
        ReprUtil.deserialize(this, repr);
        init();
    }

    /**
     * Initializes the underlying bilinear map {@link #bilMap}.
     */
    protected void init() {
        bilMap = new CountingBilinearMap(totalBilGroup.getBilinearMap(), expMultiExpBilGroup.getBilinearMap());
    }

    @Override
    public Group getG1() {
        return new CountingGroup(totalBilGroup.getG1(), expMultiExpBilGroup.getG1());
    }

    @Override
    public Group getG2() {
        return new CountingGroup(totalBilGroup.getG2(), expMultiExpBilGroup.getG2());
    }

    @Override
    public Group getGT() {
        return new CountingGroup(totalBilGroup.getGT(), expMultiExpBilGroup.getGT());
    }

    @Override
    public BilinearMap getBilinearMap() {
        return bilMap;
    }

    @Override
    public GroupHomomorphism getHomomorphismG2toG1() throws UnsupportedOperationException {
        if (pairingType != Type.TYPE_1 && pairingType != Type.TYPE_2)
            throw new UnsupportedOperationException("Didn't require existence of a group homomorphism");
        return new CountingHomomorphism(
                totalBilGroup.getHomomorphismG2toG1(),
                expMultiExpBilGroup.getHomomorphismG2toG1()
        );
    }

    @Override
    public HashIntoGroup getHashIntoG1() throws UnsupportedOperationException {
        return new HashIntoCountingGroup(totalBilGroup.getHashIntoG1(), expMultiExpBilGroup.getHashIntoG1());
    }

    @Override
    public HashIntoGroup getHashIntoG2() throws UnsupportedOperationException {
        return new HashIntoCountingGroup(totalBilGroup.getHashIntoG2(), expMultiExpBilGroup.getHashIntoG2());

    }

    @Override
    public HashIntoGroup getHashIntoGT() throws UnsupportedOperationException {
        return new HashIntoCountingGroup(totalBilGroup.getHashIntoGT(), expMultiExpBilGroup.getHashIntoGT());

    }

    @Override
    public Integer getSecurityLevel() {
        return securityParameter;
    }

    @Override
    public Type getPairingType() {
        return pairingType;
    }

    @Override
    public Representation getRepresentation() {
        return ReprUtil.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        CountingBilinearGroup that = (CountingBilinearGroup) other;
        return Objects.equals(totalBilGroup, that.totalBilGroup)
                && Objects.equals(expMultiExpBilGroup, that.expMultiExpBilGroup)
                && Objects.equals(bilMap, that.bilMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalBilGroup, expMultiExpBilGroup, bilMap);
    }

    /**
     * Returns the number of pairings computed in this bilinear group.
     */
    public long getNumPairings() {
        return bilMap.getNumPairings();
    }

    /**
     * Resets pairing counter.
     */
    public void resetNumPairings() {
        bilMap.resetNumPairings();
    }

    /**
     * Resets all counters, including the ones in groups G1, G2, GT.
     */
    public void resetCounters() {
        resetNumPairings();
        ((CountingGroup) getG1()).resetCounters();
        ((CountingGroup) getG2()).resetCounters();
        ((CountingGroup) getGT()).resetCounters();
    }

    /**
     * Returns a string with all count data formatted for printing.
     */
    public String formatCounterData() {
        return bilMap.formatCounterData();
    }
}
