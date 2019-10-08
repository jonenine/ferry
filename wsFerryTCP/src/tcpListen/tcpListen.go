package tcpListen

import (
	"fmt"
	"net"
	"strconv"

	"../utils"
	"container/list"
	"errors"
	"sync"
)

/*
 监听端口和TcpListen之间的映射关系,方便获取已经存在的监听
 */
var listenMap = make(map[int]*TcpListen)
var listenMapLock = &sync.Mutex{}

type TcpListen struct {
	//监听端口
	listenPort   int
	tcpListener  *net.TCPListener

	//消费方法,注意Message本身就是接口指针,默认的实现是直接通过ws发送给ferry客户端,这个方法必须是同步的或者是可以保证顺序的
	consumer     func(msg Message)

	//维护connection id和connectionHandler的关系
	connectMap   map[uint32]*connectionHandler
	//对map的读写锁
	mapLock      *sync.RWMutex

	//这个tcpListen下面所有connectionHandler的链表,关闭监听的时候使用这个链表来关闭所有已经打开的连接
	connHandlers *list.List
}

func (tl *TcpListen) GetHandlerList() *list.List{
	return tl.connHandlers
}

/**
 得到监听端口对应的TcpListen
 */
func GetOrCreateTCPListen(port int, consumer func(msg Message)) (*TcpListen, error) {
	listenMapLock.Lock()
	defer listenMapLock.Unlock()

	tl := listenMap[port]
	if tl == nil {
		tl = &TcpListen{
			listenPort:   port,
			consumer:     consumer,
			connectMap:   make(map[uint32]*connectionHandler),
			mapLock:      &sync.RWMutex{},
			connHandlers: list.New(),
		}

		err := tl.startListen(port)
		if err != nil {
			return nil, err
		}
		listenMap[port] = tl
	}

	return tl, nil
}


/*
 * 通过连接Id,获得对应的connection,然后发送数据
 */
func (tl *TcpListen) Write(connId uint32, bs []byte) error {
	//得到这个connection
	tl.mapLock.RLock()
	connHandler := tl.connectMap[connId]
	tl.mapLock.RUnlock()

	if connHandler != nil {
		err := connHandler.write(bs)
		if err != nil {
			return errors.New("端口" + strconv.Itoa(tl.listenPort) + "上" + err.Error())
		}
	}

	return nil
}

/**
 异步的关闭一个connection
 */
func (tl *TcpListen) CloseConn(connId uint32) {
	tl.mapLock.RLock()
	defer tl.mapLock.RUnlock()
	connHandler := tl.connectMap[connId]
	if connHandler != nil {
		connHandler.closeFromOuter(tl.listenPort)
	}

}

/**
 结束accept循环后,再触发后面关闭所有连接的逻辑
 */
func (tl *TcpListen) BreakAcceptAndCloseConnections() error{
	return utils.Try(func() error{
		//监听关闭
		err:=tl.tcpListener.Close()
		if err == nil{
			//从listenMap中删掉
			utils.Sync(listenMapLock, func() {
				delete(listenMap,tl.listenPort)
			})
		}

		return err
	})
}


/*
 * 程序启动的时候,开始监听端口
 */
func (tl *TcpListen) startListen(port int) error {
	listener, err := net.Listen("tcp4", ":"+strconv.Itoa(port))
	if err != nil {
		fmt.Printf("listen error:%s", err.Error())
		return err
	}
	var tcpListener = listener.(*net.TCPListener)
	tl.tcpListener = tcpListener

	fmt.Printf("成功打开监听端口:%d\n",port)

	var connHandlersLock = &sync.Mutex{}

	var closeChan = make(chan *connectionHandler, 10000)
	/*
	close采用单独协程的原因是,close属于Io操作,可能会耗时较长,或引发异常,而且close并不频繁
	所以同worker分开.如果close非常频繁(属于业务的一部分),可能需要在worker中进行处理
	 */
	go func() {
		for connHandler:= range closeChan{
			//关闭连接
			connHandler.doCloseConn()

			//从list中删除
			utils.Sync(connHandlersLock, func() {
				tl.connHandlers.Remove(connHandler.element)
			})

			//从map中删掉
			tl.mapLock.Lock()
			delete(tl.connectMap, connHandler.connectId)
			tl.mapLock.Unlock()


			//连接断线事件
			tl.consumer(&ConnectInactiveMessage{
				ConnectId: connHandler.connectId,
			})
		}
	}()

	//accept goroutine
	go func() {
		//接受连接
		var seq = uint64(0)

		acceptFor:
		for {
			conn, err := tcpListener.AcceptTCP()
			if err != nil {
				fmt.Printf("accept error:%s\n", err)
				break acceptFor;
			} else {
				conn.SetKeepAlive(true)
				conn.SetNoDelay(true)
				//增加读缓冲区可以减少"循环"次数,由8192改成2000,貌似提高了性能,貌似2000是个经验值,大了,小了,或不写性能都不行
				conn.SetReadBuffer(2000)
				//增加读缓冲区,可以减少write时阻塞的时间
				conn.SetWriteBuffer(2000)

				seq++;
				var connectionId = uint32(seq % 2147483647)

				//创建handler
				var connHandler = newConnectionHandler(conn, connectionId, closeChan);
				utils.Sync(connHandlersLock, func() {
					//connHandler本身就是指针,不要再取地址了
					connHandler.element = tl.connHandlers.PushBack(connHandler)
				})
				/*
			    连接上线事件
			    在下面这个go的前面,否则会出现data消息先于connectActive消息
				 */
				tl.consumer(&ConnectActiveMessage{
					ConnectId: connectionId,
				})

				//开始阻塞接收数据
				go func() {
					receiverFor:
					for {
						if connHandler.receiveRoutine(tl.consumer)!=nil{
							break receiverFor
						}
					}
				}()

				//添加到map
				tl.mapLock.Lock()
				tl.connectMap[connectionId] = connHandler
				tl.mapLock.Unlock()
			}
		} //~for

		fmt.Printf("端口"+strconv.Itoa(port)+"退出监听!\n")

		/**
		 退出accept循环后进行清理工作
		 */
		err = utils.Try(func() error{
			//关闭所有connection
			closeAllConnection:
			for {
				connHandlersLock.Lock()
				e := tl.connHandlers.Front()
				if e != nil {
					tl.connHandlers.Remove(e)
					connHandlersLock.Unlock()
				} else {
					connHandlersLock.Unlock()
					break closeAllConnection
				}

				if e != nil {
					var ch = e.Value.(*connectionHandler)
					closeChan <- ch
				}
			}

			//关闭close channel
			close(closeChan)

			return nil
		})//~try

		if err!=nil{
			fmt.Printf("清理端口"+strconv.Itoa(port)+"上已经打开的连接失败!\n")
		}

	}()

	return nil
}

