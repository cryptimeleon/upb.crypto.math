package de.upb.crypto.math.swante;

import de.upb.crypto.math.interfaces.structures.Group;
import de.upb.crypto.math.interfaces.structures.GroupElement;
import de.upb.crypto.math.interfaces.structures.PowProductExpression;
import de.upb.crypto.math.structures.ec.AbstractEllipticCurvePoint;
import de.upb.crypto.math.structures.zn.Zp;
import de.upb.crypto.math.swante.powproducts.MyArrayPowProductWithFixedBases;
import de.upb.crypto.math.swante.powproducts.MyFastPowProductWithoutCaching;
import de.upb.crypto.math.swante.powproducts.MySimultaneous2wAryPowProduct;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.upb.crypto.math.swante.misc.pln;

public class MultiExponentiationTests {
    
//    MyShortFormWeierstrassCurveParameters parameters = MyShortFormWeierstrassCurveParameters.createSecp256r1CurveParameters();
//    MyProjectiveCurve curve = new MyProjectiveCurve(parameters);
//    BigInteger p = parameters.p;
    BigInteger p = BigInteger.valueOf(11);
    Group curve = new Zp(p).asUnitGroup();
    
    
    @Test
    public void testCorrectness() {
        int numBases = 2;
        GroupElement[] bases = IntStream.range(0, numBases).mapToObj(it -> curve.getUniformlyRandomElement()).toArray(GroupElement[]::new);
        BigInteger[] exponents = IntStream.range(0, numBases).mapToObj(it -> misc.randBig(p)).toArray(BigInteger[]::new);
        exponents[0] = BigInteger.valueOf(2);
        exponents[1] = BigInteger.valueOf(3);
        PowProductExpression originalPowProductExpression = new PowProductExpression(curve);
        pln(bases);
        pln(exponents);
        GroupElement expected = curve.getNeutralElement();
        for (int i = 0; i < numBases; i++) {
            GroupElement base = bases[i];
            BigInteger e = exponents[i];
            expected = expected.op(base.pow(e));
            originalPowProductExpression.op(base, e);
        }
        Assert.assertEquals(expected, curve.evaluate(originalPowProductExpression));
        Assert.assertEquals(expected, new MyArrayPowProductWithFixedBases(bases).evaluate(exponents));
        Assert.assertEquals(expected, new MyFastPowProductWithoutCaching(bases).evaluate(exponents));
        Assert.assertEquals(expected, new MySimultaneous2wAryPowProduct(bases, 3).evaluate(exponents));
    }
    
    @Test
    public void testPerformance() {
        int numIterations = 200;
        int numBases = 10;
        GroupElement[] bases = IntStream.range(0, numBases).mapToObj(it -> curve.getUniformlyRandomElement()).toArray(GroupElement[]::new);
        BigInteger[] exponents = IntStream.range(0, numBases).mapToObj(it -> misc.randBig(p)).toArray(BigInteger[]::new);
        PowProductExpression originalPowProductExpression = new PowProductExpression(curve);
        pln(bases);
        for (int i = 0; i < numBases; i++) {
            GroupElement base = bases[i];
            BigInteger e = exponents[i];
            originalPowProductExpression.op(base, e);
        }
        
        pln("==========================");
        pln(String.format("#iterations=%d, #bases=%d", numIterations, numBases));
        misc.tick();
        for (int i = 0; i < numIterations; i++) {
            curve.evaluate(originalPowProductExpression);
        }
        pln(String.format("original power product -> %.2f ms", misc.tick()));
        misc.tick();
        MyArrayPowProductWithFixedBases my1 = new MyArrayPowProductWithFixedBases(bases);
        for (int i = 0; i < numIterations; i++) {
            my1.evaluate(exponents);
        }
        pln(String.format("simple array multi exponentiation -> %.2f ms", misc.tick()));
        misc.tick();
        MyArrayPowProductWithFixedBases my2 = new MyFastPowProductWithoutCaching(bases);
        for (int i = 0; i < numIterations; i++) {
            my2.evaluate(exponents);
        }
        pln(String.format("fast multi exponentiation without caching -> %.2f ms", misc.tick()));
    }
}