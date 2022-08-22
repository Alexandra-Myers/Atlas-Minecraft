package net.atlas.minecraft.server.networking.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class PacketByteBuf extends ByteBuf {

    private static final int MAX_VAR_INT_LENGTH = 5;

    private static final int MAX_VAR_LONG_LENGTH = 10;

    private static final int MAX_READ_NBT_SIZE = 2097152;
    private final ByteBuf parent;

    public static final short DEFAULT_MAX_STRING_LENGTH = 32767;

    public static final int MAX_TEXT_LENGTH = 262144;
    private static final int PUBLIC_KEY_SIZE = 256;
    private static final int MAX_PUBLIC_KEY_HEADER_SIZE = 256;
    private static final int MAX_PUBLIC_KEY_LENGTH = 512;

    public PacketByteBuf(ByteBuf byteBuf) {
        this.parent = byteBuf;
    }

    /**
     * Returns the number of bytes needed to encode {@code value} as a
     * {@linkplain #writeVarInt(int) var int}. Guaranteed to be between {@code
     * 1} and {@value #MAX_VAR_INT_LENGTH}.
     *
     * @return the number of bytes a var int {@code value} uses
     *
     * @param value the value to encode
     */
    public static int getVarIntLength(int value) {
        for(int i = 1; i < 5; ++i) {
            if ((value & -1 << i * 7) == 0) {
                return i;
            }
        }

        return 5;
    }

    public static int getVarLongLength(long value) {
        for(int i = 1; i < 10; ++i) {
            if ((value & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    public static <T> IntFunction<T> getMaxValidator(IntFunction<T> applier, int max) {
        return value -> {
            if (value > max) {
                throw new DecoderException("Value " + value + " is larger than limit " + max);
            } else {
                return applier.apply(value);
            }
        };
    }

    /**
     * Reads a collection from this buf. The collection is stored as a leading
     * {@linkplain #readVarInt() var int} size followed by the entries
     * sequentially.
     *
     * @param <T> the collection's entry type
     * @param <C> the collection's type
     * @return the read collection
     * @see #writeCollection(Collection, Writer)
     *
     * @param collectionFactory a factory that creates a collection with a given size
     */
    public <T, C extends Collection<T>> C readCollection(IntFunction<C> collectionFactory, PacketByteBuf.Reader<T> entryReader) {
        int i = this.readVarInt();
        C collection = (C)collectionFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            collection.add(entryReader.apply(this));
        }

        return collection;
    }

    /**
     * Writes a collection to this buf. The collection is stored as a leading
     * {@linkplain #readVarInt() var int} size followed by the entries
     * sequentially.
     *
     * @param <T> the list's entry type
     * @see #readCollection(IntFunction, Reader)
     *
     * @param collection the collection to write
     */
    public <T> void writeCollection(Collection<T> collection, PacketByteBuf.Writer<T> entryWriter) {
        this.writeVarInt(collection.size());

        for (T object : collection) {
            entryWriter.accept(this, object);
        }
    }

    /**
     * Reads a map from this buf. The map is stored as a leading
     * {@linkplain #readVarInt() var int} size followed by each key and value
     * pair.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M> the map type
     * @return the read map
     * @see #writeMap(Map, Writer, Writer)
     *
     * @param mapFactory a factory that creates a map with a given size
     */
    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> mapFactory, PacketByteBuf.Reader<K> keyReader, PacketByteBuf.Reader<V> valueReader) {
        int i = this.readVarInt();
        M map = (M)mapFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            K object = (K)keyReader.apply(this);
            V object2 = (V)valueReader.apply(this);
            map.put(object, object2);
        }

        return map;
    }

    /**
     * Writes a map to this buf. The map is stored as a leading
     * {@linkplain #readVarInt() var int} size followed by each key and value
     * pair.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @see #readMap(IntFunction, Reader, Reader)
     *
     * @param map the map to write
     */
    public <K, V> void writeMap(Map<K, V> map, PacketByteBuf.Writer<K> keyWriter, PacketByteBuf.Writer<V> valueWriter) {
        this.writeVarInt(map.size());
        map.forEach((key, value) -> {
            keyWriter.accept(this, key);
            valueWriter.accept(this, value);
        });
    }

    /**
     * Iterates a collection from this buf. The collection is stored as a leading
     * {@linkplain #readVarInt() var int} {@code size} followed by the entries
     * sequentially. The {@code consumer} will be called {@code size} times.
     *
     * @see #readCollection(IntFunction, Reader)
     *
     * @param consumer the consumer to read entries
     */
    public void forEachInCollection(Consumer<PacketByteBuf> consumer) {
        int i = this.readVarInt();

        for(int j = 0; j < i; ++j) {
            consumer.accept(this);
        }

    }

    /**
     * Writes an optional value to this buf. An optional value is represented by
     * a boolean indicating if the value is present, followed by the value only if
     * the value is present.
     *
     * @see #readOptional(Reader)
     */
    public <T> void writeOptional(Optional<T> value, PacketByteBuf.Writer<T> writer) {
        if (value.isPresent()) {
            this.writeBoolean(true);
            writer.accept(this, value.get());
        } else {
            this.writeBoolean(false);
        }

    }

    /**
     * Reads an optional value from this buf. An optional value is represented by
     * a boolean indicating if the value is present, followed by the value only if
     * the value is present.
     *
     * @return the read optional value
     * @see #writeOptional(Optional, Writer)
     */
    public <T> Optional<T> readOptional(PacketByteBuf.Reader<T> reader) {
        return this.readBoolean() ? Optional.of(reader.apply(this)) : Optional.empty();
    }

    /**
     * Reads a nullable value from this buf. A nullable value is represented by a
     * boolean indicating if the value is present, followed by the value only if
     * it's present.
     *
     * @return the read value, or null
     * @see #writeNullable(Object, Writer)
     */
    @Nullable
    public <T> T readNullable(PacketByteBuf.Reader<T> reader) {
        return (T)(this.readBoolean() ? reader.apply(this) : null);
    }

    /**
     * Writes a nullable value to this buf. A nullable value is represented by a
     * boolean indicating if the value is present, followed by the value only if
     * it's present.
     *
     * @see #readNullable(Reader)
     */
    public <T> void writeNullable(@Nullable T value, PacketByteBuf.Writer<T> writer) {
        if (value != null) {
            this.writeBoolean(true);
            writer.accept(this, value);
        } else {
            this.writeBoolean(false);
        }

    }

    /**
     * Reads an array of primitive bytes from this buf. The array first has a
     * var int indicating its length, followed by the actual bytes. The array
     * does not have a length limit.
     *
     * @see #readByteArray(int)
     * @see #writeByteArray(byte[])
     * @return the read byte array
     */
    public byte[] readByteArray() {
        return this.readByteArray(this.readableBytes());
    }

    /**
     * Writes an array of primitive bytes to this buf. The array first has a
     * var int indicating its length, followed by the actual bytes.
     *
     * @see #readByteArray()
     * @return this buf, for chaining
     *
     * @param array the array to write
     */
    public PacketByteBuf writeByteArray(byte[] array) {
        this.writeVarInt(array.length);
        this.writeBytes(array);
        return this;
    }

    /**
     * Reads an array of primitive bytes from this buf. The array first has a
     * var int indicating its length, followed by the actual bytes. The array
     * has a length limit given by {@code maxSize}.
     *
     * @see #readByteArray()
     * @see #writeByteArray(byte[])
     * @return the read byte array
     * @throws io.netty.handler.codec.DecoderException if the read array has a
     * length over {@code maxSize}
     *
     * @param maxSize the max length of the read array
     */
    public byte[] readByteArray(int maxSize) {
        int i = this.readVarInt();
        if (i > maxSize) {
            throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
        } else {
            byte[] bs = new byte[i];
            this.readBytes(bs);
            return bs;
        }
    }

    /**
     * Writes an array of primitive ints to this buf. The array first has a
     * var int indicating its length, followed by the var int entries.
     *
     * @implNote An int array has the same format as a list of ints.
     *
     * @see #readIntArray(int)
     * @see #writeIntArray(int[])

     * @return this buf, for chaining
     *
     * @param array the array to write
     */
    public PacketByteBuf writeIntArray(int[] array) {
        this.writeVarInt(array.length);

        for(int i : array) {
            this.writeVarInt(i);
        }

        return this;
    }

    /**
     * Reads an array of primitive ints from this buf. The array first has a
     * var int indicating its length, followed by the var int entries. The array
     * does not have a length limit.
     *
     * @implNote An int array has the same format as a list of ints.
     *
     * @see #readIntArray(int)
     * @see #writeIntArray(int[])
     * @return the read byte array
     */
    public int[] readIntArray() {
        return this.readIntArray(this.readableBytes());
    }

    /**
     * Reads an array of primitive ints from this buf. The array first has a
     * var int indicating its length, followed by the var int entries. The array
     * has a length limit given by {@code maxSize}.
     *
     * @implNote An int array has the same format as a list of ints.
     *
     * @see #readIntArray()
     * @see #writeIntArray(int[])
     * @return the read byte array
     * @throws io.netty.handler.codec.DecoderException if the read array has a
     * length over {@code maxSize}
     *
     * @param maxSize the max length of the read array
     */
    public int[] readIntArray(int maxSize) {
        int i = this.readVarInt();
        if (i > maxSize) {
            throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + maxSize);
        } else {
            int[] is = new int[i];

            for(int j = 0; j < is.length; ++j) {
                is[j] = this.readVarInt();
            }

            return is;
        }
    }

    /**
     * Writes an array of primitive longs to this buf. The array first has a
     * var int indicating its length, followed by the regular long (not var
     * long) values.
     *
     * @see #readLongArray()
     * @return this buf, for chaining
     *
     * @param array the array to write
     */
    public PacketByteBuf writeLongArray(long[] array) {
        this.writeVarInt(array.length);

        for(long l : array) {
            this.writeLong(l);
        }

        return this;
    }

    /**
     * Reads an array of primitive longs from this buf. The array first has a
     * var int indicating its length, followed by the regular long (not var
     * long) values. The array does not have a length limit.
     *
     * @see #writeLongArray(long[])
     * @see #readLongArray(long[])
     * @see #readLongArray(long[], int)
     * @return the read long array
     */
    public long[] readLongArray() {
        return this.readLongArray(null);
    }

    /**
     * Reads an array of primitive longs from this buf. The array first has a
     * var int indicating its length, followed by the regular long (not var
     * long) values. The array does not have a length limit.
     *
     * <p>Only when {@code toArray} is not {@code null} and {@code
     * toArray.length} equals to the length var int read will the {@code
     * toArray} be reused and returned; otherwise, a new array
     * of proper size is created.
     *
     * @see #writeLongArray(long[])
     * @see #readLongArray()
     * @see #readLongArray(long[], int)
     * @return the read long array
     *
     * @param toArray the array to reuse
     */
    public long[] readLongArray(@Nullable long[] toArray) {
        return this.readLongArray(toArray, this.readableBytes() / 8);
    }

    /**
     * Reads an array of primitive longs from this buf. The array first has a
     * var int indicating its length, followed by the regular long (not var
     * long) values. The array has a length limit of {@code maxSize}.
     *
     * <p>Only when {@code toArray} is not {@code null} and {@code
     * toArray.length} equals to the length var int read will the {@code
     * toArray} be reused and returned; otherwise, a new array
     * of proper size is created.
     *
     * @see #writeLongArray(long[])
     * @see #readLongArray()
     * @see #readLongArray(long[])
     * @return the read long array
     * @throws io.netty.handler.codec.DecoderException if the read array has a
     * length over {@code maxSize}
     *
     * @param toArray the array to reuse
     * @param maxSize the max length of the read array
     */
    public long[] readLongArray(@Nullable long[] toArray, int maxSize) {
        int i = this.readVarInt();
        if (toArray == null || toArray.length != i) {
            if (i > maxSize) {
                throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + maxSize);
            }

            toArray = new long[i];
        }

        for(int j = 0; j < toArray.length; ++j) {
            toArray[j] = this.readLong();
        }

        return toArray;
    }

    /**
     * Returns an array of bytes of contents in this buf between index {@code 0} and
     * the {@link #writerIndex()}.
     */
    public byte[] getWrittenBytes() {
        int i = this.writerIndex();
        byte[] bs = new byte[i];
        this.getBytes(0, bs);
        return bs;
    }


    /**
     * Reads an enum constant from this buf. An enum constant is represented
     * by a var int indicating its ordinal.
     *
     * @return the read enum constant
     * @see #writeEnumConstant(Enum)
     *
     * @param enumClass the enum class, for constant lookup
     */
    public <T extends Enum<T>> T readEnumConstant(Class<T> enumClass) {
        return (T)enumClass.getEnumConstants()[this.readVarInt()];
    }

    /**
     * Writes an enum constant to this buf. An enum constant is represented
     * by a var int indicating its ordinal.
     *
     * @return this buf, for chaining
     * @see #readEnumConstant(Class)
     *
     * @param instance the enum constant to write
     */
    public PacketByteBuf writeEnumConstant(Enum<?> instance) {
        return this.writeVarInt(instance.ordinal());
    }

    /**
     * Reads a single var int from this buf.
     *
     * @return the value read
     * @see #writeVarInt(int)
     */
    public int readVarInt() {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = this.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b & 128) == 128);

        return i;
    }

    /**
     * Reads a single var long from this buf.
     *
     * @return the value read
     * @see #writeVarLong(long)
     */
    public long readVarLong() {
        long l = 0L;
        int i = 0;

        byte b;
        do {
            b = this.readByte();
            l |= (long)(b & 127) << i++ * 7;
            if (i > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while((b & 128) == 128);

        return l;
    }

    /**
     * Writes a UUID (universally unique identifier) to this buf. A UUID is
     * represented by two regular longs.
     *
     * @return this buf, for chaining
     * @see #readUuid()
     *
     * @param uuid the UUID to write
     */
    public PacketByteBuf writeUuid(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    /**
     * Reads a UUID (universally unique identifier) from this buf. A UUID is
     * represented by two regular longs.
     *
     * @return the read UUID
     * @see #writeUuid(UUID)
     */
    public UUID readUuid() {
        return new UUID(this.readLong(), this.readLong());
    }

    /**
     * Writes a single var int to this buf.
     *
     * <p>Compared to regular ints, var ints may use less bytes (ranging from 1
     * to 5, where regular ints use 4) when representing smaller positive
     * numbers.
     *
     * @return this buf, for chaining
     * @see #readVarInt()
     * @see #getVarIntLength(int)
     *
     * @param value the value to write
     */
    public PacketByteBuf writeVarInt(int value) {
        while((value & -128) != 0) {
            this.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        this.writeByte(value);
        return this;
    }

    /**
     * Writes a single var long to this buf.
     *
     * <p>Compared to regular longs, var longs may use less bytes when
     * representing smaller positive numbers.
     *
     * @return this buf, for chaining
     * @see #readVarLong()
     * @see #getVarLongLength(long)
     *
     * @param value the value to write
     */
    public PacketByteBuf writeVarLong(long value) {
        while((value & -128L) != 0L) {
            this.writeByte((int)(value & 127L) | 128);
            value >>>= 7;
        }

        this.writeByte((int)value);
        return this;
    }

    /**
     * Reads a string from this buf. A string is represented by a byte array of
     * its UTF-8 data. The string can have a maximum length of {@value
     * #DEFAULT_MAX_STRING_LENGTH}.
     *
     * @return the string read
     * @throws io.netty.handler.codec.DecoderException if the string read
     * exceeds the maximum length
     * @see #readString(int)
     * @see #writeString(String)
     * @see #writeString(String, int)
     */
    public String readString() {
        return this.readString(32767);
    }

    /**
     * Reads a string from this buf. A string is represented by a byte array of
     * its UTF-8 data. The string can have a maximum length of {@code maxLength}.
     *
     * @return the string read
     * @throws io.netty.handler.codec.DecoderException if the string read
     * is longer than {@code maxLength}
     * @see #readString()
     * @see #writeString(String)
     * @see #writeString(String, int)
     *
     * @param maxLength the maximum length of the string read
     */
    public String readString(int maxLength) {
        int i = method_44302(maxLength);
        int j = this.readVarInt();
        if (j > i) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i + ")");
        } else if (j < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            String string = this.toString(this.readerIndex(), j, StandardCharsets.UTF_8);
            this.readerIndex(this.readerIndex() + j);
            if (string.length() > maxLength) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + string.length() + " > " + maxLength + ")");
            } else {
                return string;
            }
        }
    }

    /**
     * Writes a string to this buf. A string is represented by a byte array of
     * its UTF-8 data. That byte array can have a maximum length of
     * {@value #DEFAULT_MAX_STRING_LENGTH}.
     *
     * @return this buf, for chaining
     * @throws io.netty.handler.codec.EncoderException if the byte array of the
     * string to write is longer than {@value #DEFAULT_MAX_STRING_LENGTH}
     * @see #readString()
     * @see #readString(int)
     * @see #writeString(String, int)
     *
     * @param string the string to write
     */
    public PacketByteBuf writeString(String string) {
        return this.writeString(string, 32767);
    }

    /**
     * Writes a string to this buf. A string is represented by a byte array of
     * its UTF-8 data. That byte array can have a maximum length of
     * {@code maxLength}.
     *
     * @return this buf, for chaining
     * @throws io.netty.handler.codec.EncoderException if the byte array of the
     * string to write is longer than {@code maxLength}
     * @see #readString()
     * @see #readString(int)
     * @see #writeString(String)
     *
     * @param string the string to write
     * @param maxLength the max length of the byte array
     */
    public PacketByteBuf writeString(String string, int maxLength) {
        if (string.length() > maxLength) {
            throw new EncoderException("String too big (was " + string.length() + " characters, max " + maxLength + ")");
        } else {
            byte[] bs = string.getBytes(StandardCharsets.UTF_8);
            int i = method_44302(maxLength);
            if (bs.length > i) {
                throw new EncoderException("String too big (was " + bs.length + " bytes encoded, max " + i + ")");
            } else {
                this.writeVarInt(bs.length);
                this.writeBytes(bs);
                return this;
            }
        }
    }

    private static int method_44302(int i) {
        return i * 3;
    }

    /**
     * Reads a date from this buf. A date is represented by its time, a regular
     * long.
     *
     * @return the read date
     * @see #writeDate(Date)
     */
    public Date readDate() {
        return new Date(this.readLong());
    }

    /**
     * Writes a date to this buf. A date is represented by its time, a regular
     * long.
     *
     * @return this buf, for chaining
     * @see #readDate()
     *
     * @param date the date to write
     */
    public PacketByteBuf writeDate(Date date) {
        this.writeLong(date.getTime());
        return this;
    }

    /**
     * Reads an instant from this buf. An instant is represented by a long indicating
     * the instant epoch milliseconds.
     *
     * @see #writeInstant(Instant)
     */
    public Instant readInstant() {
        return Instant.ofEpochMilli(this.readLong());
    }

    /**
     * Writes an instant to this buf. An instant is represented by a long indicating
     * the instant epoch milliseconds.
     *
     * @see #readInstant()
     */
    public void writeInstant(Instant instant) {
        this.writeLong(instant.toEpochMilli());
    }

    @Override
    public int capacity() {
        return this.parent.capacity();
    }

    @Override
    public ByteBuf capacity(int i) {
        return this.parent.capacity(i);
    }

    @Override
    public int maxCapacity() {
        return this.parent.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.parent.alloc();
    }

    @Override
    public ByteOrder order() {
        return this.parent.order();
    }

    @Override
    public ByteBuf order(ByteOrder byteOrder) {
        return this.parent.order(byteOrder);
    }

    @Override
    public ByteBuf unwrap() {
        return this.parent.unwrap();
    }

    @Override
    public boolean isDirect() {
        return this.parent.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return this.parent.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return this.parent.asReadOnly();
    }

    @Override
    public int readerIndex() {
        return this.parent.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int i) {
        return this.parent.readerIndex(i);
    }

    @Override
    public int writerIndex() {
        return this.parent.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int i) {
        return this.parent.writerIndex(i);
    }

    @Override
    public ByteBuf setIndex(int i, int j) {
        return this.parent.setIndex(i, j);
    }

    @Override
    public int readableBytes() {
        return this.parent.readableBytes();
    }

    @Override
    public int writableBytes() {
        return this.parent.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return this.parent.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return this.parent.isReadable();
    }

    @Override
    public boolean isReadable(int i) {
        return this.parent.isReadable(i);
    }

    @Override
    public boolean isWritable() {
        return this.parent.isWritable();
    }

    @Override
    public boolean isWritable(int i) {
        return this.parent.isWritable(i);
    }

    @Override
    public ByteBuf clear() {
        return this.parent.clear();
    }

    @Override
    public ByteBuf markReaderIndex() {
        return this.parent.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return this.parent.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex() {
        return this.parent.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return this.parent.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes() {
        return this.parent.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return this.parent.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable(int i) {
        return this.parent.ensureWritable(i);
    }

    @Override
    public int ensureWritable(int i, boolean bl) {
        return this.parent.ensureWritable(i, bl);
    }

    @Override
    public boolean getBoolean(int i) {
        return this.parent.getBoolean(i);
    }

    @Override
    public byte getByte(int i) {
        return this.parent.getByte(i);
    }

    @Override
    public short getUnsignedByte(int i) {
        return this.parent.getUnsignedByte(i);
    }

    @Override
    public short getShort(int i) {
        return this.parent.getShort(i);
    }

    @Override
    public short getShortLE(int i) {
        return this.parent.getShortLE(i);
    }

    @Override
    public int getUnsignedShort(int i) {
        return this.parent.getUnsignedShort(i);
    }

    @Override
    public int getUnsignedShortLE(int i) {
        return this.parent.getUnsignedShortLE(i);
    }

    @Override
    public int getMedium(int i) {
        return this.parent.getMedium(i);
    }

    @Override
    public int getMediumLE(int i) {
        return this.parent.getMediumLE(i);
    }

    @Override
    public int getUnsignedMedium(int i) {
        return this.parent.getUnsignedMedium(i);
    }

    @Override
    public int getUnsignedMediumLE(int i) {
        return this.parent.getUnsignedMediumLE(i);
    }

    @Override
    public int getInt(int i) {
        return this.parent.getInt(i);
    }

    @Override
    public int getIntLE(int i) {
        return this.parent.getIntLE(i);
    }

    @Override
    public long getUnsignedInt(int i) {
        return this.parent.getUnsignedInt(i);
    }

    @Override
    public long getUnsignedIntLE(int i) {
        return this.parent.getUnsignedIntLE(i);
    }

    @Override
    public long getLong(int i) {
        return this.parent.getLong(i);
    }

    @Override
    public long getLongLE(int i) {
        return this.parent.getLongLE(i);
    }

    @Override
    public char getChar(int i) {
        return this.parent.getChar(i);
    }

    @Override
    public float getFloat(int i) {
        return this.parent.getFloat(i);
    }

    @Override
    public double getDouble(int i) {
        return this.parent.getDouble(i);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf) {
        return this.parent.getBytes(i, byteBuf);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int j) {
        return this.parent.getBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int j, int k) {
        return this.parent.getBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bs) {
        return this.parent.getBytes(i, bs);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bs, int j, int k) {
        return this.parent.getBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuffer byteBuffer) {
        return this.parent.getBytes(i, byteBuffer);
    }

    @Override
    public ByteBuf getBytes(int i, OutputStream outputStream, int j) throws IOException {
        return this.parent.getBytes(i, outputStream, j);
    }

    @Override
    public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int j) throws IOException {
        return this.parent.getBytes(i, gatheringByteChannel, j);
    }

    @Override
    public int getBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
        return this.parent.getBytes(i, fileChannel, l, j);
    }

    @Override
    public CharSequence getCharSequence(int i, int j, Charset charset) {
        return this.parent.getCharSequence(i, j, charset);
    }

    @Override
    public ByteBuf setBoolean(int i, boolean bl) {
        return this.parent.setBoolean(i, bl);
    }

    @Override
    public ByteBuf setByte(int i, int j) {
        return this.parent.setByte(i, j);
    }

    @Override
    public ByteBuf setShort(int i, int j) {
        return this.parent.setShort(i, j);
    }

    @Override
    public ByteBuf setShortLE(int i, int j) {
        return this.parent.setShortLE(i, j);
    }

    @Override
    public ByteBuf setMedium(int i, int j) {
        return this.parent.setMedium(i, j);
    }

    @Override
    public ByteBuf setMediumLE(int i, int j) {
        return this.parent.setMediumLE(i, j);
    }

    @Override
    public ByteBuf setInt(int i, int j) {
        return this.parent.setInt(i, j);
    }

    @Override
    public ByteBuf setIntLE(int i, int j) {
        return this.parent.setIntLE(i, j);
    }

    @Override
    public ByteBuf setLong(int i, long l) {
        return this.parent.setLong(i, l);
    }

    @Override
    public ByteBuf setLongLE(int i, long l) {
        return this.parent.setLongLE(i, l);
    }

    @Override
    public ByteBuf setChar(int i, int j) {
        return this.parent.setChar(i, j);
    }

    @Override
    public ByteBuf setFloat(int i, float f) {
        return this.parent.setFloat(i, f);
    }

    @Override
    public ByteBuf setDouble(int i, double d) {
        return this.parent.setDouble(i, d);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf) {
        return this.parent.setBytes(i, byteBuf);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int j) {
        return this.parent.setBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int j, int k) {
        return this.parent.setBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bs) {
        return this.parent.setBytes(i, bs);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bs, int j, int k) {
        return this.parent.setBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuffer byteBuffer) {
        return this.parent.setBytes(i, byteBuffer);
    }

    @Override
    public int setBytes(int i, InputStream inputStream, int j) throws IOException {
        return this.parent.setBytes(i, inputStream, j);
    }

    @Override
    public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int j) throws IOException {
        return this.parent.setBytes(i, scatteringByteChannel, j);
    }

    @Override
    public int setBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
        return this.parent.setBytes(i, fileChannel, l, j);
    }

    @Override
    public ByteBuf setZero(int i, int j) {
        return this.parent.setZero(i, j);
    }

    @Override
    public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
        return this.parent.setCharSequence(i, charSequence, charset);
    }

    @Override
    public boolean readBoolean() {
        return this.parent.readBoolean();
    }

    @Override
    public byte readByte() {
        return this.parent.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return this.parent.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return this.parent.readShort();
    }

    @Override
    public short readShortLE() {
        return this.parent.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return this.parent.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return this.parent.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return this.parent.readMedium();
    }

    @Override
    public int readMediumLE() {
        return this.parent.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return this.parent.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return this.parent.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return this.parent.readInt();
    }

    @Override
    public int readIntLE() {
        return this.parent.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return this.parent.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return this.parent.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return this.parent.readLong();
    }

    @Override
    public long readLongLE() {
        return this.parent.readLongLE();
    }

    @Override
    public char readChar() {
        return this.parent.readChar();
    }

    @Override
    public float readFloat() {
        return this.parent.readFloat();
    }

    @Override
    public double readDouble() {
        return this.parent.readDouble();
    }

    @Override
    public ByteBuf readBytes(int i) {
        return this.parent.readBytes(i);
    }

    @Override
    public ByteBuf readSlice(int i) {
        return this.parent.readSlice(i);
    }

    @Override
    public ByteBuf readRetainedSlice(int i) {
        return this.parent.readRetainedSlice(i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf) {
        return this.parent.readBytes(byteBuf);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i) {
        return this.parent.readBytes(byteBuf, i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i, int j) {
        return this.parent.readBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf readBytes(byte[] bs) {
        return this.parent.readBytes(bs);
    }

    @Override
    public ByteBuf readBytes(byte[] bs, int i, int j) {
        return this.parent.readBytes(bs, i, j);
    }

    @Override
    public ByteBuf readBytes(ByteBuffer byteBuffer) {
        return this.parent.readBytes(byteBuffer);
    }

    @Override
    public ByteBuf readBytes(OutputStream outputStream, int i) throws IOException {
        return this.parent.readBytes(outputStream, i);
    }

    @Override
    public int readBytes(GatheringByteChannel gatheringByteChannel, int i) throws IOException {
        return this.parent.readBytes(gatheringByteChannel, i);
    }

    @Override
    public CharSequence readCharSequence(int i, Charset charset) {
        return this.parent.readCharSequence(i, charset);
    }

    @Override
    public int readBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return this.parent.readBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf skipBytes(int i) {
        return this.parent.skipBytes(i);
    }

    @Override
    public ByteBuf writeBoolean(boolean bl) {
        return this.parent.writeBoolean(bl);
    }

    @Override
    public ByteBuf writeByte(int i) {
        return this.parent.writeByte(i);
    }

    @Override
    public ByteBuf writeShort(int i) {
        return this.parent.writeShort(i);
    }

    @Override
    public ByteBuf writeShortLE(int i) {
        return this.parent.writeShortLE(i);
    }

    @Override
    public ByteBuf writeMedium(int i) {
        return this.parent.writeMedium(i);
    }

    @Override
    public ByteBuf writeMediumLE(int i) {
        return this.parent.writeMediumLE(i);
    }

    @Override
    public ByteBuf writeInt(int i) {
        return this.parent.writeInt(i);
    }

    @Override
    public ByteBuf writeIntLE(int i) {
        return this.parent.writeIntLE(i);
    }

    @Override
    public ByteBuf writeLong(long l) {
        return this.parent.writeLong(l);
    }

    @Override
    public ByteBuf writeLongLE(long l) {
        return this.parent.writeLongLE(l);
    }

    @Override
    public ByteBuf writeChar(int i) {
        return this.parent.writeChar(i);
    }

    @Override
    public ByteBuf writeFloat(float f) {
        return this.parent.writeFloat(f);
    }

    @Override
    public ByteBuf writeDouble(double d) {
        return this.parent.writeDouble(d);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf) {
        return this.parent.writeBytes(byteBuf);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i) {
        return this.parent.writeBytes(byteBuf, i);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i, int j) {
        return this.parent.writeBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf writeBytes(byte[] bs) {
        return this.parent.writeBytes(bs);
    }

    @Override
    public ByteBuf writeBytes(byte[] bs, int i, int j) {
        return this.parent.writeBytes(bs, i, j);
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer byteBuffer) {
        return this.parent.writeBytes(byteBuffer);
    }

    @Override
    public int writeBytes(InputStream inputStream, int i) throws IOException {
        return this.parent.writeBytes(inputStream, i);
    }

    @Override
    public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i) throws IOException {
        return this.parent.writeBytes(scatteringByteChannel, i);
    }

    @Override
    public int writeBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return this.parent.writeBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf writeZero(int i) {
        return this.parent.writeZero(i);
    }

    @Override
    public int writeCharSequence(CharSequence charSequence, Charset charset) {
        return this.parent.writeCharSequence(charSequence, charset);
    }

    @Override
    public int indexOf(int i, int j, byte b) {
        return this.parent.indexOf(i, j, b);
    }

    @Override
    public int bytesBefore(byte b) {
        return this.parent.bytesBefore(b);
    }

    @Override
    public int bytesBefore(int i, byte b) {
        return this.parent.bytesBefore(i, b);
    }

    @Override
    public int bytesBefore(int i, int j, byte b) {
        return this.parent.bytesBefore(i, j, b);
    }

    @Override
    public int forEachByte(ByteProcessor byteProcessor) {
        return this.parent.forEachByte(byteProcessor);
    }

    @Override
    public int forEachByte(int i, int j, ByteProcessor byteProcessor) {
        return this.parent.forEachByte(i, j, byteProcessor);
    }

    @Override
    public int forEachByteDesc(ByteProcessor byteProcessor) {
        return this.parent.forEachByteDesc(byteProcessor);
    }

    @Override
    public int forEachByteDesc(int i, int j, ByteProcessor byteProcessor) {
        return this.parent.forEachByteDesc(i, j, byteProcessor);
    }

    @Override
    public ByteBuf copy() {
        return this.parent.copy();
    }

    @Override
    public ByteBuf copy(int i, int j) {
        return this.parent.copy(i, j);
    }

    @Override
    public ByteBuf slice() {
        return this.parent.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return this.parent.retainedSlice();
    }

    @Override
    public ByteBuf slice(int i, int j) {
        return this.parent.slice(i, j);
    }

    @Override
    public ByteBuf retainedSlice(int i, int j) {
        return this.parent.retainedSlice(i, j);
    }

    @Override
    public ByteBuf duplicate() {
        return this.parent.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return this.parent.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return this.parent.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.parent.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int i, int j) {
        return this.parent.nioBuffer(i, j);
    }

    @Override
    public ByteBuffer internalNioBuffer(int i, int j) {
        return this.parent.internalNioBuffer(i, j);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.parent.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int i, int j) {
        return this.parent.nioBuffers(i, j);
    }

    @Override
    public boolean hasArray() {
        return this.parent.hasArray();
    }

    @Override
    public byte[] array() {
        return this.parent.array();
    }

    @Override
    public int arrayOffset() {
        return this.parent.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return this.parent.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return this.parent.memoryAddress();
    }

    @Override
    public String toString(Charset charset) {
        return this.parent.toString(charset);
    }

    @Override
    public String toString(int i, int j, Charset charset) {
        return this.parent.toString(i, j, charset);
    }

    @Override
    public int hashCode() {
        return this.parent.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return this.parent.equals(object);
    }

    @Override
    public int compareTo(ByteBuf byteBuf) {
        return this.parent.compareTo(byteBuf);
    }

    @Override
    public String toString() {
        return this.parent.toString();
    }

    @Override
    public ByteBuf retain(int i) {
        return this.parent.retain(i);
    }

    @Override
    public ByteBuf retain() {
        return this.parent.retain();
    }

    @Override
    public ByteBuf touch() {
        return this.parent.touch();
    }

    @Override
    public ByteBuf touch(Object object) {
        return this.parent.touch(object);
    }

    @Override
    public int refCnt() {
        return this.parent.refCnt();
    }

    @Override
    public boolean release() {
        return this.parent.release();
    }

    @Override
    public boolean release(int i) {
        return this.parent.release(i);
    }

    /**
     * A functional interface to read a value from a {@link PacketByteBuf}.
     */
    @FunctionalInterface
    public interface Reader<T> extends Function<PacketByteBuf, T> {
        default PacketByteBuf.Reader<Optional<T>> asOptional() {
            return buf -> buf.readOptional(this);
        }
    }

    /**
     * A functional interface to write a value to a {@link PacketByteBuf}.
     */
    @FunctionalInterface
    public interface Writer<T> extends BiConsumer<PacketByteBuf, T> {
        default PacketByteBuf.Writer<Optional<T>> asOptional() {
            return (buf, optional) -> buf.writeOptional(optional, this);
        }
    }
}

