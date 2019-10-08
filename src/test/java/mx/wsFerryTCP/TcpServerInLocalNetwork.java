package mx.wsFerryTCP;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import mx.tcp.NettyBase;
import mx.tcp.NettyServer;
import mx.utils.ByteUtils;

/**
 * 模拟本地网络的tcp服务端
 */
public class TcpServerInLocalNetwork {

    public static void main(String[] args) {
        try {
            NettyServer echoServer = new NettyServer();
            echoServer.start(1, 1, new int[]{28888}, new NettyBase.ChannelHandlerCreater() {
                @Override
                public ChannelHandler[] create(SocketChannel ch) {
                    return new ChannelHandler[]{
                            new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx)throws Exception {
                                    super.channelActive(ctx);
                                    System.err.println("和本地网络28888端口服务建立连接");
                                    //响应
                                    ByteBuf bbf = Unpooled.buffer();
                                    bbf.writeBytes("来自内网服务器28888的问候".getBytes("GBK"));
                                    ctx.writeAndFlush(bbf);
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx)throws Exception {
                                    System.err.println("和本地网络28888端口连接断了");
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg)throws Exception {
                                    ByteBuf data = (ByteBuf) msg;
                                    byte[] bs = ByteUtils.getBytes(data);
                                    if(new String(bs).startsWith("close")){
                                        System.err.println("正在关闭连接");
                                        ctx.close();
                                    }else{
                                        System.err.println("本地网络28888端口接到\"摆渡\"过来额数据"+ ByteUtils.toHexString(data));
                                        ByteBuf bbf = Unpooled.buffer();
                                        bbf.writeBytes(data);
                                        //形成echo
                                        ctx.writeAndFlush(bbf);
                                    }
                                }
                            }
                    };
                }
            });
        } catch (Throwable throwable) {
        }
    }
}
