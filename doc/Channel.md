# Channel 
Channel 是 Netty 处理网络 IO 请求的核心接口，基于事件模型。
Channel 注册到 EventLoop，监听IO请求，产生事件，在 pipeline 中传播，并由 ChannelHandler 负责拦截和处理事件


- NioServerSocketChannel
- NioSocketChannel