package mx.wsFerryTCP.ws;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 映射维护表,需要配置在配置文件中
 */
public class ListenPortToServerIpPortMap {

    Map<Integer,String> map;

    public void setMap(Map<Integer, String> map) {
        this.map = map;
    }

    /**
     * tcp listen端的监听端口,同"本地网络内"的服务器ip和端口的映射关系
     */
    public static ListenPortToServerIpPortMap defaultMap;

    public ListenPortToServerIpPortMap(){
        synchronized (ListenPortToServerIpPortMap.class){
            if(defaultMap == null){
                defaultMap = this;
            }else{
                throw new RuntimeException("ListenPortToServerIpPortMap不能多次创建");
            }
        }
    }

    public static String[] getIpPort(int listenPort){
        String ipPort = defaultMap.map.get(listenPort);
        if(ipPort!=null){
            return ipPort.split("\\:");
        }

        return null;
    }

}
