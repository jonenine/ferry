/*
 * 标准库的websocket
 */

package main


import (
	"fmt"
	"net/http"
	"os"
	"strings"

	"golang.org/x/net/websocket"
)

/*
 * 每个连接是一个协程，连接工作完成前，协程是不能结束的
 * 文本方式处理webSocket
 */
func handlerWSText(ws *websocket.Conn) {
	var err error
flag0:
	for {
		var reply string

		println("1-----"+ws.Request().RemoteAddr)

		if err = websocket.Message.Receive(ws, &reply); err != nil {
			fmt.Printf("接收字符串错误:%s\n", err)
			ws.Close();
			break flag0
		}

		fmt.Printf("ws服务端接收到:%s\n", reply)

		/**
		  使用其他协程send,没有发现连接断掉的情况
		 */
		go func(){
			if err = websocket.Message.Send(ws, strings.ToUpper(reply)); err != nil {
				fmt.Printf("发送字符串错误:%s\n", err)
			}
			fmt.Printf("ws服务端接发送2:%s\n", reply)
		}()

		//if err = websocket.Message.Send(ws, strings.ToUpper(reply)); err != nil {
		//	fmt.Printf("发送字符串错误:%s\n", err)
		//	ws.Close();
		//	break flag0
		//}
		//fmt.Printf("ws服务端接发送:%s\n", reply)
	}
}

func handlerWSBinary(ws *websocket.Conn) {
	var err error
flag0:
	for {
		var reply []byte

		if err = websocket.Message.Receive(ws, &reply); err != nil {
			fmt.Printf("接收二进制错误:%s\n", err)
			ws.Close();
			break flag0
		}

		fmt.Printf("ws服务端接收到:%v+\n", reply)


		if err = websocket.Message.Send(ws, reply); err != nil {
			fmt.Printf("发送二进制错误:%s\n", err)
			ws.Close();
			break flag0
		}
		fmt.Printf("ws服务端接发送:%v+\n", reply)
	}
}

func initHandler(w http.ResponseWriter, r *http.Request) {
	s := websocket.Server{
		Handler: websocket.Handler(handlerWSText),

		//解决跨域的问题,要不还得使用第三方库
		Handshake: func(config *websocket.Config, request *http.Request) error {
			return nil
		},
	}

	s.ServeHTTP(w, r);
}

func main() {
	http.HandleFunc("/upper", initHandler)

	//下面这个方法是阻塞的
	if err := http.ListenAndServe(":38888", nil); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

}
