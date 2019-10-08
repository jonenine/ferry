package mx.wsFerryTCP.tcp;


import com.google.common.base.Throwables;
import io.netty.channel.*;
import mx.tcp.NettyBase;
import mx.tcp.NettyClient;
import mx.wsFerryTCP.ws.HammalBase;
import mx.wsFerryTCP.ws.IWriter;
import mx.wsFerryTCP.ws.ListenPortToServerIpPortMap;
import mx.wsFerryTCP.ws.WsClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 和ws客户端WsClient对象是一对一的关系
 * 和tcp listen是一对一关系
 * 和"本地网络"中服务端ip和port是一对一关系
 * 维护对服务端ip和port的所有连接
 */
public class NettyClientHammal extends HammalBase {

    protected static final Logger logger = LogManager.getLogger(NettyClientHammal.class);

    private volatile NettyClient nc;

    @Override
    public void setWriter(IWriter writer) {
        super.setWriter(writer);
        nc = new NettyClient();
        //仅仅是转发数据,1个线程足以,5秒钟连接不上closeFuture的监听器就会接收到错误
        nc.init(1).getBootstrap().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
    }

    final Map<Integer,TcpHandler> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void onListenConnActive(int connectId) {
        //
        try {
            TcpHandler handler = new TcpHandler(connectId,this);
            //查找listenPort对应的"本地网络"中的服务端ip和端口
            String[] ipPort = ListenPortToServerIpPortMap.getIpPort(((WsClient)writer).listenPort);
            //向tcp服务端发起请求
            Channel channel = nc.connect(ipPort[0], Integer.parseInt(ipPort[1]), new NettyBase.ChannelHandlerCreater() {
                @Override
                public ChannelHandler[] create(io.netty.channel.socket.SocketChannel ch) {
                    return new ChannelHandler[]{handler};
                }
            });
            //一旦连接失败,要从handlerMap去掉
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    handlerMap.remove(connectId);
                }
            });

            //将connectId和handler建立对应关系
            handlerMap.put(connectId,handler);
        } catch (Throwable e) {
            logger.error(Throwables.getStackTraceAsString(e));

            //向服务端发起连接失败,发断连接消息
            sendInactive(connectId);
        }
    }

    @Override
    public void onListenConnInactive(int connectId) {
        TcpHandler handler = handlerMap.get(connectId);
        if(handler != null){
            handler.close();
        }
    }

    @Override
    public void onReceiveDataFromListenConn(int connectId, byte[] bs) {
        TcpHandler handler = handlerMap.get(connectId);
        if(handler!=null){
            handler.write(bs);
        }
    }


    @Override
    public void doOnWsClose() {
        Iterator<TcpHandler> it = handlerMap.values().iterator();
        while(it.hasNext()){
            it.next().close();
        }
        handlerMap.clear();
    }
}
