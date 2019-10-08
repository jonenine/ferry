package tcpListen

import (
	"bytes"
	"encoding/binary"
	"errors"
	"strconv"
	"strings"
)


type Message interface {
	Marshal() []byte
}

/*
 * 由ferry发起的登录消息
 */
type LoginMessage struct {
	ListenPort int
	UserName   string
	Password   string
}

func (message *LoginMessage) Marshal() []byte {
	//没有这种需要
	return nil;
}

/**
 * tcp listen accept连接的消息
 */
type ConnectActiveMessage struct {
	//连接id
	ConnectId uint32
}

func (message *ConnectActiveMessage) Marshal() []byte {
	buf := bytes.NewBuffer([]byte{})
	//以大端序写入buf
	binary.Write(buf, binary.BigEndian, &message.ConnectId)

	return append([]byte{connectActiveMessageType}, buf.Bytes()...)
}

/*
 * 两边传递给对方的连接断掉消息
 */
type ConnectInactiveMessage struct {
	//连接id
	ConnectId uint32
}

func (message *ConnectInactiveMessage) Marshal() []byte {
	buf := bytes.NewBuffer([]byte{})
	//以大端序写入buf
	binary.Write(buf, binary.BigEndian, &message.ConnectId)

	return append([]byte{connectInactiveMessageType}, buf.Bytes()...)
}

/*
 * 登录响应消息
 */
type LoginResponseMessage struct {
	//无异常表示登录成功,否则会返回失败原因
	Error string
}

func (message *LoginResponseMessage) Marshal() []byte {
	bs := []byte{loginResponseMessageType}

	if message.Error!=""{
		//将error转成byte数组,再写入
		var buf = bytes.NewBuffer([]byte{})
		//字符串直接转byte数组,就是utf-8编码的
		strBs := []byte(message.Error)
		binary.Write(buf, binary.BigEndian, &strBs)
		bs = append(bs, buf.Bytes()...)
	}


	return bs
}

/*
 * 数据消息
 */
type DataMessage struct {
	//连接id
	ConnectId uint32
	Data      []byte
}

func (message *DataMessage) Marshal() []byte {
	bs := []byte{dataMessageType}

	//以大端序写入ConnectId
	var buf = bytes.NewBuffer([]byte{})
	binary.Write(buf, binary.BigEndian, &message.ConnectId)
	bs = append(bs, buf.Bytes()...)

	//写入data
	bs = append(bs,message.Data...)

	return bs
}

const (
	loginMessageType           = 0
	connectActiveMessageType   = 31
	connectInactiveMessageType = 61
	loginResponseMessageType   = 01
	dataMessageType            = 30
)

/**
  返回的都是message的子类型
 */
func Unmarshal(bs []byte) (interface{}, error) {
	typeFlag := int(bs[0])
	switch typeFlag {
	case loginMessageType:
		//读监听端口
		buf := bytes.NewBuffer(bs[1:5])
		//binary.Read方法源码中是忽略int类型的
		var listenPort uint32
		binary.Read(buf, binary.BigEndian, &listenPort)
		//读应户名密码
		str := string(bs[5:])
		uap := strings.Split(str, "||")
		return &LoginMessage{ListenPort:int(listenPort),UserName: uap[0], Password: uap[1]}, nil

	case connectInactiveMessageType:
		buf := bytes.NewBuffer(bs[1:])
		var connectId uint32
		binary.Read(buf, binary.BigEndian, &connectId)
		return &ConnectInactiveMessage{connectId}, nil

	case loginResponseMessageType:
		//没有这种需求
		return nil, nil
	case dataMessageType:
		var buf = bytes.NewBuffer(bs[1:])
		var connectId uint32
		binary.Read(buf, binary.BigEndian, &connectId)

		return &DataMessage{connectId, bs[5:]}, nil

	default:
		return nil, errors.New("不支持的类型" + strconv.Itoa(typeFlag));
	}

}
