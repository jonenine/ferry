package mx.wsFerryTCP;

import mx.utils.ByteUtils;
import mx.wsFerryTCP.ws.HammalBase;
import mx.wsFerryTCP.ws.WsClient;

import java.util.function.BiFunction;

public class TestWsClient {

    static class Hammal extends HammalBase {

        @Override
        public void onListenConnActive(int connectId) {
            System.err.println("   tcp listen端的连接:"+connectId+"上线了");
            try {
                sendDataToListen(connectId,"欢迎,欢迎,热烈欢迎".getBytes("GBK"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onListenConnInactive(int connectId) {
            System.err.println("###tcp listen端的连接:"+connectId+"掉线了线了");
        }

        @Override
        public void onReceiveDataFromListenConn(int connectId, byte[] bs) {
            System.err.println("从tcp listen端的连接:"+connectId+"接收到数据:"+ ByteUtils.toHexString(bs));
            //把原数据返回，形成echo
            sendDataToListen(connectId,bs);
        }

        @Override
        public void doOnWsClose() {
            System.err.println("从webSocket端断掉连接");
        }
    }


    public static void main(String[] args) {
        try {
            WsClient wsClient = new WsClient("127.0.0.1:38080",38888,new Hammal());
            wsClient.connect();
            //
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
