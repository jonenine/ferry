package tcpListen

import (
	"../utils"
	"fmt"
	"golang.org/x/net/websocket"
	"net/http"
	"os"
	"strconv"
	"sync"
)

func initHandler(w http.ResponseWriter, r *http.Request) {
	s := websocket.Server{
		Handler: websocket.Handler(handlerWSBinary),

		//解决跨域的问题,要不还得使用第三方库
		Handshake: func(config *websocket.Config, request *http.Request) error {
			return nil
		},
	}

	s.ServeHTTP(w, r);
}

/**
 全局唯一,可用于阻塞main routine
 */
func StartWsServer(port int) {
	//url长一些,省的被黑
	http.HandleFunc("/ws/ferry/tcp", initHandler)

	//下面这个方法是阻塞的
	if err := http.ListenAndServe(":"+strconv.Itoa(port), nil); err != nil {
		fmt.Printf("webSocket服务端创建失败:%s,进程退出\n", err.Error())
		//ws服务端打开失败就退出进程
		os.Exit(1)
	}
}

func send(conn *websocket.Conn, bs []byte) error {
	err := websocket.Message.Send(conn, bs)
	if err != nil {
		fmt.Printf("发送二进制错误:%s\n", err)
		//重复关闭会抛出panic
		utils.Try(func() error {
			/*
			ws客户端断掉引起tcp监听关闭及关闭所有现存connection
			现存connection关闭反过来会生成inActive消息,反过来又会尝试通过这里的ws connection发送出去
			而这里的ws连接已断,会抛出一个错误
			*/
			return conn.Close()
		})
		return err
	}

	return nil
}

/*
 tcp监听端口和ws客户端地址是1对1的关系,这里维持这种关系
 */
var tcpListenClientMap = make(map[int]string)
var tcpListenClientMapLock = &sync.Mutex{}

/**
 模仿java的concurrentHashMap的compute方法,整个操作是一个原子方法
 返回值1,表示是否完成操作,填充了map中key,如果未完成,可能(1)map中已经有哼着歌key (2)操作出错
 返回值2,表示操作异常
 */
func computeIfTcpListenClientAbsent(tcpListenPort int, compute func() (string, error)) (bool, error) {
	tcpListenClientMapLock.Lock()
	defer tcpListenClientMapLock.Unlock()
	if tcpListenClientMap[tcpListenPort] == "" {
		clientAddr, err := compute();
		if err == nil {
			tcpListenClientMap[tcpListenPort] = clientAddr
			return true, nil
		} else {
			return false, err
		}
	}

	return false, nil
}

/**
 关闭ws客户端上来的连接
 */
func closeWSConnection(tcpListenPort int, wsConn *websocket.Conn) {
	utils.TrySync(tcpListenClientMapLock, func() error {
		wsConn.Close()

		if tcpListenPort > 0 {
			var clientAddr = tcpListenClientMap[tcpListenPort]
			if clientAddr == wsConn.Request().RemoteAddr {
				delete(tcpListenClientMap, tcpListenPort)
			}
		}
		return nil
	})

}

/*
 标准库的connection的upstream处理模式就是一个循环
 */
func handlerWSBinary(conn *websocket.Conn) {
	//1.ws客户端的连接上来
	var listenPort = -1
	var tl *TcpListen

	var clientAddr = conn.Request().RemoteAddr

flag0:
	for {
		var upStreamData []byte
		if err := websocket.Message.Receive(conn, &upStreamData); err != nil {
			println(clientAddr + "对应的webSocket服务端接收二进制错误:" + err.Error())
			closeWSConnection(listenPort, conn)
			break flag0
		} else {
			//2.ws客户端的数据上来
			var message, err = Unmarshal(upStreamData)
			if err != nil {
				//反序列化失败
				utils.PrintErrorStack(err)
				closeWSConnection(listenPort, conn)
				//不给其他不友好的客户端连接的机会
				break flag0
			} else {
				/*
				 A.登录消息
				 B.ferry target端的连接断掉消息
				 C.从ferry target到ferry source端的数据传输消息
				 */
				switch message.(type) {
				/*
				 A.登录消息
				 */
				case *LoginMessage:
					var errorMessage string

					var lm = message.(*LoginMessage)
					if lm.UserName == "Admin" && lm.Password == "密码牢不可破" { //暂时采取固定的用户名
						if tl == nil {
							listenPort = lm.ListenPort
							//如果登录成功,建立tcpListen
							var isPut, _err = computeIfTcpListenClientAbsent(listenPort, func() (string, error) {
								/*
								 创建这个端口tcpListen,并向这个tcpListen注册消费tcp事件消息的方法
								 */
								tl, err = GetOrCreateTCPListen(listenPort, func(msg Message) {
									bs := msg.Marshal()
									send(conn, bs)
								})
								//如果tcpListen创建成功,返回成功
								if err == nil {
									//发送登录成功的效应消息
									send(conn, (&LoginResponseMessage{}).Marshal())

									fmt.Printf("来自" + clientAddr + "的客户端登录成功!并成功打开监听端口" + strconv.Itoa(listenPort)+"\n")

									return conn.Request().RemoteAddr, nil
								} else {
									return "", err
								}
							})

							if _err != nil {
								errorMessage = _err.Error()
							} else {
								if isPut {
									continue flag0
								} else {
									errorMessage = "已经有其他客户端创建了连接,请等待"
								}
							}
						} else {
							//忽略同一个客户端重复的登录
							continue flag0
						}
					} else {
						errorMessage = "用户名密码错误"
					}

					println("来自" + clientAddr + "的客户端登录失败!原因为," + errorMessage)

					var lrm = LoginResponseMessage{
						errorMessage,
					}

					//发送失败的登录响应消息
					send(conn, lrm.Marshal())

					//登录遇到失败就关闭这个ws连接
					closeWSConnection(listenPort, conn);
					break flag0

				/*
				B.ferry端的某个TCP连接断了,对应连接id的tcpListen端的TCP连接也断
				*/
				case *ConnectInactiveMessage:
					var clm = message.(*ConnectInactiveMessage)
					if tl != nil {
						tl.CloseConn(clm.ConnectId)
						continue flag0
					}

				/*
				C.数据传输的消息
				*/
				case *DataMessage:
					var dm = message.(*DataMessage)
					if tl != nil {
						tl.Write(dm.ConnectId, dm.Data)
						continue flag0
					}

				default:
					//不应该到这里
				} //~switch
			} //~处理个类型的message
		} //~else
	} //~for

	/*
	 退出服务端消息循环
	 */
	var warnMessage = "客户端:" + clientAddr + "对应的webSocket服务端轮询退出";
	if listenPort > 0 {
		warnMessage = "TCP监听端口:" + strconv.Itoa(listenPort) + "对应的,而且是" + warnMessage
	}
	println(warnMessage);

	/*
	 ws断掉,其对应的tcp listen也同样断掉
	 */
	if tl != nil {
		tl.BreakAcceptAndCloseConnections()
	}
}
