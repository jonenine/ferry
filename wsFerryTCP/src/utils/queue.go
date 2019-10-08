/**
 建立一个简单的,协程安全的queue
 */
package utils

import (
	"container/list"
	"errors"
	"sync"
)

type Queue struct {
	list *list.List
	lock *sync.Mutex
}

func NewQueue() *Queue {
	return &Queue{
		list:list.New(),
		lock:&sync.Mutex{},
	}
}

var nilError = errors.New("不能向队列中offer空值");


/*
   向队列尾部插入值,值不能为nil
 */
func (q *Queue) Offer(value interface{}) error{
	if value==nil{
		return nilError
	}
	q.lock.Lock()
	defer q.lock.Unlock()
	//从后端插入
	q.list.PushBack(value)

	return nil
}

/*
  从队列头部取出值,如果队列为空,就返回nil
 */
func (q *Queue) Poll() interface{}{
	q.lock.Lock()
	defer q.lock.Unlock()
	ele:= q.list.Front()
	if ele!=nil{
		q.list.Remove(ele)
		return ele.Value
	}

	return nil
}

