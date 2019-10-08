package main

import "time"

func main() {
	var ch = make(chan int,100)

	go func(){
		for i:=0;i<10;i++{
			ch<-i
		}
		close(ch)
	}()

	//关闭之后 for自动退出
	for ii := range ch{
		println(ii)
	}

	println("退出循环")

	time.Sleep(10*time.Second)
}
