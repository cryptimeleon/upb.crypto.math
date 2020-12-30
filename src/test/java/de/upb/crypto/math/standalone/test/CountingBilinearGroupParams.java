package de.upb.crypto.math.standalone.test;

import de.upb.crypto.math.pairings.counting.CountingBilinearGroupImpl;
import de.upb.crypto.math.pairings.counting.CountingIsomorphismImpl;
import de.upb.crypto.math.pairings.generic.BilinearGroup;
import de.upb.crypto.math.pairings.counting.CountingBilinearGroup;
import de.upb.crypto.math.pairings.generic.BilinearGroupImpl;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class CountingBilinearGroupParams {

    public static List<StandaloneTestParams> get() {
        BilinearGroup group = new CountingBilinearGroup(128, BilinearGroup.Type.TYPE_1);
        BilinearGroupImpl groupImpl = new CountingBilinearGroupImpl(128, BilinearGroup.Type.TYPE_1);
        return Arrays.asList(new StandaloneTestParams(group),
                new StandaloneTestParams(group.getG1()),
                new StandaloneTestParams(group.getHashIntoG1()),
                new StandaloneTestParams(group.getHomomorphismG2toG1()),
                new StandaloneTestParams(groupImpl),
                new StandaloneTestParams(groupImpl.getG1()),
                new StandaloneTestParams(groupImpl.getHashIntoG1()),
                new StandaloneTestParams(groupImpl.getHomomorphismG2toG1())
        );
    }
}
