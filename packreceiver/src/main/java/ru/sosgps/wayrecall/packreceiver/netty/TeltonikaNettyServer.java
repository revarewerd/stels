package ru.sosgps.wayrecall.packreceiver.netty;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sosgps.wayrecall.avlprotocols.teltonika.TeltonikaParser;
import ru.sosgps.wayrecall.core.GPSData;
import ru.sosgps.wayrecall.data.IllegalImeiException;
import ru.sosgps.wayrecall.packreceiver.Netty3Server;
import ru.sosgps.wayrecall.packreceiver.PackProcessor;
import ru.sosgps.wayrecall.utils.FutureConverters;
import ru.sosgps.wayrecall.utils.io.CRC16;
import ru.sosgps.wayrecall.utils.io.RichDataInput;
import ru.sosgps.wayrecall.utils.io.Utils;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 13.03.13
 * Time: 21:42
 * To change this template use File | Settings | File Templates.
 */
public class TeltonikaNettyServer extends Netty3Server {

    @Autowired
    public PackProcessor store;

    protected ChannelPipelineFactory getPipelineFactory() {
        return new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {

                TeltonikaFirstStepDecoder firstStepDecoder = new TeltonikaFirstStepDecoder();
                TeltonikaAvlDataDecoder avlDataDecoder = new TeltonikaAvlDataDecoder(firstStepDecoder, store);

                return Channels.pipeline(
                        new LoggingHandler(TeltonikaNettyServer.class),
                        new ReadTimeoutHandler(timer, 30, TimeUnit.MINUTES),
                        firstStepDecoder,
                        avlDataDecoder
                );
            }
        };
    }

    private static void clientTest() throws IOException, InterruptedException {
        Socket socket = new Socket("localhost", 9012);

        OutputStream socketOut = socket.getOutputStream();
        InputStream socketin = socket.getInputStream();

        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayStream);

        dataOutputStream.writeShort(15);
        dataOutputStream.writeBytes("352848021741364");
        byte[] bytes = Utils.asBytesHex("08 19 00 00 01 3d 5a ba bc 18 00 16 90 e9 60 21 30 0a 80 00 a7 01 0f 04 00 " +
                "09 00 04 03 01 01 b3 00 b4 00 01 42 33 ce 00 00 00 00 01 3d 5a ba a8 0e 00 16 90 f0 00 21 30 09 00 " +
                "00 a7 01 05 08 00 0d 00 04 03 01 01 b3 00 b4 00 01 42 33 db 00 00 00 00 01 3d 5a b9 f7 b0 00 16 90 " +
                "f2 20 21 30 0f 40 00 a1 00 ef 08 00 06 00 04 03 01 01 b3 00 b4 00 01 42 34 37 00 00 00 00 01 3d 5a " +
                "b9 eb 76 00 16 90 f3 60 21 30 10 c0 00 a1 00 fa 06 00 06 00 04 03 01 01 b3 00 b4 00 01 42 34 16 00 " +
                "00 00 00 01 3d 5a b8 dd 6c 00 16 91 3b c0 21 2f fd 80 00 9c 01 0a 06 00 06 00 04 03 01 01 b3 00 b4 " +
                "00 01 42 33 f5 00 00 00 00 01 3d 5a b8 d9 02 00 16 91 3b c0 21 2f fe 80 00 9d 01 15 06 00 08 00 04 " +
                "03 01 01 b3 00 b4 00 01 42 33 f3 00 00 00 00 01 3d 5a b8 b9 40 00 16 91 49 e0 21 2f f8 40 00 9d 00 " +
                "fa 05 00 0c 00 04 03 01 01 b3 00 b4 00 01 42 34 35 00 00 00 00 01 3d 5a b8 b0 ee 00 16 91 4f 80 21 " +
                "2f f9 00 00 9d 00 ef 05 00 06 00 04 03 01 01 b3 00 b4 00 01 42 34 20 00 00 00 00 01 3d 5a b8 62 4c " +
                "00 16 91 48 40 21 2f f3 00 00 9b 00 4e 09 00 06 00 04 03 01 01 b3 00 b4 00 01 42 34 2f 00 00 00 00 " +
                "01 3d 5a b8 56 12 00 16 91 43 80 21 2f f3 40 00 9b 00 5f 09 00 0c 00 04 03 01 01 b3 00 b4 00 01 42 " +
                "34 21 00 00 00 00 01 3d 5a b8 49 d8 00 16 91 36 c0 21 2f f5 40 00 9b 00 55 09 00 0f 00 04 03 01 01 " +
                "b3 00 b4 00 01 42 33 df 00 00 00 00 01 3d 5a b8 41 86 00 16 91 2e 40 21 2f f5 00 00 9b 00 61 09 00 " +
                "0b 00 04 03 01 01 b3 00 b4 00 01 42 34 1f 00 00 00 00 01 3d 5a b8 39 34 00 16 91 28 20 21 2f f5 80 " +
                "00 9b 00 6b 09 00 09 00 04 03 01 01 b3 00 b4 00 01 42 34 2e 00 00 00 00 01 3d 5a b8 34 ca 00 16 91 " +
                "25 40 21 2f f5 80 00 9b 00 7e 09 00 08 00 04 03 01 01 b3 00 b4 00 01 42 34 30 00 00 00 00 01 3d 5a " +
                "b8 28 90 00 16 91 20 60 21 2f f7 00 00 9a 00 9c 09 00 08 00 04 03 01 01 b3 00 b4 00 01 42 34 2d 00 " +
                "00 00 00 01 3d 5a b8 20 3e 00 16 91 1f 20 21 2f f8 c0 00 9a 00 85 08 00 08 00 04 03 01 01 b3 00 b4 " +
                "00 01 42 33 d0 00 00 00 00 01 3d 5a b8 1b d6 00 16 91 1e 60 21 2f f9 c0 00 9a 00 78 08 00 08 00 04 " +
                "03 01 01 b3 00 b4 00 01 42 34 3e 00 00 00 00 01 3d 5a b8 13 86 00 16 91 1c 80 21 2f fa 80 00 9a 00 " +
                "6b 08 00 0d 00 04 03 01 01 b3 00 b4 00 01 42 34 13 00 00 00 00 01 3d 5a b7 ff 78 00 16 91 0e c0 21 " +
                "2f fd 40 00 9a 00 81 08 00 0a 00 04 03 01 01 b3 00 b4 00 01 42 34 1d 00 00 00 00 01 3d 5a b7 f3 3e " +
                "00 16 91 0b 00 21 2f ff 00 00 9b 00 8f 07 00 0a 00 04 03 01 01 b3 00 b4 00 01 42 34 2a 00 00 00 00 " +
                "01 3d 5a b7 ea ec 00 16 91 09 00 21 30 00 80 00 9a 00 81 08 00 09 00 04 03 01 01 b3 00 b4 00 01 42 " +
                "34 1f 00 00 00 00 01 3d 5a b7 de b2 00 16 91 04 c0 21 30 02 40 00 99 00 71 08 00 0c 00 04 03 01 01 " +
                "b3 00 b4 00 01 42 33 c2 00 00 00 00 01 3d 5a b7 da 48 00 16 91 02 c0 21 30 02 80 00 99 00 8d 08 00 " +
                "11 00 04 03 01 01 b3 00 b4 00 01 42 33 ee 00 00 00 00 01 3d 5a b7 d5 de 00 16 91 01 00 21 30 03 80 " +
                "00 9a 00 97 07 00 14 00 04 03 01 01 b3 00 b4 00 01 42 34 37 00 00 00 00 01 3d 5a b7 d1 74 00 16 90 " +
                "ff 80 21 30 05 40 00 9b 00 b0 08 00 17 00 04 03 01 01 b3 00 b4 00 01 42 34 0a 00 00 19");
        dataOutputStream.writeInt(0x00);
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
        dataOutputStream.writeInt(53029);

        byte[] bytes1 = byteArrayStream.toByteArray();


        for (int i = 0; i < 20; i++) {
            socketOut.write(bytes1[i]);
            socketOut.flush();
            Thread.sleep(100);
        }

        for (int i = 20; i < bytes1.length; i++) {
            socketOut.write(bytes1[i]);
            socketOut.flush();
            Thread.sleep(5);
        }

        DataInput datain = new DataInputStream(socketin);

        byte b = datain.readByte();
        int i = datain.readInt();

        if (b != 1 || i != 25) {
            throw new IllegalArgumentException("not correct answer from server");
        }

    }

}

class TeltonikaFirstStepDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(
            TeltonikaFirstStepDecoder.class.getName());

    public TeltonikaFirstStepDecoder() {
        super(Integer.MAX_VALUE, 4, 4, 4, 0);
    }

    private String IMEI = null;

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        if (IMEI == null) {
            try {
                if (buf.readableBytes() < 4) {
                    return null;
                }
                buf.markReaderIndex();
                int length = buf.readShort();
                logger.trace("length={}", length);
                if (buf.readableBytes() < length) {
                    buf.resetReaderIndex();
                    return null;
                }

                byte[] imeibytes = new byte[length];
                buf.readBytes(imeibytes);

                IMEI = new String(imeibytes);

                logger.trace("IMEI={}", IMEI);

                ChannelBuffer buffer = ChannelBuffers.buffer(1);
                buffer.writeByte(0x01);
                channel.write(buffer);
            } catch (Exception e) {
                logger.warn("Exception " + e + " was thrown while processing incomingdata: " + Utils.toHexString
                        (buf.array(), ""));
                throw e;
            }
        }

        return super.decode(ctx, channel, buf);
    }

    public String getIMEI() {
        return IMEI;
    }
}


class TeltonikaAvlDataDecoder extends SimpleChannelUpstreamHandler {

    ExecutionContext executionContext = FutureConverters.fromExecutor(MoreExecutors.sameThreadExecutor());

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(
            TeltonikaAvlDataDecoder.class.getName());

    private TeltonikaFirstStepDecoder firstStepDecoder = null;

    private PackProcessor store;

    TeltonikaAvlDataDecoder(TeltonikaFirstStepDecoder firstStepDecoder, PackProcessor store) {
        this.firstStepDecoder = firstStepDecoder;
        this.store = store;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

        if (e.getMessage() instanceof ChannelBuffer) {
            ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();

            try {
                try (ChannelBufferInputStream is = new ChannelBufferInputStream(channelBuffer)) {

                    RichDataInput input = new RichDataInput((DataInput) is);

                    int size = TeltonikaParser.readPrefix(input);
                    logger.trace("incoming pack size =" + size);
                    byte[] avlDataArray = input.readNumberOfBytes(size);
                    int CRC = TeltonikaParser.readCRC(input);
                    logger.trace("avlDataArray = " + Utils.toHexString(avlDataArray, ""));
                    int calcedCRC = CRC16.calc(avlDataArray);
                    logger.trace("calkedCrc1 = " + String.format("%x", calcedCRC));
//                    logger.trace("calkedCrc2 = " + String.format("%x", CRC16CCITT.calc(avlDataArray))) ;
//                    logger.trace("calkedCrc3 = " + String.format("%x", CRC16CCITT.invertedKermit(avlDataArray))) ;
                    logger.trace("CRC = " + String.format("%x", CRC));
                    if (calcedCRC != CRC)
                        logger.warn("invalid CRC:" + calcedCRC + " expected " + CRC + " data=" +
                                Utils.toHexString(avlDataArray, ""));

                    List<GPSData> gpsDatas = TeltonikaParser.readAVLDataArray(new RichDataInput(avlDataArray),
                            firstStepDecoder.getIMEI());

                    final List<Future<GPSData>> futures = new ArrayList<Future<GPSData>>(gpsDatas.size());

                    for (GPSData gpsData : gpsDatas) {
                        futures.add(store.addGpsDataAsync(gpsData));
                    }

                    final Future<Collection<GPSData>> collectionFuture = FutureConverters.sequence(futures, executionContext);

                    FutureConverters.addCallBack(collectionFuture, new FutureCallback<Collection<GPSData>>() {
                        @Override
                        public void onSuccess(@Nullable Collection<GPSData> gpsDatas) {
                            ChannelBuffer outBuffer = ChannelBuffers.buffer(4);
                            outBuffer.writeInt(gpsDatas.size());
                            e.getChannel().write(outBuffer);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                             if(t instanceof IllegalImeiException){
                                 IllegalImeiException ime = (IllegalImeiException) t;
                                 logger.warn("IllegalImeiException:" + ime.getImei() + " " + ime.toString());
                                 e.getChannel().close();
                             }
                            else{
                                 logger.warn(t.getMessage(), t);
                                 e.getChannel().close();
                             }
                        }
                    });

                }

            } catch (IllegalImeiException ime) {
                logger.warn("IllegalImeiException:" + ime.getImei() + " " + ime.toString());
                e.getChannel().close();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        } else {
            super.messageReceived(ctx, e);
        }

    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        final Throwable cause = e.getCause();
        if (!(cause instanceof ReadTimeoutException))
            logger.debug(
                    "Unexpected exception from downstream.",
                    cause);
        e.getChannel().close();
    }

}