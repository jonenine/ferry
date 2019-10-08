
/**
   测试golang的list
   不支持queue接口,无法pop
 */

package main

import (
	"container/list"
	"fmt"
	"reflect"
)

func main() {
	li0:=list.New()

	for i:=1;i<100;i++{
		li0.PushBack(i)
	}

	for li0.Len()>0{
		e:= li0.Front()
		li0.Remove(e)


		//*list.Element
		fmt.Println(reflect.TypeOf(e));
		fmt.Println(reflect.TypeOf(e.Value));
		//interface类型强转
		fmt.Printf("%d\n",e.Value.(int));
	}


}






