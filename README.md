# 公网反向摆渡（代理）到内网

 golang java 代理 摆渡

---

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;业务系统一般部署在内网，内网和公网之间一般是物理隔离或是防火墙隔离的，但有的时候也需要将内网网段的服务开放到公网上，用来进行调试、确认等临时工作。<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;公网访问内网，一般要具备两个条件（1）到运营商申请固定IP （2）通过路由器，代理服务器等将内网端口映射到固定IP上。 这种方式成本较高，不适合小公司，小团队操作。<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;有的时候公网资源有限。比如：还是因为成本限制，不可能在阿里云上租用大量的服务器；或者因为licence限制，也不可能将很多非开源中间件（比如oracle）部署在阿里云上。那么就需要出现一个工具，可以方便的将内网的资源开放到公网。（注意，这样做同样不要违反中间件版权涉及的法律约束）<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;工具的实质就是将客户端请求从公网代理（反向摆渡）给内网的服务中，再将内网服务的响应返回到外网的客户端。<br/>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;…想到便要做到…<br/>

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;于是这样一款反向摆渡工具就诞生了。<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(1)	内网摆渡程序需要登录到云端代理程序，保证安全性<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(2)	云端代理程序按照内网请求动态监听端口<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(3)	一旦内网程序关闭web socket客户端，云端代理便会关闭socket监听。收放自如<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(4)	云端程序采用golang编写，性能消耗极小（测试时性能消耗在1%左右），占内存也只占用几十M。租一个阿里的低配虚拟机可以同时将很多内网服务开放到公网。<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(5)	这是个开源程序（而且是用java和go这两种低成本的语言编写），你可以改成任何你需要的样子，这才是最大的优点。<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;如图所示：<br/>
![此处输入图片的描述][1]
类图:
![此处输入图片的描述][2]
配置使用说明:
![此处输入图片的描述][3]
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;摆渡程序分两部分组成，在内网的部分做web socket客户端和tcp客户端，使用java编写。公网云端的程序做web socket服务端和tcp服务端使用go编写 。<br/>

  [1]: https://github.com/jonenine/ferry/blob/master/docs/1.jpg
  [2]: https://github.com/jonenine/ferry/blob/master/docs/2.jpg
  [3]: https://github.com/jonenine/ferry/blob/master/docs/3.jpg
