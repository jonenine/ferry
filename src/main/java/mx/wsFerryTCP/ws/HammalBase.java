package mx.wsFerryTCP.ws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 通过一个类来解耦
 * listenPort对serverIP和Port
 * 每一个connectId对应一个channelHandler,而且要注入这个channelHandler
 */
public abstract class HammalBase {

    protected static final Logger logger = LogManager.getLogger(HammalBase.class);

    protected volatile IWriter writer;

    public void setWriter(IWriter writer) {
        this.writer = writer;
    }

    /**
     * 向tcp listen的ws服务端send数据
     */
    public final void sendDataToListen(int connectId, byte[] bs){
        Message.DataMessage dm = new Message.DataMessage(connectId,bs);
        writer.write(dm.marshal());
    }

    /**
     * 向tcp listen的ws服务端send连接断掉消息
     */
    public final void sendInactive(int connectId){
        Message.ConnectInactiveMessage sim = new Message.ConnectInactiveMessage(connectId);
        writer.write(sim.marshal());
    }


    /**
     * connectId标识的一个连接在tcpListen上线
     * 在ferry target端也要根据配置连接ip和端口,如果不能成功创建连接,需要向tcpListen端发送connectInactive事件
     */
    public abstract void onListenConnActive(int connectId);
    /**
     * connectId标识的连接在tcpListen端断线
     * 在ferry target端也要断掉connectId标识的id
     */
    public abstract void onListenConnInactive(int connectId);

    /**
     * connectId标识的连接接收数据
     */
    public abstract void onReceiveDataFromListenConn(int connectId, byte[] bs);


    protected final AtomicBoolean destroy = new AtomicBoolean(false);
    /**
     * 生命周期结束,ws关闭
     * 需要关闭所有的
     */
    public void onWsClose(){
        if(destroy.compareAndSet(false,true)){
            writer = null;
            doOnWsClose();
        }
    }

    public abstract void doOnWsClose();

}
