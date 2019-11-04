package mx.wsFerryTCP.ws;

import com.google.common.base.Throwables;
import mx.wsFerryTCP.tcp.NettyClientHammal;
import okhttp3.*;
import okio.ByteString;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WsClient extends WebSocketListener implements IWriter{

    /**
     * 创建摆渡通道
     * @param wsServerAddress       ws服务端地址 包括端口
     * @param remoteListenPort      在tcp listen端要监听的端口,而不是ws服务端的端口
     */
    public static void establishFerryTransport(String wsServerAddress, int remoteListenPort){
        WsClient client =  new WsClient(wsServerAddress,remoteListenPort,new NettyClientHammal());
        client.connect();
    }

    protected static final Logger logger = LogManager.getLogger(WsClient.class);

    public  final  String     wsServerAddress;
    public  final  int        listenPort;
    private final  HammalBase hammal;

    /**
     * @param wsServerIpPort    webSocket服务端ip和端口
     * @param remoteListenPort  tcp listen的端口
     * @param hammal            数据搬运工
     */
    public WsClient(String wsServerIpPort, int remoteListenPort, HammalBase hammal) {
        if(hammal==null){
            throw new RuntimeException("hammal不能为null");
        }
        this.wsServerAddress = wsServerIpPort.trim();
        this.listenPort = remoteListenPort;
        this.hammal = hammal;
        this.hammal.setWriter(this);
    }

    public void connect(){
        Request request = new Request.Builder()
                .url("ws://"+wsServerAddress+"/ws/ferry/tcp")
                .build();

        OkHttpClient client = new OkHttpClient();
        //这个连接是异步的,失败也不会报错
        client.newWebSocket(request,this);
    }

    volatile WebSocket webSocket;

    @Override
    public void write(byte[] bs){
        //这个方法本身是同步的
        webSocket.send(ByteString.of(bs));
        //what's the fuck!
        while(webSocket.queueSize()>10276683){
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 当连接打开时登录
     */
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        this.webSocket = webSocket;
        //发送登陆消息
        Message.LoginMessage loginMessage = new Message.LoginMessage(listenPort,"Admin","密码牢不可破");
        write(loginMessage.marshal());
    }


    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        try {
            byte[] bs = bytes.toByteArray();
            Message message = Message.unmarshal(bs);
            //登录响应
            if (message instanceof Message.LoginResponseMessage){
                Message.LoginResponseMessage loginResponseMessage = (Message.LoginResponseMessage) message;
                String error;
                if((error = loginResponseMessage.getError())!=null){
                    logger.error("登录失败"+error);
                    hammal.onWsClose();
                    webSocket.close(1001,"webSocket登录失败,客户端主动关闭连接");
                }else{
                    //登录成功
                    logger.info("登录成功");
                }
            }
            //tcp listen连接建立消息
            else if(message instanceof Message.ConnectActiveMessage){
                Message.ConnectActiveMessage ca = (Message.ConnectActiveMessage) message;
                hammal.onListenConnActive(ca.connectId);
            }
            //tcp listen连接断掉消息
            else if(message instanceof Message.ConnectInactiveMessage){
                Message.ConnectInactiveMessage cim = (Message.ConnectInactiveMessage) message;
                hammal.onListenConnInactive(cim.connectId);
            }
            //从tcp listen摆渡数据消息
            else if(message instanceof Message.DataMessage){
                Message.DataMessage dm = (Message.DataMessage) message;
                hammal.onReceiveDataFromListenConn(dm.connectId,dm.data);
            }else{
                //忽略
            }
        } catch (Exception e) {
            logger.error(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        hammal.onWsClose();
        webSocket.close(1001,"webSocket登录失败,客户端主动关闭连接");
    }

    /**
     * 连接和写入出现错误
     */
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.err.println("wsServerAddress:"+wsServerAddress+",listenPort:"+listenPort+
                ",连接或写入出现错误:"+Throwables.getStackTraceAsString(t));
        hammal.onWsClose();
        webSocket.close(1001,"webSocket登录失败,客户端主动关闭连接");
    }


    /**
     * 从外部手动关闭这个ws客户端
     */
    public void close(String reason){
        if(webSocket!=null){
            webSocket.close(1000,reason+"客户端主动关闭");
        }
    }

}









