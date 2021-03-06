package org.cryptimeleon.math.hash;

import org.cryptimeleon.math.hash.impl.ByteArrayAccumulator;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * A {@code ByteAccumulator} takes a (large) byte-string x as input and outputs a byte array
 * representing f(x) for some function f.
 * The function f depends on the concrete instantiation of this class. It can be, for example, the identity function
 * {@link ByteArrayAccumulator}.
 * <p>
 * The input x can be given to the ByteAccumulator in substrings, so if x = x_1 || x_2 (|| denotes concatenation), then
 * after calling {@code append(x_1)} and {@code append(x_2)}, {@code extractBytes()} will output f(x) = f(x_1 || x_2).
 * <p>
 * Usually, this class will be used in the context of the {@link UniqueByteRepresentable} interface.
 * To that end, it contains some helper methods to make it easier to insert certain x into this.
 */
public abstract class ByteAccumulator implements Comparable<ByteAccumulator> {
    public static byte SEPARATOR = (byte) '\\';

    /**
     * Appends bytes to the input x of this accumulator.
     */
    public abstract void append(byte[] bytes);

    /**
     * Extracts f(x) from the accumulator, where x was input by {@code append()} calls.
     *
     * @return f(x)
     */
    public abstract byte[] extractBytes();

    /**
     * Appends the input and then appends a separator byte.
     */
    public void appendAndSeparate(byte[] bytes) {
        append(bytes);
        appendSeperator();
    }

    /**
     * Appends the input and then appends a separator byte.
     */
    public void appendAndSeparate(String str) {
        appendAndSeparate(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Escapes any occurrence of the separator symbol and appends the escaped string
     * to the byte accumulator.
     */
    public void escapeAndAppend(byte[] bytes) {
        new EscapingByteAccumulator(this).append(bytes);
    }

    /**
     * Escapes any occurrence of the separator symbol and appends the escaped string
     * to the byte accumulator.
     */
    public void escapeAndAppend(String str) {
        escapeAndAppend(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Escapes any occurrence of the separator symbol and appends the escaped string
     * to the byte accumulator.
     */
    public void escapeAndAppend(UniqueByteRepresentable ubr) {
        new EscapingByteAccumulator(this).append(ubr);
    }

    /**
     * Escapes the separator symbol, appends the escaped bytes, then appends a separator symbol.
     */
    public void escapeAndSeparate(byte[] bytes) {
        escapeAndAppend(bytes);
        appendSeperator();
    }

    /**
     * Escapes the separator symbol in the given string, appends the escaped bytes, then appends a separator symbol.
     */
    public void escapeAndSeparate(String str) {
        escapeAndSeparate(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Escapes the separator symbol, appends the escaped bytes, then appends a separator symbol.
     */
    public void escapeAndSeparate(UniqueByteRepresentable ubr) {
        escapeAndAppend(ubr);
        appendSeperator();
    }

    /**
     * Appends a special separator symbol to the accumulator.
     */
    public void appendSeperator() {
        this.append(new byte[]{SEPARATOR});
    }

    /**
     * Pads the bytes with leading zero bytes to the desired length and then appends that to the accumulator.
     *
     * @param length the desired length of the appended byte string
     * @param bytes  the bytes to update
     */
    public void appendPadded(int length, byte[] bytes) {
        if (length < bytes.length) {
            throw new IllegalArgumentException("The bytes have a longer length than specified!");
        }
        byte[] paddedBytes = new byte[length];
        System.arraycopy(bytes, 0, paddedBytes, 0, bytes.length);
        for (int i = bytes.length; i < length; i++) {
            paddedBytes[i] = (byte) 0;
        }
        append(paddedBytes);
    }

    /**
     * Updates the {@code ByteAccumulator} with bytes from a {@code UniqueByteRepresentable}.
     */
    public void append(UniqueByteRepresentable ubr) {
        append(ubr.getUniqueByteRepresentation());
    }

    /**
     * Appends a single byte to the accumulator.
     */
    public void append(byte singleByte) {
        append(new byte[]{singleByte});
    }

    /**
     * Appends the given integer as four bytes to the accumulator.
     */
    public void append(int integer) {
        append(ByteBuffer.allocate(4).putInt(integer).array());
    }

    /**
     * Appends the given String encoded as UTF-8 to the accumulator
     */
    public void append(String str) {
        append(str.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int compareTo(ByteAccumulator byteAccumulator) {
        byte[] thisArr = extractBytes();
        byte[] theirArr = byteAccumulator.extractBytes();

        //Let's go for lexicographic ordering
        for (int i=0;i<Math.min(thisArr.length, theirArr.length);i++) {
            if (thisArr[i] != theirArr[i])
                return thisArr[i] < theirArr[i] ? -1 : 1;
        }

        return Integer.compare(thisArr.length, theirArr.length);
    }
}
