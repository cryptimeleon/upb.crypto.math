package org.cryptimeleon.math.serialization.converter;

import org.cryptimeleon.math.serialization.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A converter for serializing representations to a binary format in form of a {@code byte[]}.
 */
public class BinaryFormatConverter extends Converter<byte[]> {
    protected static final byte TYPE_OBJ = 0;
    protected static final byte TYPE_INT = 1;
    protected static final byte TYPE_INT_INLINE = 2;
    protected static final byte TYPE_STR = 3;
    protected static final byte TYPE_BYTES = 4;
    protected static final byte TYPE_REPR = 5;
    protected static final byte TYPE_LIST = 6;
    protected static final byte TYPE_MAP = 7;
    protected static final byte TYPE_NULL = 8;


    /**
     * Maps a well-known string to its index in {@code this.well_known_strings}.
     * <p>
     * Fulfills contract that {@code str.equals(well_known_strings.get(well_known_string_indices.get(str)))}.
     */
    protected final HashMap<String, Integer> well_known_string_indices = new HashMap<>();
    /**
     * Stores well-known strings (see {@link #BinaryFormatConverter(List, List)}).
     * <p>
     * Fulfills contract that {@code str.equals(well_known_strings.get(well_known_string_indices.get(str)))}.
     */
    protected final ArrayList<String> well_known_strings = new ArrayList<>();

    public BinaryFormatConverter() {

    }

    /**
     * Instantiates the converter with lists of strings and classes well-known by both serializer and deserializer.
     * <p>
     * These strings and class names won't appear in the serialization result
     * (making it shorter by omitting strings from the byte array result of serialization).
     * For serialization and deserialization, the lists passed here must be equal
     * (in particular, the order of elements is important).
     */
    public BinaryFormatConverter(List<String> well_known_strings, List<Class<?>> well_known_classes) {
        int i=0;
        for (String str : well_known_strings) {
            this.well_known_string_indices.put(str, i++);
            this.well_known_strings.add(str);
        }
        for (Class<?> c : well_known_classes) {
            this.well_known_string_indices.put(c.getName(), i++);
            this.well_known_strings.add(c.getName());
        }
    }

    @Override
    public byte[] serialize(Representation r) {
        ByteString constants = new ByteString();
        // Map to look up strings that have already been stored as constants (to avoid storing them twice)
        HashMap<String, Integer> stringConstantPos = new HashMap<>();

        // Run the serialization
        ByteString structure = internalSerialize(r, constants, stringConstantPos);

        // Format: constantLen(4) || constants(len) || structure
        int constantLen = constants.len();
        ByteString overall = new ByteString();
        overall.append(constantLen);
        overall.append(constants);
        overall.append(structure);

        byte[] result = new byte[overall.len()];
        overall.writeToByteArray(result, 0);

        //TODO maybe gzip it using GZIPOutputStream ?!

        return result;
    }

    private ByteString internalSerialize(Representation repr, ByteString constants, HashMap<String, Integer> stringConstantPos) {
        ByteString structure = new ByteString();

        // Formats are denoted, for example "type(1) || ptr(4)", meaning that the first byte indicates a type,
        //  the next four bytes are a pointer.
        // Type is a byte indicating whether this is a String or a byte array or whatever,
        //  and ptr is an index on the constants stream (where the bulk of the serialization is written,
        //  for basic deduplication and to keep the structure format simpler).
        if (repr == null) {
            // Format: type(1)
            structure.append(TYPE_NULL);
        }
        if (repr instanceof StringRepresentation) {
            // Format: type(1) || ptr(4)
            structure.append(TYPE_STR);
            int ptr = addToConstants(repr.str().get(), constants, stringConstantPos);
            structure.append(ptr);
        }
        if (repr instanceof ByteArrayRepresentation) {
            // Format: type(1) || ptr(4)
            structure.append(TYPE_BYTES);
            int ptr = addToConstants(repr.bytes().get(), constants);
            structure.append(ptr);
        }
        if (repr instanceof BigIntegerRepresentation) {
            // Format: type(1) || int(4) for 32 bit integers, and type(1) || ptr(4) for long integers.
            BigInteger value = repr.bigInt().get();
            try {
                int valueAsInlineInt = value.intValueExact();
                structure.append(TYPE_INT_INLINE);
                structure.append(valueAsInlineInt);
            } catch (ArithmeticException e) {
                // Integer too large to fit into an int. Write to constants
                structure.append(TYPE_INT);
                int ptr = addToConstants(value.toByteArray(), constants);
                structure.append(ptr);
            }
        }

        if (repr instanceof RepresentableRepresentation) {
            // Format: type(1) || ptrClassname(4) || lenRepr(4) || repr(lenRepr)
            structure.append(TYPE_REPR);
            int ptrClassname = addToConstants(repr.repr().getRepresentedTypeName(), constants, stringConstantPos);
            structure.append(ptrClassname);

            ByteString reprBytes = internalSerialize(repr.repr().getRepresentation(), constants, stringConstantPos);
            // Serialized representation of contained object.
            int lenRepr = reprBytes.len();
            structure.append(lenRepr);
            structure.append(reprBytes);
        }

        if (repr instanceof ListRepresentation) {
            // Format: type(1) || (len(4) || repr(len))*
            structure.append(TYPE_LIST);

            for (Representation listItem : repr.list()) {
                ByteString listItemSerialization = internalSerialize(listItem, constants, stringConstantPos);
                structure.append(listItemSerialization.len());
                structure.append(listItemSerialization);
            }
        }

        if (repr instanceof ObjectRepresentation) {
            // Format: type(1) || ( ptrToKey(4) || len(4) || repr(len) )*
            structure.append(TYPE_OBJ);

            repr.obj().forEachOrderedByKeys((key, value) -> {
                        int ptrToKey = addToConstants(key, constants, stringConstantPos);
                        structure.append(ptrToKey);

                        ByteString inner = internalSerialize(value, constants, stringConstantPos);
                        structure.append(inner.len());
                        structure.append(inner);
                    });
        }

        if (repr instanceof MapRepresentation) {
            // Format: type(1) || ( keyLen(4) || key(keyLen) || valueLen(4) || value(valueLen) )*
            structure.append(TYPE_MAP);

            // Random order to ensure map order doesn't leak anything useful
            repr.map().forEachRandomlyOrdered((key, value) -> {
                        ByteString keySerialized = internalSerialize(key, constants, stringConstantPos);
                        structure.append(keySerialized.len());
                        structure.append(keySerialized);

                        ByteString valueSerialized = internalSerialize(value, constants, stringConstantPos);
                        structure.append(valueSerialized.len());
                        structure.append(valueSerialized);
                    });
        }

        return structure;
    }

    /**
     * Adds str to constants stream (if not already in there) and returns its position.
     */
    private int addToConstants(String str, ByteString constants, HashMap<String, Integer> stringConstantPos) {
        Integer well_known = well_known_string_indices.get(str);
        if (well_known != null)
            return -well_known-1;

        return stringConstantPos.computeIfAbsent(str, s -> {
            byte[] stringAsBytes = s.getBytes(StandardCharsets.UTF_8);
            return addToConstants(stringAsBytes, constants);
        });
    }

    private int addToConstants(byte[] bytes, ByteString constants) {
        // Constant format: len(4) || bytes(len)
        int indexWhereConstantIsWrittenTo = constants.len();
        constants.append(bytes.length);
        constants.append(bytes);

        return indexWhereConstantIsWrittenTo;
    }

    private static int byteArrayToInt(byte[] array, int posOfInt) {
        return ByteBuffer.wrap(array).getInt(posOfInt);
    }

    private static byte[] intToByteArray(int val) {
        return ByteBuffer.allocate(4).putInt(val).array();
    }

    @Override
    public Representation deserialize(byte[] data) {
        return internalDeserialize(new Input(data));
    }

    /**
     * Interprets the given (structure-)data as a structure and recreates the corresponding Representation.
     * @return the Representation corresponding to the given Input
     */
    private Representation internalDeserialize(Input data) {
        //Whatever the concrete type, it begins with type(1)
        byte type = data.readByte(0);

        if (type == TYPE_NULL) {
            // Format: type(1)
            return null;
        }
        if (type == TYPE_BYTES) {
            // Format: type(1) || ptr(4)
            int ptr = data.readInt(1);
            return new ByteArrayRepresentation(data.getByteArrayFromConstants(ptr));
        }
        if (type == TYPE_STR) {
            // Format: type(1) || ptr(4)
            int ptr = data.readInt(1);
            return new StringRepresentation(data.getStringFromConstants(ptr));
        }
        if (type == TYPE_INT_INLINE) {
            // Format: type(1) || int(4)
            int value = data.readInt(1);
            return new BigIntegerRepresentation(value);
        }
        if (type == TYPE_INT) {
            // Format: type(1) || ptr(4)
            int ptr = data.readInt(1);
            return new BigIntegerRepresentation(new BigInteger(data.getByteArrayFromConstants(ptr)));
        }
        if (type == TYPE_REPR) {
            // Format: type(1) || ptrClassname(4) || lenRepr(4) || repr(lenRepr)
            int ptrClassname = data.readInt(1);
            int lenRepr = data.readInt(1+4);
            return new RepresentableRepresentation(
                    data.getStringFromConstants(ptrClassname),
                    internalDeserialize(data.getSubstructureData(1+4+4, lenRepr))
            );
        }
        if (type == TYPE_LIST) {
            // Format: type(1) || (len(4) || repr(len))*
            int offset = 1;
            ListRepresentation result = new ListRepresentation();

            // There's still a chance to find a list item between offset and the end of data.
            while (offset<data.len()-4) {
                int len = data.readInt(offset);
                Representation listItem = internalDeserialize(data.getSubstructureData(offset+4, len));
                result.put(listItem);
                offset += 4+len;
            }

            return result;
        }
        if (type == TYPE_OBJ) {
            // Format: type(1) || ( ptrToKey(4) || len(4) || repr(len) )*
            int offset = 1;
            ObjectRepresentation result = new ObjectRepresentation();

            while (offset < data.len()-8) {
                int ptrToKey = data.readInt(offset);
                int len = data.readInt(offset + 4);
                Representation value = internalDeserialize(data.getSubstructureData(offset+8, len));
                result.put(data.getStringFromConstants(ptrToKey), value);
                offset += 8 + len;
            }

            return result;
        }
        if (type == TYPE_MAP) {
            // Format: type(1) || ( keyLen(4) || key(keyLen) || valueLen(4) || value(valueLen) )*
            int offset = 1;
            MapRepresentation result = new MapRepresentation();

            while (offset < data.len()-4) {
                int keyLen = data.readInt(offset);
                Representation key = internalDeserialize(data.getSubstructureData(offset + 4, keyLen));
                offset += 4 + keyLen;

                int valueLen = data.readInt(offset);
                Representation value = internalDeserialize(data.getSubstructureData(offset + 4, valueLen));
                offset += 4 + valueLen;

                result.put(key, value);
            }

            return result;
        }
        throw new IllegalArgumentException("Don't know how to deserialize type marked with "+type);
    }

    /**
     * Input used during deserialization. Logically contains a part of the structure and the full constants data.
     */
    protected class Input {
        private final byte[] data;
        private final static int constantsOffset = 4;
        private final int constantsLen;
        /**
         * Offset of the currently selected substructure in the input data.
         */
        private final int substructureOffset;
        private final int substructureLength;

        public Input(byte[] data) {
            // Format of s: constantLen(4) || constants(len) || structure
            this.data = data;
            constantsLen = byteArrayToInt(data, 0);
            substructureOffset = constantsOffset + constantsLen;
            substructureLength = data.length-substructureOffset;
        }

        /**
         * Select a structure at a given offset with a given length.
         * @param input the existing input
         * @param structureOffset the offset of the substructure to select
         * @param structureLength the length of the substructure to select
         */
        private Input(Input input, int structureOffset, int structureLength) {
            this.data = input.data;
            this.constantsLen = input.constantsLen;
            this.substructureOffset = input.substructureOffset + structureOffset;
            this.substructureLength = structureLength;
        }

        public String getStringFromConstants(int ptr) {
            if (ptr < 0)
                return well_known_strings.get(-(ptr+1));

            return new String(data, ptr+constantsOffset+4, byteArrayToInt(data, ptr+constantsOffset),
                    StandardCharsets.UTF_8);
        }

        public byte[] getByteArrayFromConstants(int ptr) {
            return Arrays.copyOfRange(data, ptr+constantsOffset+4,
                    (ptr+constantsOffset+4)+byteArrayToInt(data, ptr+constantsOffset));
        }

        /**
         * Read an integer at offset {@code posInStructure} in the currently selected substructure.
         * @param posInStructure offset to read integer from
         * @return the resulting integer
         * @throws IndexOutOfBoundsException if the position is out of bounds of this input's data
         */
        public int readInt(int posInStructure) {
            return byteArrayToInt(data, substructureOffset + posInStructure);
        }

        public byte readByte(int posInStructure) {
            return data[substructureOffset + posInStructure];
        }

        public Input getSubstructureData(int offset, int length) {
            if (substructureLength-offset < length)
                throw new RuntimeException("Illegal offset or length");
            return new Input(this, offset, length);
        }

        /**
         * Returns length of substructure
         */
        public int len() {
            return substructureLength;
        }
    }

    /**
     * A string of bytes used to implement serialization for {@code BinaryFormatConverter}.
     */
    protected static class ByteString {
        private int len = 0;
        ByteStringListEntry firstPart;
        ByteStringListEntry lastPart;
        boolean valid = true;

        public ByteString() {
            firstPart = new ByteStringListEntry(new byte[0]);
            lastPart = firstPart;
        }

        /**
         * Appends the given ByteString to this one.
         * Do not use other afterwards anymore.
         */
        public void append(ByteString other) {
            if (!valid)
                throw new RuntimeException("Do not re-use ByteStrings that you've already appended to something.");

            lastPart.nextPart = other.firstPart;
            lastPart = other.lastPart;

            other.valid = false;
            len = len + other.len;
        }

        public void append(byte[] bytes) {
            if (!valid)
                throw new RuntimeException("Do not re-use ByteStrings that you've already appended to something.");

            lastPart.nextPart = new ByteStringListEntry(bytes);
            lastPart = lastPart.nextPart;

            len = len + bytes.length;
        }

        public void append(byte val) {
            append(new byte[] {val});
        }

        public void append(int val) {
            append(intToByteArray(val));
        }

        public int len() {
            if (!valid)
                throw new RuntimeException("Do not re-use ByteStrings that you've already appended to something.");
            return len;
        }

        public void writeToByteArray(Object dest, int pos) {
            firstPart.writeToByteArray(dest, pos);
        }

        protected static class ByteStringListEntry {
            ByteStringListEntry nextPart;
            final byte[] thisPart;

            public ByteStringListEntry(byte[] bytes) {
                thisPart = bytes;
                nextPart = null;
            }

            protected void writeToByteArray(Object dest, int pos) {
                if (thisPart.length != 0)
                    System.arraycopy(thisPart, 0, dest, pos, thisPart.length);
                if (nextPart != null)
                    nextPart.writeToByteArray(dest, pos+thisPart.length);
            }
        }

        @Override
        public String toString() {
            byte[] result = new byte[len];
            firstPart.writeToByteArray(result, 0);
            return Arrays.toString(result);
        }
    }
}
