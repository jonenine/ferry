package mx.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;

public class NettyClient extends NettyBase{

	public NettyClient() {
		super(false);
	}

	public NettyClient init(int workerNum){
		this.initClient(workerNum);
		return this;
	}
	
	public Bootstrap getBootstrap(){
		return this.clientBootstrap;
	}
	
	private ChannelFuture _connect(String host,int port,final ChannelHandlerCreater handlerCreater){
		ChannelFuture channelFuture = clientBootstrap.connect(host,port);
		channelFuture.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()){
					ChannelHandler[] handlers = handlerCreater.create((SocketChannel) future.channel());
					future.channel().pipeline().addLast(handlers);
				}
			}
		});
		
		return channelFuture;
	}
	
	/**
	 * 同步的连接服务器端,返回的Channel对象可用于直接发送第一次对服务端的请求
	 */
	public Channel connect(String host,int port,ChannelHandlerCreater handlerCreater) throws Throwable{
		ChannelFuture cf = _connect(host,port,handlerCreater).sync();
		Throwable exp;
		if(!cf.isSuccess() && (exp = cf.cause())!=null){
			throw exp;
		}
		return cf.channel();
	}
	
	/**
	 * 异步连接,请使用如下代码来处理连接不上的情况
	 * channelFuture.channel().closeFuture().addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture future) throws Exception {
			
			}
        });
	 */
	public ChannelFuture connectAsync(String host,int port,final ChannelHandlerCreater handlerCreater){
		return _connect(host,port,handlerCreater);
	}
}
