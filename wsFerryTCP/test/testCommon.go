package main

import "fmt"
import "../src/utils"

func main() {
	testTry()

	fmt.Printf("%s\n","后面的工作继续进行中...")
}


func testTry(){
	err:=utils.Try(func() {
		method()
	})
	if err!=nil {
		fmt.Printf("%s\n",err.Error())
	}

}

func method(){
	fmt.Printf("%s\n","before panic")
	panic("12345")
	fmt.Printf("%s\n","after panic")
}