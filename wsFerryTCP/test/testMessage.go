package main

import (
	"../src/tcpListen"
	"../src/utils/byteUtils"
	"fmt"
)

func main() {
	//测试marshal
	lm := tcpListen.LoginMessage{27001,"cjadmin", "2Bqnynt!"};
	var bs = lm.Marshal();

	fmt.Printf("断连接消息")
	ci := tcpListen.ConnectInactiveMessage{9876543};
	bs = ci.Marshal();
	fmt.Printf("----%s\n", byteUtils.SliceToHex(bs));

	fmt.Printf("登录响应消息")
	lr := tcpListen.LoginResponseMessage{ "已经有另外的账户登录了"};
	bs = lr.Marshal();
	fmt.Printf("----%s\n", byteUtils.SliceToHex(bs));

	fmt.Printf("数据传输消息")
	dm:=tcpListen.DataMessage{12345,[]byte{0x11,0x22,0x33,0xff,0x80,0x7f,0x99}};
	bs = dm.Marshal();
	fmt.Printf("----%s\n", byteUtils.SliceToHex(bs));

	//验证java端的结果
	testUnmarshal("00 00 00 4A 92 61 64 6D 69 6E 7C 7C 4F 6E 65 70 6C 75 73 31 3D 74 77 6F ")
}

func testUnmarshal(hex string){
	var bs1,err =byteUtils.HexToSlice(hex);
	if err == nil{
		var message,err = tcpListen.Unmarshal(bs1);
		if err == nil{
			fmt.Printf("反序列化结果%+v\n",message)
		}else{
			println(err)
		}
	}else{
		println(err)
	}
}
