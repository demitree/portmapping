package main.V2;


import main.V1.Constant;
import main.V1.MapInfo;
import main.V1.Utils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Set;

/**
* Created by I002008 on 2014/9/18.
*/
public class LocalService implements Runnable {
    private static Logger logger = Logger.getLogger(Utils.class);
    private StringBuffer log;
    private MapInfo mapInfo;
    private ServerSocketChannel ssc;

    private volatile boolean shutdown = false;
    private Selector selector;
    private Set<Socket> sockets;




    public LocalService(MapInfo mi) throws IOException {
        mapInfo = mi;
        selector = Selector.open();
        sockets = new HashSet<Socket>();
        log = new StringBuffer();
    }

    @Override
    public void run() {
        this.listen();

        while (keepRun()) {
            int selected_count = 0;
            try {
                selected_count = selector.select();
                if (selected_count > 0)
                    processSelectedKeys();
                if (shutdown) {
                    selector.close();
                    break;
                }
                closeSockets();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }



    public void listen() {
        try {
            ssc = ServerSocketChannel.open();
            InetSocketAddress isa = new InetSocketAddress("localhost",mapInfo.getLocalPort());
            ssc.bind(isa);
            // unblocking i/o mode
            ssc.configureBlocking(false);
            ssc.register(selector,SelectionKey.OP_ACCEPT);
            logger.info("start listening...");
        } catch (IOException e) {
            Utils.printErrorLog(e);
        }

    }


    private void processSelectedKeys() throws IOException {
        for (SelectionKey sk: selector.selectedKeys()) {
            Object attachment = sk.attachment();
            if (sk.isAcceptable()) {
                accept();
            } else if (attachment instanceof ServiceChannel && sk.isConnectable()) {
                connected((ServiceChannel) attachment);
            } else if (attachment instanceof ClientChannel && sk.isReadable()) {
                System.out.println("clientread");
                readClient((ClientChannel) attachment);
            } else if (attachment instanceof ClientChannel && sk.isWritable()) {
                System.out.println("writeclient");
                writeClient((ClientChannel) attachment);
            } else if (attachment instanceof ServiceChannel && sk.isReadable()) {
                System.out.println("readService");
                readService((ServiceChannel) attachment);
            } else if (attachment instanceof ServiceChannel && sk.isWritable()) {
                System.out.println("writeservice");
                writeService((ServiceChannel) attachment);
            } else {
                logger.info("Unknown action");
            }
        }
        selector.selectedKeys().clear();
    }



    //close all the sockets including serversocket
    private void closeSockets() throws IOException {
        for (Socket s : sockets) {
            if (s != null) {
                s.close();
            }
        }
    }

    private void accept() throws IOException{
        SocketChannel sc = ssc.accept();
        connectService(new ClientChannel(sc));
    }

    private void connectService(ClientChannel clientChannel) throws IOException{
        SocketChannel sc = SocketChannel.open();

        InetSocketAddress isa = new InetSocketAddress(mapInfo.getDesIp(),mapInfo.getDesPort());
        sc.configureBlocking(false);
        sc.connect(isa);

        sc.register(selector, SelectionKey.OP_CONNECT, new ServiceChannel(sc,clientChannel));
        logger.info("New connection: " + clientChannel.socketChannel);

    }


    private void connected(ServiceChannel serviceChannel) throws IOException{
        try {
            serviceChannel.socketChannel.finishConnect();
            serviceChannel.socketChannel.register(selector,SelectionKey.OP_READ,serviceChannel);
            serviceChannel.clientChannel.socketChannel.register(selector, SelectionKey.OP_READ, serviceChannel.clientChannel);

        } catch (ConnectException e) {
            closeServiceChannel(serviceChannel);
            Utils.printErrorLog(e);
            return;
        }

        synchronized (sockets) {
            sockets.add(serviceChannel.socketChannel.socket());
            sockets.add(serviceChannel.clientChannel.socketChannel.socket());
        }

    }

    private void closeServiceChannel(ServiceChannel sc) {
        try {
            if(sc.socketChannel!=null) {
                sc.socketChannel.close();
                sockets.remove(sc.socketChannel);
            }
            if(sc.clientChannel.socketChannel!=null) {
                sc.clientChannel.socketChannel.close();
                sockets.remove(sc.clientChannel.socketChannel);
            }
        } catch (IOException e) {
            Utils.printErrorLog(e);
        }
    }


    private void readClient(ClientChannel cc) {
        try {

            int read = cc.socketChannel.read(cc.buffer);
            if (read == -1) {
                closeServiceChannel(cc.serviceChannel);
                return;
            }
            logger.info("get data from client"+ cc.buffer.toString());
            cc.buffer.flip();
            cc.socketChannel.register(selector, 0, cc);
            cc.serviceChannel.socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, cc.serviceChannel);

        } catch (IOException e) {
            Utils.printErrorLog(e);
        }
    }


    private void writeClient(ClientChannel cc) {
        try {
            int write = cc.socketChannel.write(cc.serviceChannel.buffer);
            if (!cc.serviceChannel.buffer.hasRemaining()) {
                cc.serviceChannel.buffer.clear();
                cc.socketChannel.register(selector, SelectionKey.OP_READ, cc);
                cc.serviceChannel.socketChannel.register(selector, SelectionKey.OP_READ, cc.serviceChannel);
            }
        } catch (ClosedChannelException e) {
            Utils.printErrorLog(e);
        } catch (IOException e) {
            Utils.printErrorLog(e);
        }
    }

    private void readService(ServiceChannel sc) {
        try {
            int read = sc.socketChannel.read(sc.buffer);
            if (read == -1) {
                closeServiceChannel(sc);
                return;
            }
            sc.buffer.flip();
            sc.socketChannel.register(selector, 0, sc);
            sc.clientChannel.socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, sc.clientChannel);
        } catch (IOException e) {
            Utils.printErrorLog(e);
        }
    }

    private void writeService(ServiceChannel sc) {
        try {
            int write = sc.socketChannel.write(sc.clientChannel.buffer);
            if (!sc.clientChannel.buffer.hasRemaining()) {
                sc.clientChannel.buffer.clear();
                sc.socketChannel.register(selector, SelectionKey.OP_READ, sc);
                sc.clientChannel.socketChannel.register(selector, SelectionKey.OP_READ, sc.clientChannel);
            }
        } catch (ClosedChannelException e) {
            Utils.printErrorLog(e);
        } catch (IOException e) {
            Utils.printErrorLog(e);
        }
    }

    private boolean keepRun() {
        return true;
    }

    class ServiceChannel {
        public SocketChannel socketChannel;
        public ClientChannel clientChannel;
        public ByteBuffer buffer;
        public ServiceChannel(SocketChannel sc, ClientChannel cc) {
            socketChannel = sc;
            clientChannel = cc;
            buffer = ByteBuffer.allocate(Constant.BUF_SIZE);
            cc.serviceChannel = this;
        }

    }

    class ClientChannel {
        public SocketChannel socketChannel;
        public ServiceChannel serviceChannel;
        public ByteBuffer buffer;
        public ClientChannel(SocketChannel sc) {
            socketChannel = sc;
            buffer = ByteBuffer.allocate(Constant.BUF_SIZE);
        }
    }

    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            shutdown();
            try {
                this.join();
            } catch (InterruptedException ex) {
                Utils.printErrorLog(ex);
            }
        }
    }

    public void shutdown() {
        shutdown = true;
        selector.wakeup();
    }
}
