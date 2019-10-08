package main

import (
	"../src/tcpListen"
	"fmt"
	"time"
	"reflect"
)

var tl *tcpListen.TcpListen

func showHandlerList(){
	var list = tl.GetHandlerList()

	if list!=nil {
		if e:=list.Front();e!=nil {
			forFlag:
			for{
				fmt.Printf("#######%+v\n",e.Value)
				if e = e.Next();e == nil{
					break forFlag;
				}
			}
		}
	}
	fmt.Printf("以上为handlerList内容")
}

func consume(msg tcpListen.Message){
	fmt.Printf("%s,%+v\n",reflect.TypeOf(msg),msg)
	switch msg.(type){
	case *tcpListen.ConnectInactiveMessage:
		println("连接断了")
		showHandlerList()

	case *tcpListen.DataMessage:
		var dm = msg.(*tcpListen.DataMessage)
		var l = len(dm.Data)
		var cmd = string(dm.Data[0:l-1])
		if cmd == "close"{
			//关闭连接
			tl.CloseConn(dm.ConnectId)
		}else if cmd == "destroy"{
			//关闭监听
			tl.BreakAcceptAndCloseConnections()
		}else{
			//形成echo
			tl.Write(dm.ConnectId,dm.Data)
		}

	default:
		showHandlerList()
	}
}

func main() {
	_tl,err:= tcpListen.GetOrCreateListen(38888,consume)
	if err!=nil {
		println(err)
	}else{
		tl = _tl
		fmt.Printf("程序已经启动\n")
		time.Sleep(9999999*time.Second)
	}

}