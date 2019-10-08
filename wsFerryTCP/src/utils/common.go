package utils

import (
	"fmt"
	"github.com/pkg/errors"
	"runtime/debug"
	"sync"
)

/**
 打印错误及线程栈
 */
func PrintErrorStackWithPrefix(prefix string,err error){
	fmt.Printf(prefix+":%s\n", err.Error())
	debug.PrintStack()
}

/**
 打印错误及线程栈
 */
func PrintErrorStack(err error){
	PrintErrorStackWithPrefix("出现了错误:",err)
}

/**
 在某个"锁"上,同步的做一件事情
 */
func Sync(lock *sync.Mutex ,do func()){
	lock.Lock()
	defer lock.Unlock()
	do()
}

func SyncRead(lock *sync.RWMutex ,do func()){
	lock.RLock()
	defer lock.RUnlock()
	do()
}

func SyncWrite(lock *sync.RWMutex ,do func()){
	lock.Lock()
	defer lock.Unlock()
	do()
}

func tryWithError(pErr *interface{},do func() error) error{
	defer func(){
		err := recover()
		if err!=nil {
			*pErr = err
		}
	}()
	return do()
}

/**
 捕获panic,并以error的形式返回
 */
func Try(do func() error) error{
	var panicErr interface{}
	var err = tryWithError(&panicErr,do)

	if panicErr !=nil{
		//应该做的更细致些,把Try方法之前的错误栈信息去掉
		debug.PrintStack()
		switch panicErr.(type){
		case error:
			println(panicErr.(error).Error())
			return panicErr.(error)
		case string:
			println(panicErr.(string))
			return errors.New(panicErr.(string));
		default:
			println("出现了panic")
			return errors.New("出现了panic");
		}
	}else if err!=nil{
		debug.PrintStack()
		println(err.Error())
		return err
	}

	return nil
}

/**
 在锁参与的情况下,进行同步的情况
 */
func TrySync(lock *sync.Mutex ,do func() error) error{
	lock.Lock()
	err:=Try(do)
	lock.Unlock()
	return err
}

/**
 相对于读写锁的读的情况
 */
func TrySyncRead(lock *sync.RWMutex ,do func() error) error{
	lock.RLock()
	err:=Try(do)
	lock.RUnlock()
	return err
}

/**
 相对于读写锁的读的情况
 */
func TrySyncWrite(lock *sync.RWMutex ,do func() error) error{
	lock.Lock()
	err:=Try(do)
	lock.Unlock()
	return err
}







