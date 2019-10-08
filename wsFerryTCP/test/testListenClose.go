package main

import (
	"net"
	"fmt"
	"os"
	"time"
	"../src/utils"
)

func main(){
	listener, err := net.Listen("tcp4", ":19999")
	var tcpListener = listener.(*net.TCPListener)
	if err != nil {
		fmt.Printf("listen error:%s", err)
		os.Exit(1)
	}

	/*
	 定时从另一个协程关闭监听
	 */
	go func(){
		time.Sleep(10*time.Second)
		utils.Try(func() {
			tcpListener.Close()
		})
	}()

	flag0:
	for{
		conn, err := tcpListener.AcceptTCP()
		if err != nil {
			//打个日志,然后忽略
			fmt.Printf("accept error:%s\n", err)
			break flag0
		}else{
			//简单的阻塞读取一帧数据
			go func(){
				for{
					var data = make([]byte,10000)
					var n, err = conn.Read(data)
					if err != nil {
						fmt.Printf("accept error:%s", err)
					}else{
						var str = string(data[0:n])
						fmt.Printf("取得客户端数据:"+str)
					}
				}
			}()

		}
	}

	fmt.Printf("%s\n","退出监听")

}

