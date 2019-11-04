package mx.wsFerryTCP.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TcpHandler extends ChannelInboundHandlerAdapter {

    final int connectId;
    final NettyClientHammal hammal;

    public TcpHandler(int connectId,NettyClientHammal hammal) {
        this.connectId = connectId;
        this.hammal = hammal;
    }

    volatile Channel channel;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    volatile ChannelHandlerContext context;

    /**
     * 同服务端的连接建立
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
    }

    /**
     * 当服务端发送过来数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf data = (ByteBuf) msg;
        byte[] bs = new byte[data.readableBytes()];
        data.readBytes(bs);
        hammal.sendDataToListen(connectId,bs);
    }

    /**
     * 同服务端的连接断掉
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        hammal.sendInactive(connectId);
    }

    /*----------------------------------------------------------*/

    /**
     * 关闭tcp连接
     */
    public void close(){
        try {
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 向tcp写入数据
     */
    public void write(byte[] bs){
        ByteBuf buf = Unpooled.wrappedBuffer(bs);
        if(context!=null){
            context.writeAndFlush(buf);
        }else{
            channel.writeAndFlush(buf);

        }

    }

}
