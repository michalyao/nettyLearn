package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {

  private Selector selector;
  private ServerSocketChannel serverSocketChannel;
  private volatile boolean stop;

  // init
  // 初始化多路复用器，serverSocketChannel 绑定端口并注册复用器
  public MultiplexerTimeServer(int port) {
    try {
      // open
      selector = Selector.open();
      serverSocketChannel = ServerSocketChannel.open();

      //configure
      serverSocketChannel.configureBlocking(false);

      //bind
      serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);

      //register on selector

      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    this.stop = true;
  }

  @Override
  public void run() {
    while (!stop) {
      try {
        // select round robin
        selector.select(1000);
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectionKeys.iterator();
        SelectionKey key = null;
        while (iterator.hasNext()) {
          key = iterator.next();
          iterator.remove();
          handleInput(key); // find and handle
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
//      关闭多路复用器，channel pipe资源会被自动关闭
      if (selector != null) {
        try {
          selector.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void handleInput(SelectionKey key) throws IOException {
    if (key.isValid()) {
//      处理新接入的请求消息
      if (key.isAcceptable()) {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
      } if (key.isReadable()) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int readBytes = sc.read(readBuffer);
        if (readBytes > 0) {
          // get write 前需要 flip
          readBuffer.flip();
          byte[] bytes = new byte[readBuffer.remaining()];
          readBuffer.get(bytes);
          String body = new String(bytes, "UTF-8");
          System.out.println("server read: " + body);
          String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)
              ? Instant.now().toString() : "BAD ORDER";
          doWrite(sc, currentTime);
        } else if (readBytes < 0) {
          key.cancel();
          sc.close();
        } else ;
      }
    }
  }

  private void doWrite(SocketChannel sc, String currentTime) throws IOException {
    if (currentTime != null && currentTime.trim().length() > 0) {
      byte[] bytes = currentTime.getBytes();
      ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
      writeBuffer.put(bytes);
      writeBuffer.flip();
      // write 和 get 之前需要flip 在这之前，可能进行了读或者put操作
      sc.write(writeBuffer);
    }
  }
}
