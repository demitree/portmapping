package main.V2;

import main.V1.Constant;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by I002008 on 2014/9/18.
 */
public class SessionChannel {
    private SocketChannel clientChannel;
    private SocketChannel serviceChannel;
    private ByteBuffer clientBuffer;
    private ByteBuffer serviceBuffer;

    public SessionChannel(SocketChannel cc) {
        clientChannel = cc;
        clientBuffer = ByteBuffer.allocate(Constant.BUF_SIZE);
    }

    public ByteBuffer getClientBuffer() {
        return clientBuffer;
    }

    public void setClientBuffer(ByteBuffer clientBuffer) {
        this.clientBuffer = clientBuffer;
    }

    public ByteBuffer getServiceBuffer() {
        return serviceBuffer;
    }

    public void setServiceBuffer(ByteBuffer serviceBuffer) {
        this.serviceBuffer = serviceBuffer;
    }

    public SocketChannel getClientChannel() {

        return clientChannel;
    }

    public void setClientChannel(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    public SocketChannel getServiceChannel() {
        return serviceChannel;
    }


    public void setServiceChannel(SocketChannel sc) {
        serviceChannel = sc;
        serviceBuffer = ByteBuffer.allocate(Constant.BUF_SIZE);
    }
}
