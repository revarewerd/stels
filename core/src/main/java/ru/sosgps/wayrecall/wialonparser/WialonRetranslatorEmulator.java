package ru.sosgps.wayrecall.wialonparser;

import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 */
public class WialonRetranslatorEmulator implements Runnable {

    private List<String> dataFiles;
    private int maxSleepTime = 300;
    private int port;

    public static void main(String[] args) throws IOException {
        WialonRetranslatorEmulator wialonRetranslatorEmulator =
                new WialonRetranslatorEmulator(
                        Arrays.asList(
                                "../data/withoutDoubling/readData1334935085764.dat",
                                "../data/withoutDoubling/readData1335443286095.wrp",
                                "../data/withoutDoubling/readData1335530876874.wrp"), 31313, 0);
        Thread thread = new Thread(wialonRetranslatorEmulator);

        thread.start();


        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (thread.isAlive()) {
            System.out.println("enter command");
            String line = in.readLine();

            if (line.equals("stop")) {
                System.out.println("stopping");
                wialonRetranslatorEmulator.stop();
                break;
            }


        }

        System.out.println("stopped");


    }

    private boolean work = true;

    public WialonRetranslatorEmulator(String dataFile, int port, int maxSleepTime) {
        this.dataFiles = Arrays.asList(dataFile);
        this.maxSleepTime = maxSleepTime;
        this.port = port;
    }

    public WialonRetranslatorEmulator(List<String> dataFiles, int port, int maxSleepTime) {
        this.dataFiles = dataFiles;
        this.maxSleepTime = maxSleepTime;
        this.port = port;
    }

    public void run() {
        Iterator<String> dfi = dataFiles.iterator();


        //Socket clientSocket = new Socket("localhost", 8888);
        //Socket clientSocket = new Socket(InetAddress.getByName("192.168.0.105"), 8888);
        //Socket clientSocket = new Socket(InetAddress.getByName("91.230.151.33"), 8888);
        Socket clientSocket = null;
        //Socket clientSocket = new Socket(InetAddress.getByName("2001:0:5ef5:79fb:2867:10f7:a419:68e1"), 8888);

        FileInputStream datatosent = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            String filename = dfi.next();
            System.out.println("WialonRetranslatorEmulator reading file: " + filename);
            //datatosent = new FileInputStream("C:\\Users\\nickl-new\\Documents\\forStels\\Seniel\\readData1334931237176.dat");
            //datatosent = new FileInputStream("../data/withoutDoubling/readData1334935085764.dat");
            datatosent = new FileInputStream(filename);

//            int[] ints = new int[]
//            {
//                0x74, 0x00, 0x00, 0x00, 0x33, 0x35, 0x33, 0x39, 0x37, 0x36, 0x30, 0x31, 0x33, 0x34, 0x34, 0x35, 0x34, 0x38, 0x35, 0x00, 0x4B, 0x0B, 0xFB, 0x70, 0x00, 0x00, 0x00, 0x03, 0x0B, 0xBB, 0x00, 0x00, 0x00, 0x27, 0x01, 0x02, 0x70, 0x6F, 0x73, 0x69, 0x6E, 0x66, 0x6F, 0x00, 0xA0, 0x27, 0xAF, 0xDF, 0x5D, 0x98, 0x48, 0x40, 0x3A, 0xC7, 0x25, 0x33, 0x83, 0xDD, 0x4B, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x5A, 0x40, 0x00, 0x36, 0x01, 0x46, 0x0B, 0x0B, 0xBB, 0x00, 0x00, 0x00, 0x12, 0x00, 0x04, 0x70, 0x77, 0x72, 0x5F, 0x65, 0x78, 0x74, 0x00, 0x2B, 0x87, 0x16, 0xD9, 0xCE, 0x97, 0x3B, 0x40, 0x0B, 0xBB, 0x00, 0x00, 0x00, 0x11, 0x01, 0x03, 0x61, 0x76, 0x6C, 0x5F, 0x69, 0x6E, 0x70, 0x75, 0x74, 0x73, 0x00, 0x00, 0x00, 0x00, 0x01
//            };
//
//            byte[] bytes = new byte[ints.length];
//
//            for (int i = 0; i < ints.length; i++)
//            {
//                System.out.format("%h ", ints[i]);
//                bytes[i] = (byte) (ints[i] & 0x000000ff);
//            }
//
//            datatosent = new ByteArrayInputStream(bytes);


            long startTime = System.currentTimeMillis();
            clientSocket = new Socket(InetAddress.getByName("localhost"), port);
            outputStream = clientSocket.getOutputStream();
            inputStream = clientSocket.getInputStream();

            byte[] buffer = new byte[1024];

            Random r = new Random();

            while (work) {

                if (datatosent.available() <= 0) {
                    if (dfi.hasNext()) {
                        datatosent.close();
                        filename = dfi.next();
                        System.out.println("WialonRetranslatorEmulator reading file: " + filename);
                        datatosent = new FileInputStream(filename);
                    } else {
                        System.out.println("EOF reached in " + (System.currentTimeMillis() - startTime) + " millis on file: " + filename);
                        break;
                    }

                }
                try {
                    WialonPackage pack = WialonParser.parsePackage(datatosent);

                    int s = 0;
                    while ((s = inputStream.available()) > 0) {
                        byte[] data = new byte[s];
                        int read1 = inputStream.read(data, 0, s);
                        //System.out.println("WialonRetranslatorEmulator read " + read1 + " bytes as response");                        
                        //System.out.println();

                    }
                    if (maxSleepTime > 0) {
                        Thread.sleep(r.nextInt(maxSleepTime));
                    }

                    outputStream.write(pack.rawData, 0, pack.rawData.length);
                    //System.out.println("WialonRetranslatorEmulator pack: " + pack.imei + " " + pack.time + " has been sent");
                } catch (java.io.EOFException e) {
                    System.out.println("WialonRetranslatorEmulator unexpected EOF on file " + filename);
                }
            }


        } catch (ConnectException e) {
            System.out.println("ConnectException on port:" + port);
            System.out.println(e.getMessage());
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace(System.out);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
            closeSilently(datatosent);
            closeSilently(clientSocket);
            System.out.println("Connection was closed");
        }
    }

    private void closeSilently(Socket clientSocket) {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(WialonRetranslatorEmulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void closeSilently(Closeable clientSocket) {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(WialonRetranslatorEmulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void stop() {
        work = false;
    }
}
