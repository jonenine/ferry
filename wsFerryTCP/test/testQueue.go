package main

import (
	"../src/utils"
	"time"
	"os"
)

func test0(){
	q:= utils.NewQueue()
	q.Offer(1)
	q.Offer(2)
	q.Offer(3)

	//这个要自己转换
	println(q.Poll().(int))
	println(q.Poll().(int))
	println(q.Poll().(int))
	//nil不能强转,需要提前判断是否为nil
	println(q.Poll()==nil)
}

/**
  验证协程安全
 */
func test1(){
	q:= utils.NewQueue()
	go func(){
		var lastV  = -1
		for{
			v:=q.Poll()
			if v!=nil{
				var v int = v.(int)
				if(v - lastV != 1){
					os.Exit(1)
				}else{
					println(v)
				}
				lastV = v
			}else{
				time.Sleep(1*time.Millisecond)
			}
		}
	}()

	for i:=0;i<10000;i++{
		q.Offer(i)
	}

	time.Sleep(10*time.Second)
}

func main() {
	test1()
}