package ru.sosgps.wayrecall.packreceiver.netty;

import com.google.common.util.concurrent.FutureCallback;
import org.jboss.netty.buffer.*;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sosgps.wayrecall.core.GPSData;
import ru.sosgps.wayrecall.core.GPSDataConversions;
import ru.sosgps.wayrecall.data.IllegalImeiException;
import ru.sosgps.wayrecall.packreceiver.Netty3Server;
import ru.sosgps.wayrecall.packreceiver.PackProcessor;
import ru.sosgps.wayrecall.utils.FutureConverters;
import ru.sosgps.wayrecall.wialonparser.WialonPackage;
import ru.sosgps.wayrecall.wialonparser.WialonParser;

import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 12.03.13
 * Time: 20:31
 * To change this template use File | Settings | File Templates.
 */
public class WialonNettyServer extends Netty3Server {


    @Autowired
    public PackProcessor store;


    @Override
    protected ChannelPipelineFactory getPipelineFactory() {
        return () -> Channels.pipeline(
                new LoggingHandler(WialonNettyServer.class),
                new ReadTimeoutHandler(timer, 3, TimeUnit.MINUTES),
                new WialonLengthDecoder(),
                new WialonDecoderAndReceiver(store)
        );
    }
}

class WialonLengthDecoder extends LengthFieldBasedFrameDecoder {
    public WialonLengthDecoder() {
        super(Integer.MAX_VALUE, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, final ChannelBuffer buffer) throws Exception {
        return super.decode(ctx, channel, new ChannelBufferProxy(buffer) {
            public long getUnsignedInt(int index) {
                byte[] sizebytes = new byte[4];
                buffer.getBytes(index, sizebytes);

                int size = sizebytes[3] << 32
                        | sizebytes[2] << 16
                        | sizebytes[1] << 8
                        | sizebytes[0] & 0x000000ff;

                return size;
            }
        });
    }
}

class WialonDecoderAndReceiver extends SimpleChannelUpstreamHandler {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(
            WialonDecoderAndReceiver.class.getName());

    private static final Set<String> illegalImeis = new HashSet<>();

    private PackProcessor store;

    WialonDecoderAndReceiver(PackProcessor store) {
        this.store = store;
    }


    @Override
    public void messageReceived(
            ChannelHandlerContext ctx, final MessageEvent e) {
        // Send back the received message to the remote peer.
        ChannelBuffer message = (ChannelBuffer) e.getMessage();

        final ChannelBuffer channelBuffer = ChannelBuffers.buffer(1);
        try {
            try (ChannelBufferInputStream is = new ChannelBufferInputStream(message)) {
                WialonPackage wp = WialonParser.parsePackage((DataInput) is);

                FutureConverters.addCallBack(store.addGpsDataAsync(GPSDataConversions.fromWialonPackage(null, wp)), new FutureCallback<GPSData>() {
                    @Override
                    public void onSuccess(@Nullable GPSData result) {
                        channelBuffer.writeByte(0x11);
                        e.getChannel().write(channelBuffer);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (t instanceof IllegalImeiException) {
                            IllegalImeiException arg = (IllegalImeiException) t;
                            if (illegalImeis.add(arg.getImei()))
                                logger.warn("not processing package:" + arg.toString());
                            channelBuffer.writeByte(0x00);
                            e.getChannel().write(channelBuffer);
                        } else {
                            logger.warn(t.getMessage(), t);
                            e.getChannel().close();
                        }
                    }
                });
            }

        } catch (IOException e1) {
            throw new RuntimeException(e1);
        } catch (IllegalImeiException arg) {
            //TODO: сделать с этим что-то более приличное, например вынести в отдельный лог
            if (illegalImeis.add(arg.getImei()))
                logger.warn("not processing package:" + arg.toString());
            channelBuffer.writeByte(0x00);
            e.getChannel().write(channelBuffer);
        }

    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        final Throwable cause = e.getCause();
        if(!(cause instanceof ReadTimeoutException))
        logger.debug(
                "Unexpected exception from downstream.",
                cause);
        e.getChannel().close();
    }
}

class ChannelBufferProxy implements ChannelBuffer {
    private final ChannelBuffer buffer;

    public ChannelBufferProxy(ChannelBuffer buffer) {
        this.buffer = buffer;
    }

    public ChannelBufferFactory factory() {
        return buffer.factory();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public ByteOrder order() {
        return buffer.order();
    }

    public boolean isDirect() {
        return buffer.isDirect();
    }

    public int readerIndex() {
        return buffer.readerIndex();
    }

    public void readerIndex(int readerIndex) {
        buffer.readerIndex(readerIndex);
    }

    public int writerIndex() {
        return buffer.writerIndex();
    }

    public void writerIndex(int writerIndex) {
        buffer.writerIndex(writerIndex);
    }

    public void setIndex(int readerIndex, int writerIndex) {
        buffer.setIndex(readerIndex, writerIndex);
    }

    public int readableBytes() {
        return buffer.readableBytes();
    }

    public int writableBytes() {
        return buffer.writableBytes();
    }

    public boolean readable() {
        return buffer.readable();
    }

    public boolean writable() {
        return buffer.writable();
    }

    public void clear() {
        buffer.clear();
    }

    public void markReaderIndex() {
        buffer.markReaderIndex();
    }

    public void resetReaderIndex() {
        buffer.resetReaderIndex();
    }

    public void markWriterIndex() {
        buffer.markWriterIndex();
    }

    public void resetWriterIndex() {
        buffer.resetWriterIndex();
    }

    public void discardReadBytes() {
        buffer.discardReadBytes();
    }

    public void ensureWritableBytes(int writableBytes) {
        buffer.ensureWritableBytes(writableBytes);
    }

    public byte getByte(int index) {
        return buffer.getByte(index);
    }

    public short getUnsignedByte(int index) {
        return buffer.getUnsignedByte(index);
    }

    public short getShort(int index) {
        return buffer.getShort(index);
    }

    public int getUnsignedShort(int index) {
        return buffer.getUnsignedShort(index);
    }

    public int getMedium(int index) {
        return buffer.getMedium(index);
    }

    public int getUnsignedMedium(int index) {
        return buffer.getUnsignedMedium(index);
    }

    public int getInt(int index) {
        return buffer.getInt(index);
    }

    public long getUnsignedInt(int index) {
        return buffer.getUnsignedInt(index);
    }

    public long getLong(int index) {
        return buffer.getLong(index);
    }

    public char getChar(int index) {
        return buffer.getChar(index);
    }

    public float getFloat(int index) {
        return buffer.getFloat(index);
    }

    public double getDouble(int index) {
        return buffer.getDouble(index);
    }

    public void getBytes(int index, ChannelBuffer dst) {
        buffer.getBytes(index, dst);
    }

    public void getBytes(int index, ChannelBuffer dst, int length) {
        buffer.getBytes(index, dst, length);
    }

    public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        buffer.getBytes(index, dst, dstIndex, length);
    }

    public void getBytes(int index, byte[] dst) {
        buffer.getBytes(index, dst);
    }

    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        buffer.getBytes(index, dst, dstIndex, length);
    }

    public void getBytes(int index, ByteBuffer dst) {
        buffer.getBytes(index, dst);
    }

    public void getBytes(int index, OutputStream out, int length) throws IOException {
        buffer.getBytes(index, out, length);
    }

    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return buffer.getBytes(index, out, length);
    }

    public void setByte(int index, int value) {
        buffer.setByte(index, value);
    }

    public void setShort(int index, int value) {
        buffer.setShort(index, value);
    }

    public void setMedium(int index, int value) {
        buffer.setMedium(index, value);
    }

    public void setInt(int index, int value) {
        buffer.setInt(index, value);
    }

    public void setLong(int index, long value) {
        buffer.setLong(index, value);
    }

    public void setChar(int index, int value) {
        buffer.setChar(index, value);
    }

    public void setFloat(int index, float value) {
        buffer.setFloat(index, value);
    }

    public void setDouble(int index, double value) {
        buffer.setDouble(index, value);
    }

    public void setBytes(int index, ChannelBuffer src) {
        buffer.setBytes(index, src);
    }

    public void setBytes(int index, ChannelBuffer src, int length) {
        buffer.setBytes(index, src, length);
    }

    public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
        buffer.setBytes(index, src, srcIndex, length);
    }

    public void setBytes(int index, byte[] src) {
        buffer.setBytes(index, src);
    }

    public void setBytes(int index, byte[] src, int srcIndex, int length) {
        buffer.setBytes(index, src, srcIndex, length);
    }

    public void setBytes(int index, ByteBuffer src) {
        buffer.setBytes(index, src);
    }

    public int setBytes(int index, InputStream in, int length) throws IOException {
        return buffer.setBytes(index, in, length);
    }

    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return buffer.setBytes(index, in, length);
    }

    public void setZero(int index, int length) {
        buffer.setZero(index, length);
    }

    public byte readByte() {
        return buffer.readByte();
    }

    public short readUnsignedByte() {
        return buffer.readUnsignedByte();
    }

    public short readShort() {
        return buffer.readShort();
    }

    public int readUnsignedShort() {
        return buffer.readUnsignedShort();
    }

    public int readMedium() {
        return buffer.readMedium();
    }

    public int readUnsignedMedium() {
        return buffer.readUnsignedMedium();
    }

    public int readInt() {
        return buffer.readInt();
    }

    public long readUnsignedInt() {
        return buffer.readUnsignedInt();
    }

    public long readLong() {
        return buffer.readLong();
    }

    public char readChar() {
        return buffer.readChar();
    }

    public float readFloat() {
        return buffer.readFloat();
    }

    public double readDouble() {
        return buffer.readDouble();
    }

    public ChannelBuffer readBytes(int length) {
        return buffer.readBytes(length);
    }

    @Deprecated
    public ChannelBuffer readBytes(ChannelBufferIndexFinder indexFinder) {
        return buffer.readBytes(indexFinder);
    }

    public ChannelBuffer readSlice(int length) {
        return buffer.readSlice(length);
    }

    @Deprecated
    public ChannelBuffer readSlice(ChannelBufferIndexFinder indexFinder) {
        return buffer.readSlice(indexFinder);
    }

    public void readBytes(ChannelBuffer dst) {
        buffer.readBytes(dst);
    }

    public void readBytes(ChannelBuffer dst, int length) {
        buffer.readBytes(dst, length);
    }

    public void readBytes(ChannelBuffer dst, int dstIndex, int length) {
        buffer.readBytes(dst, dstIndex, length);
    }

    public void readBytes(byte[] dst) {
        buffer.readBytes(dst);
    }

    public void readBytes(byte[] dst, int dstIndex, int length) {
        buffer.readBytes(dst, dstIndex, length);
    }

    public void readBytes(ByteBuffer dst) {
        buffer.readBytes(dst);
    }

    public void readBytes(OutputStream out, int length) throws IOException {
        buffer.readBytes(out, length);
    }

    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return buffer.readBytes(out, length);
    }

    public void skipBytes(int length) {
        buffer.skipBytes(length);
    }

    @Deprecated
    public int skipBytes(ChannelBufferIndexFinder indexFinder) {
        return buffer.skipBytes(indexFinder);
    }

    public void writeByte(int value) {
        buffer.writeByte(value);
    }

    public void writeShort(int value) {
        buffer.writeShort(value);
    }

    public void writeMedium(int value) {
        buffer.writeMedium(value);
    }

    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    public void writeLong(long value) {
        buffer.writeLong(value);
    }

    public void writeChar(int value) {
        buffer.writeChar(value);
    }

    public void writeFloat(float value) {
        buffer.writeFloat(value);
    }

    public void writeDouble(double value) {
        buffer.writeDouble(value);
    }

    public void writeBytes(ChannelBuffer src) {
        buffer.writeBytes(src);
    }

    public void writeBytes(ChannelBuffer src, int length) {
        buffer.writeBytes(src, length);
    }

    public void writeBytes(ChannelBuffer src, int srcIndex, int length) {
        buffer.writeBytes(src, srcIndex, length);
    }

    public void writeBytes(byte[] src) {
        buffer.writeBytes(src);
    }

    public void writeBytes(byte[] src, int srcIndex, int length) {
        buffer.writeBytes(src, srcIndex, length);
    }

    public void writeBytes(ByteBuffer src) {
        buffer.writeBytes(src);
    }

    public int writeBytes(InputStream in, int length) throws IOException {
        return buffer.writeBytes(in, length);
    }

    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return buffer.writeBytes(in, length);
    }

    public void writeZero(int length) {
        buffer.writeZero(length);
    }

    public int indexOf(int fromIndex, int toIndex, byte value) {
        return buffer.indexOf(fromIndex, toIndex, value);
    }

    public int indexOf(int fromIndex, int toIndex, ChannelBufferIndexFinder indexFinder) {
        return buffer.indexOf(fromIndex, toIndex, indexFinder);
    }

    public int bytesBefore(byte value) {
        return buffer.bytesBefore(value);
    }

    public int bytesBefore(ChannelBufferIndexFinder indexFinder) {
        return buffer.bytesBefore(indexFinder);
    }

    public int bytesBefore(int length, byte value) {
        return buffer.bytesBefore(length, value);
    }

    public int bytesBefore(int length, ChannelBufferIndexFinder indexFinder) {
        return buffer.bytesBefore(length, indexFinder);
    }

    public int bytesBefore(int index, int length, byte value) {
        return buffer.bytesBefore(index, length, value);
    }

    public int bytesBefore(int index, int length, ChannelBufferIndexFinder indexFinder) {
        return buffer.bytesBefore(index, length, indexFinder);
    }

    public ChannelBuffer copy() {
        return buffer.copy();
    }

    public ChannelBuffer copy(int index, int length) {
        return buffer.copy(index, length);
    }

    public ChannelBuffer slice() {
        return buffer.slice();
    }

    public ChannelBuffer slice(int index, int length) {
        return buffer.slice(index, length);
    }

    public ChannelBuffer duplicate() {
        return buffer.duplicate();
    }

    public ByteBuffer toByteBuffer() {
        return buffer.toByteBuffer();
    }

    public ByteBuffer toByteBuffer(int index, int length) {
        return buffer.toByteBuffer(index, length);
    }

    public ByteBuffer[] toByteBuffers() {
        return buffer.toByteBuffers();
    }

    public ByteBuffer[] toByteBuffers(int index, int length) {
        return buffer.toByteBuffers(index, length);
    }

    public boolean hasArray() {
        return buffer.hasArray();
    }

    public byte[] array() {
        return buffer.array();
    }

    public int arrayOffset() {
        return buffer.arrayOffset();
    }

    public String toString(Charset charset) {
        return buffer.toString(charset);
    }

    public String toString(int index, int length, Charset charset) {
        return buffer.toString(index, length, charset);
    }

    @Deprecated
    public String toString(String charsetName) {
        return buffer.toString(charsetName);
    }

    @Deprecated
    public String toString(String charsetName, ChannelBufferIndexFinder terminatorFinder) {
        return buffer.toString(charsetName, terminatorFinder);
    }

    @Deprecated
    public String toString(int index, int length, String charsetName) {
        return buffer.toString(index, length, charsetName);
    }

    @Deprecated
    public String toString(int index, int length, String charsetName, ChannelBufferIndexFinder terminatorFinder) {
        return buffer.toString(index, length, charsetName, terminatorFinder);
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return buffer.equals(obj);
    }

    public int compareTo(ChannelBuffer buffer) {
        return buffer.compareTo(buffer);
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}