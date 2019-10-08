package mx.wsFerryTCP;

import mx.utils.Json;
import mx.wsFerryTCP.ws.ListenPortToServerIpPortMap;
import mx.wsFerryTCP.ws.WsClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.util.Map;


@Configuration
@ImportResource(locations = { "classpath:/spring.xml" })
public class FerryApp extends App {

    public static void main(String[] args) {
        startUp(args);
    }

    /**
     * 不写这个，springContext中竟然不初始化，太过分了
     */
    @Autowired
    ListenPortToServerIpPortMap map;


    @Override
    public void afterPropertiesSet() throws Exception {
        //这个不能Autowired,就这么几行就发现spring两问题
        Map<String,String> projectConfig = (Map<String, String>) applicationContext.getBean("projectConfig");
        String wsServerAddress = projectConfig.get("wsServerAddress");
        //这一句是留给业务编程的,可同时连接到多个wsServerAddress去,每个wsServer可以启动多个tcpListen监听端口
        //WsClient.establishFerryTransport(wsServerAddress,2404);
        //WsClient.establishFerryTransport(wsServerAddress,18888);

        WsClient.establishFerryTransport(wsServerAddress,9999);

        //没有上面那个Autowired,下面这个会报错
        //String[] localServerIpPort = ListenPortToServerIpPortMap.getIpPort(38888);
        //System.err.println(new Json(localServerIpPort));
    }

}
