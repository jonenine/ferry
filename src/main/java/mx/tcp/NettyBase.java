package mx.tcp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 可作为tcp客户端,也可以作为服务器端 
 */
public abstract class NettyBase {
	
	protected static final Logger logger = LogManager.getLogger(NettyBase.class);

	/**
	 * 是否作为tcp服务端,否则就是客户端
	 */
	final boolean isTCPServer;
	
	public NettyBase(boolean isServer) {
		this.isTCPServer = isServer;
	}

	public static interface ChannelHandlerCreater{
		ChannelHandler[] create(SocketChannel ch);
	}
	
	ServerBootstrap  serverBootstrap;
	EventLoopGroup   serverBossGroup;
	EventLoopGroup   serverrWorkerGroup;
	ChannelFuture[]  serverFutures;
	
	/**
	 * 此方法为阻塞方法,直到所有端口都监听成功才返回
	 * @param bossNum
	 * @param workerNum
	 * @param bindPorts
	 * @param handlerCreater
	 * @throws Throwable
	 */
	protected synchronized void initServer(int bossNum,int workerNum,int[] bindPorts,final ChannelHandlerCreater handlerCreater) throws Throwable{
		if(serverBossGroup!=null) return;
		
		if(isLinux){
			serverBossGroup = new EpollEventLoopGroup(bossNum);
			serverrWorkerGroup = new EpollEventLoopGroup(workerNum);
		}else{
			serverBossGroup = new NioEventLoopGroup(bossNum);
			//netty在windows上线程轮询有问题,不要超过核心数的两倍,低了到核心数1/2又出现同样的问题
			serverrWorkerGroup = new NioEventLoopGroup(workerNum);
		}
		
		serverBootstrap = new ServerBootstrap(); 
		
		serverBootstrap.group(serverBossGroup, serverrWorkerGroup)
		 .channel(isLinux?EpollServerSocketChannel.class:NioServerSocketChannel.class) 
		 .childHandler(new ChannelInitializer<SocketChannel>() { 
		     public void initChannel(SocketChannel ch) throws Exception {
		    	 ChannelHandler[] handers;
				try {
					handers = handlerCreater.create(ch);
					ch.pipeline().addLast(handers);
				} catch (Exception e) {
					logger.error(Throwables.getStackTraceAsString(e));
					ch.close();
				}
		        
		     }
		 })
		 .childOption(ChannelOption.SO_KEEPALIVE, true)
		 .childOption(ChannelOption.TCP_NODELAY, true)
		 .childOption(ChannelOption.SO_SNDBUF,  1024*256)
		 .childOption(ChannelOption.SO_RCVBUF,  1024*1024*64);
		
		serverFutures = new ChannelFuture[bindPorts.length];
		//绑定多个端口
		for(int i=0;i<bindPorts.length;i++){
			ChannelFuture future = serverFutures[i] = serverBootstrap.bind(bindPorts[i]).sync(); 
			Throwable exp;
			if(!future.isSuccess() && (exp = future.cause())!=null){
				throw exp;
			}
		}
	}
	
	Bootstrap      clientBootstrap; 
	EventLoopGroup clientWorkerGroup;
	protected synchronized void initClient(int workerNum){
		if(clientBootstrap!=null) return;
		
		clientBootstrap = new Bootstrap();
		clientWorkerGroup = isLinux?new EpollEventLoopGroup(workerNum):new NioEventLoopGroup(workerNum);
		clientBootstrap.group(clientWorkerGroup).channel(isLinux?EpollSocketChannel.class:NioSocketChannel.class) ;  
		clientBootstrap.option(ChannelOption.SO_KEEPALIVE, true) ;
	    clientBootstrap.option(ChannelOption.TCP_NODELAY,  true);
	    clientBootstrap.option(ChannelOption.SO_SNDBUF,    1024*256) ;
	    clientBootstrap.option(ChannelOption.SO_RCVBUF,    1024*1024*64);
	    /**
	     * 不写会报错
	     */
	    clientBootstrap.handler(new ChannelInitializer<SocketChannel>(){
			protected void initChannel(SocketChannel ch) throws Exception {
				
			}
		});
	}
	
	/**
	 * 楞关,没有验证过,也没有这样的需求
	 * 实际都是关进程
	 */
	public synchronized void destory(){
		if(serverFutures!=null){
			for(int i=0;i<serverFutures.length;i++){
				try {
					serverFutures[i].channel().closeFuture();
				} catch (Exception e) {
					logger.error(Throwables.getStackTraceAsString(e));
				}
			}
			serverFutures = null;
		}
		
		if(serverBossGroup!=null){
			serverBossGroup.shutdownGracefully();
			serverBossGroup = null;
		}
		
		if(serverrWorkerGroup!=null){
			serverrWorkerGroup.shutdownGracefully();
			serverrWorkerGroup = null;
		}
		
		if(clientWorkerGroup!=null){
			clientWorkerGroup.shutdownGracefully();
			clientWorkerGroup = null;
		}
		
	}
	
	/*-----------------------------------静态工具---------------------------------*/
	
	protected static final boolean isLinux = !isWindows();
	
	protected static boolean isWindows() {  
	    return System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1;  
	} 
	
	public static final String getLocalAddress(SocketChannel ch){
		return getIpFromNettyAddress(ch.localAddress().toString());
	}
	
	public static final String getRemoteAddress(SocketChannel ch){
		return getIpFromNettyAddress(ch.remoteAddress().toString());
	}
	
	public static final String getIpFromNettyAddress(String address){
		return address.substring(1, address.indexOf(":"));
	}
}







