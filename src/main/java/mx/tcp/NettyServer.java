package mx.tcp;


/**
 * 方便的创建一个以及基于netty的服务器程序 
 */
public class NettyServer extends NettyBase{

	public NettyServer() {
		super(true);
	}
	
	/**
	 * 此方法为阻塞方法,直到所有端口都监听成功才返回
	 * @param bossNum
	 * @param workerNum
	 * @param bindPorts
	 * @param handlerCreater
	 * @throws Throwable
	 */
	public void start(int bossNum,int workerNum,int[] bindPorts,final ChannelHandlerCreater handlerCreater) throws Throwable{
		super.initServer(bossNum, workerNum, bindPorts, handlerCreater);
	}

}
