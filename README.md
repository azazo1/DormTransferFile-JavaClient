# DormTransferFile

单片机作连接服务器, 用于向客户端提供其他客户端IP地址以让客户端间在局域网下建立点对点连接

连接服务器向客户端发送可用的连接号码, 客户端向连接服务器查询连接号码对应的IP地址, 建立客户端和文件传输者服务器的连接

文件发送者作为传输服务器, 文件接受者作为传输客户端建立连接

## 协议内容:

- 传输使用 UTF-8 编码
- 链接建立后进行消息通信

## 消息组成:

- 长度为5字节的消息序号码(此码各客户端独立, 由客户端自主创建, 服务器发送答复时也回复此码)
- 长度为2字节的消息类型码
- 消息内容，消息内容由消息自身定义
- 注意: 消息中的数字内容有固定长度的一定要有前导零