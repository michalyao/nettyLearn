# Unsafe
Unsafe 接口实际上是 Channel 接口的辅助接口，不应该被用户代码调用。
实际上的IO读写操作都是由Unsafe接口负责完成的。

unsafe 完成操作后 通知 ChannelFuture