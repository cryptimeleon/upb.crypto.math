package de.upb.crypto.math.swante.profiling;

import de.upb.crypto.math.interfaces.structures.GroupElement;
import de.upb.crypto.math.structures.ec.AbstractEllipticCurvePoint;
import de.upb.crypto.math.structures.zn.Zp;
import de.upb.crypto.math.swante.*;

import java.math.BigInteger;

import static de.upb.crypto.math.swante.MyExponentiationAlgorithms.*;
import static de.upb.crypto.math.swante.MyUtil.myAssert;
import static de.upb.crypto.math.swante.MyUtil.pln;

public class ThesisSingleExpo {
    public static void main(String[] args) {
        pln("=========================");
        if (args.length == 0) {
            args = "256 projective 10 500 1 1 True BN".split(" ");
        }
        pln(args);
        int bitLength = Integer.parseInt(args[0]);
        MyShortFormWeierstrassCurveParameters parameters = MyShortFormWeierstrassCurveParameters.createSecp192r1CurveParameters();
        if (bitLength == 256) {
            parameters = MyShortFormWeierstrassCurveParameters.createSecp256r1CurveParameters();
        }
        if (args[7].equals("BN")) {
            parameters = MyUtil.createBnWeierstrassCurveGroupParams(bitLength);
        }
        
        Zp zp = new Zp(parameters.p);
        MyShortFormWeierstrassCurve curve = new MyProjectiveCurve(parameters);
        if (args[1].equals("jacobi")) {
            curve = new MyJacobiCurve(parameters);
        }
//        AbstractEllipticCurvePoint g = curve.getGenerator();
//        AbstractEllipticCurvePoint zero = curve.getNeutralElement();
//        myAssert(g.pow(curve.size()).equals(zero));
        int numPoints = Integer.parseInt(args[2]);
        int numIterations = Integer.parseInt(args[3]);
        int windowSize = Integer.parseInt(args[4]);
        int m = (1 << windowSize) - 1;
        int algo = Integer.parseInt(args[5]);
        boolean cacheSmallPowers = false;
        if (args[6].equals("True")) {
            cacheSmallPowers = true;
        }
        AbstractEllipticCurvePoint[] bases = MyUtil.createRandomCurvePoints(curve, numPoints);
        GroupElement[][] precomputedOddPowers = new GroupElement[numPoints][];
        GroupElement[][] precomputedAllPowers = new GroupElement[numPoints][];
        for (int i = 0; i < numPoints; i++) {
            precomputedOddPowers[i] = precomputeSmallOddPowers(bases[i], m);
            precomputedAllPowers[i] = precomputeAllSmallPowers(bases[i], m);
        }
        Zp.ZpElement[] exponentsZp = MyUtil.createRandomZpValues(zp, numPoints);
        BigInteger[] exponents = new BigInteger[numPoints];
        for (int i = 0; i < numPoints; i++) {
            exponents[i] = exponentsZp[i].getInteger();
        }
        
        double startMillis = System.nanoTime() / 1.0e6;
        for (int iter = -numIterations; iter < numIterations; iter++) {
            if (iter == 0) { // start timing only after warmup phase
                startMillis = System.nanoTime() / 1.0e6;
            }
            for (int i = 0; i < numPoints; i++) {
                if (algo == 1) { // normal pow
                    bases[i].pow(exponents[i]);
                } else if (algo == 2) { // 2w ary
                    GroupElement[] smallAllPowers = precomputedAllPowers[i];
                    if (!cacheSmallPowers) {
                        smallAllPowers = precomputeAllSmallPowers(bases[i], m);
                    }
                    powUsing2wAryMethod(bases[i], exponents[i], windowSize, smallAllPowers);
                } else if (algo == 3) { // sliding window pow
                    GroupElement[] smallOddPowers = precomputedOddPowers[i];
                    if (!cacheSmallPowers) {
                        smallOddPowers = precomputeSmallOddPowers(bases[i], m);
                    }
                    powUsingSlidingWindow(bases[i], exponents[i], windowSize, smallOddPowers);
                } else if (algo == 4) { // wNAF
                    GroupElement[] smallOddPowers = precomputedOddPowers[i];
                    if (!cacheSmallPowers) {
                        smallOddPowers = precomputeSmallOddPowers(bases[i], m);
                    }
                    int[] expDigits = MyExponentiationAlgorithms.precomputeExponentDigitsForWNAF(exponents[i], windowSize);
                    MyExponentiationAlgorithms.powSingleWNaf(bases[i], expDigits, smallOddPowers);
                }
            }
        }
        double elapsedMillis = System.nanoTime() / 1.0e6 - startMillis;
        pln("Result: " + elapsedMillis);
    }
}
