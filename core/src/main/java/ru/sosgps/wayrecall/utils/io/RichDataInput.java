package ru.sosgps.wayrecall.utils.io;

import java.io.*;


/**
 *  @deprecated use Netty's {@link io.netty.buffer.ByteBuf}
 */
@Deprecated
public class RichDataInput implements DataInput {

    private DataInput wrapped;

    public DataInput getWrapped() {
        return wrapped;
    }

    public RichDataInput(DataInput wrapped) {
        this.wrapped = wrapped;
    }

    public RichDataInput(InputStream is) {
        this((DataInput) new DataInputStream(is));
    }

    public RichDataInput(byte[] byteArr) {
        this(new ByteArrayInputStream(byteArr));
    }

    public static RichDataInput fromDataInput(DataInput wrapped) {
        if (wrapped instanceof RichDataInput)
            return (RichDataInput) wrapped;
        else
            return new RichDataInput(wrapped);
    }

    public void readFully(byte[] b) throws IOException {
        wrapped.readFully(b);
    }

    public short readShort() throws IOException {
        return wrapped.readShort();
    }

    public char readChar() throws IOException {
        return wrapped.readChar();
    }

    public int readInt() throws IOException {
        return wrapped.readInt();
    }

    public String readUTF() throws IOException {
        return wrapped.readUTF();
    }

    public float readFloat() throws IOException {
        return wrapped.readFloat();
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        wrapped.readFully(b, off, len);
    }

    public int readUnsignedByte() throws IOException {
        return wrapped.readUnsignedByte();
    }

    public String readLine() throws IOException {
        return wrapped.readLine();
    }

    public byte readByte() throws IOException {
        return wrapped.readByte();
    }

    public int skipBytes(int n) throws IOException {
        return wrapped.skipBytes(n);
    }

    public long readUnsignedInt() throws IOException {
        long val = (long) readInt();
        val &= 0X00000000FFFFFFFFL;
        return val;
    }

    public int readUnsignedShort() throws IOException {
        return wrapped.readUnsignedShort();
    }

    public int readUnsignedShortLE() throws IOException {
        byte[] bytes = this.readNumberOfBytes(2);
        return bytes[0] & 0xff | (bytes[1] & 0xff) << 8;
    }

    public long readLong() throws IOException {
        return wrapped.readLong();
    }

    public double readDouble() throws IOException {
        return wrapped.readDouble();
    }

    public boolean readBoolean() throws IOException {
        return wrapped.readBoolean();
    }

    public double longToDouble() throws IOException {
        return Double.longBitsToDouble(Long.reverseBytes(this.readLong()));
    }

    public byte[] readNumberOfBytes(int number) throws IOException {
        byte[] sizebytes = new byte[number];
        this.readFully(sizebytes);
        return sizebytes;
    }

    public byte[] readNullTerminated() throws IOException {
        return readUntil((byte) 0);
    }

    public byte[] readUntil(byte terminal) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        while (true) {
            byte readByte = this.readByte();
            if (readByte == terminal) {
                break;
            }
            bs.write(readByte);
        }
        byte[] bsba = bs.toByteArray();
        return bsba;
    }

}
