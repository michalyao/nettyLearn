package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandle implements Runnable {

  private String host;
  private int port;
  private Selector selector;
  private SocketChannel socketChannel;
  private volatile boolean stop;

  // init
  public TimeClientHandle(String host, int port) {
    this.host = host == null ? "127.0.0.1" : host;
    this.port = port;
    try {
      selector = Selector.open();
      socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }



  @Override
  public void run() {
    try {
      // 发送连接请求
      doConnect();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    while (!stop) {
      try {
        selector.select(1000);
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        Iterator<SelectionKey> it = selectionKeys.iterator();
        SelectionKey key = null;
        while (it.hasNext()) {
          key = it.next();
          it.remove();
          try {
            handleInput(key);
          } catch (Exception e) {
            if (key != null) {
              key.cancel();
              if (key.channel() != null) {
                key.channel().close();
              }
            }
          }
        }
      } catch (IOException e) {
        e.getMessage();
      }
    }
  }

  // 直接连接成功
  private void doConnect() throws IOException{
    if (socketChannel.connect(new InetSocketAddress(host, port))) {
      socketChannel.register(selector, SelectionKey.OP_READ);
      doWrite(socketChannel);
    } else {
//      注册到多路复用器上，等待握手消息进行重连
      socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }
  }

  private void handleInput(SelectionKey key) throws IOException{
    if (key.isValid()) {
      SocketChannel sc = (SocketChannel) key.channel();
      if (key.isConnectable()) {
        if (sc.finishConnect()) {
          sc.register(selector, SelectionKey.OP_READ);
          doWrite(sc);
        } else {
          System.exit(1);
        }
      }
      if (key.isReadable()) {
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int readBytes = sc.read(readBuffer);
        if (readBytes > 0) {
          byte[] bytes = new byte[readBuffer.remaining()];
          readBuffer.flip();
          readBuffer.get(bytes);
          String body = new String(bytes, "UTF-8");
          System.out.println("Now is :" + body);
          this.stop = true;
        } else if (readBytes < 0) {
          key.cancel();
          sc.close();
        } else ;
      }
    }
  }

  private void doWrite(SocketChannel sc) throws IOException{
    byte[] req = "QUERY TIME ORDER".getBytes();
    ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
    writeBuffer.put(req);
    writeBuffer.flip();
    sc.write(writeBuffer);
    if (!writeBuffer.hasRemaining()) {
      System.out.println("send order succeeded");
    }
  }
}
