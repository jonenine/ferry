package tcpListen

import (
	"container/list"
	"errors"
	"fmt"
	"net"
	"strconv"
	"sync/atomic"

	"runtime/debug"
)

/*
1.非什么在上行处理时使用worker线程
  如果不使用worker线程处理upstream消息,在压力大的时候,也就是所有连接
都有数据上来的时候会造成过多goroutine同时是活动的,会卡死进程,触发golang内部线程过多的bug
upstream goroutine在接收到数据时自己不处理,交给worker处理,实际是一种异步的行为.
因为upstream goroutine是阻塞的,所以其活动的时间很短,将耗时的计算工作交给worker完成
worker数量是有限的,当等于系统cpu核心数时,同样会压满cpu,不存在性能问题
而且,这种方式压力越大,越多的连接里有数据上来,就有越多的connectionHandler在worker中进行处理
在worker中就意味着不在upstream goroutine中,可以压制upstream goroutine数量过多的问题

下面这个程序没有这样做

2.upstream处理和write和close处理分开
  通常upstream是在一个循环中,都是内部调用
  而write和close通常都是从外部进行调用
  这两个通常都不在同一个线程或协程,但彼此访问的是不同的状态,应该互不干扰,transport层并不存在同步的问题(有同步问题也是处理业务的状态层)
唯有close时,需要在connectionHandler中记录一个"原子的"状态,当状态改变时(表示已经close了)
upstream goroutine退出,write操作无效
  这一点在netty写的程序中同样有所体现
*/

/*
  管理连接
 */
type connectionHandler struct {
	//connection id,每个监听端口的connection id是独立的
	connectId  uint32

	//connection的关闭也是异步的,在一个独立的routine中进行
	closeChan  chan *connectionHandler

	//当前连接
	conn       *net.TCPConn

	err        error

	//0表示没有关闭,1表示关闭,主要给write使用
	isClosing  int32

	element    *list.Element
}


func newConnectionHandler(conn *net.TCPConn, connectId uint32,closeChan chan *connectionHandler) *connectionHandler {
	return &connectionHandler{
		conn:      conn,
		connectId: connectId,
		closeChan: closeChan,
		isClosing: 0,
	}
}

/**
 关闭connection,内部调用,不要使用
 */
func (connHandler *connectionHandler) doCloseConn() {
	connHandler.closeChan = nil
	//关闭socket
	defer connHandler.doRecover(false)
	//在closer goroutine中执行,不用担心同步问题
	if connHandler.conn!=nil{
		connHandler.conn.Close()
		connHandler.conn = nil
	}
}

/*
同步发送数据
 */
func (connHandler *connectionHandler) write(bs []byte) error {
	if atomic.LoadInt32(&connHandler.isClosing) != 0 {
		return errors.New("connectionId:" + strconv.Itoa(int(connHandler.connectId)) + "连接已经关闭")
	}
	//直接发送
	defer connHandler.doRecover(true)
	_, err := connHandler.conn.Write(bs);
	if err != nil {
		return errors.New("connectionId:" + strconv.Itoa(int(connHandler.connectId)) + "连接已经关闭")
	}

	return nil
}


/**
 阻塞读取的go routine
 */
func (connHandler *connectionHandler) receiveRoutine(consumer func(msg Message)) error{
	if atomic.LoadInt32(&connHandler.isClosing) != 0 {
		return errors.New(strconv.Itoa(int(connHandler.connectId))+"连接已经关闭!")
	}
	//一旦读取出现异常,就关闭socket
	defer connHandler.doRecover(true)

	//阻塞读取
	var data = make([]byte,2000)
	var n, err = connHandler.conn.Read(data)

	if n>0 {
		/**
		数据传输事件
		*/
		consumer(&DataMessage{
			ConnectId: connHandler.connectId,
			Data:      data[0:n],
		})
	}else if err != nil{//没读到数据而且出现了异常,就关闭连接
		//设置已关闭
		atomic.StoreInt32(&connHandler.isClosing, 1)
		connHandler.err = err
		//送入close routine处理
		connHandler.closeChan <- connHandler
		return err
	}

	return nil
}


func (connHandler *connectionHandler) doRecover(isClose bool) {
	if err := recover(); err != nil {
		fmt.Println("截获到panic:", err)
		debug.PrintStack()

		if isClose {
			//设置已关闭
			atomic.StoreInt32(&connHandler.isClosing, 1)
			connHandler.closeChan <- connHandler
		}
	}
}

/**
 从外部调用,关闭connection,这是一个异步操作
 */
func (connHandler *connectionHandler) closeFromOuter(listenPort int) {
	//设置已关闭
	atomic.StoreInt32(&connHandler.isClosing, 1)
	connHandler.err = errors.New("监听" + strconv.Itoa(listenPort) + "的连接" + strconv.Itoa(int(connHandler.connectId)) + "将从外部强制关闭连接")
	//送入close routine处理
	connHandler.closeChan <- connHandler
}

