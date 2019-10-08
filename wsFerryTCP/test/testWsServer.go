package main

import (
	"../src/tcpListen"
)

func main() {
	tcpListen.StartWsServer(38080)
}
