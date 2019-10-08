package mx.wsFerryTCP;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
@ImportResource(locations = { "classpath:/mx/wsFerryTCP/spring.xml" })
public class App implements ApplicationContextAware,InitializingBean {


    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 实例的唯一id
     */
    public static final String globalID = generateUUID();;

    public static String generateUUID(){
        return  UUID.randomUUID().toString();
    }

    /**
     * 主机名称
     */
    public static final String hostName  = getHostName();

    /**
     * 进程id,注意这时一个字符串
     */
    public static final String processId = getProcessId();

    /**
     * 如果配置了多个网卡,这个值只是其中之一
     * -Djava.net.preferIPv4Stack=true
     */
    public static final String localIp = getLocalIP();

    /**
     * 只支持linux和windows这两个平台
     */
    public static final boolean isLinux = !isWindows();


    private static String getProcessId(){
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int i = runtimeName.indexOf("@");
        return runtimeName.substring(0, i);
    }

    private static String getHostName(){
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int i = runtimeName.indexOf("@");
        return runtimeName.substring(i+1);
    }

    private static String getLocalIP(){
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
            return addr.getHostAddress().toString();
        } catch (UnknownHostException e) {
        }

        return null;
    }


    private static boolean isWindows() {
        return System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1;
    }

    /**
     * cpu核心数
     */
    public static final int numOfCores = Runtime.getRuntime().availableProcessors();


    private static volatile OkHttpClient client;

    /**
     * 用的时候再分配资源,里面有缓冲和线程池,估计消耗也少不了
     */
    public static OkHttpClient getHTTPClient(){
        if(client == null){
            synchronized (App.class){
                if(client == null){
                    client = new OkHttpClient.Builder()
                            //有的服务就是很慢,像是访问数据库的服务,一般30秒到2分分钟之内都是正常的
                            //okhttp的默认值是10秒
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build();

                    client.dispatcher().setMaxRequestsPerHost(8000);
                    //client.dispatcher().setMaxRequests(20000);
                }
            }
        }

        return client;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    private static Class applicationClazz;


    protected static void startUp(String[] args){
        try {
            //得到启动类,找到第一个BaseApplication的子类为止
            Thread mainThread = Thread.currentThread();
            StackTraceElement[] stackTrace = mainThread.getStackTrace();

            for(int i=stackTrace.length - 1;i>=0;i--){
                String mainClassName = stackTrace[i].getClassName();
                applicationClazz = mainThread.getContextClassLoader().loadClass(mainClassName);
                if(App.class.isAssignableFrom(applicationClazz)){
                    break;
                }else{
                    applicationClazz = null;
                }
            }

            System.out.println("启动类:"+ applicationClazz);

            //找不到启动类,下面报错
            SpringApplication app = new SpringApplication(applicationClazz);
            app.setBannerMode(Banner.Mode.OFF);
            app.run(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("进程退出");
            System.exit(1);
        }
    }


    protected static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext _applicationContext) throws BeansException {
        applicationContext = _applicationContext;
    }

    public static void main(String[] args) {
        startUp(args);
    }
}
