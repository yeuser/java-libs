package org.nise.ux.asl.run;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import org.nise.ux.asl.lib.DataStream;
import org.nise.ux.lib.Living;

class ListenerNode extends Living {
  private final QueueFace<DataStream> outQueue;
  private ServiceServerImpl           serviceServerImpl;
  private int                         port;
  private ServerSocket                sock;
  private boolean                     initialized = false;
  private boolean                     not_ended   = true;

  public ListenerNode(int port, int id, ServiceServerImpl serviceServerImpl, QueueFace<DataStream> outQueue) {
    super("ListenerNode #" + id);
    this.port = port;
    this.outQueue = outQueue;
    this.serviceServerImpl = serviceServerImpl;
    initialize();
  }

  public void initialize() {
    initialized = true;
  }

  @Override
  protected void init() {
    while (!initialized) {
      Thread.yield();
    }
    super.init();
  }

  @Override
  protected void runtimeBehavior() throws Throwable {
    try {
      sock = new ServerSocket(port); // port to listen
      while (not_ended) {
        if (serviceServerImpl.getInSystem() > (serviceServerImpl.getMax_clients())) {
          Thread.yield();
        } else {
          // get next client here
          Socket socket = sock.accept();
          DataStream dataStream = new DataStream(socket);
          while (!outQueue.push(dataStream)) {
            // Waiting System in Queue couldn't handle something, we handle it here.
            // Possibility of this kind of error is one in million...
          }
        }
      }
    } catch (BindException be) {
      port++;// run the system on next port!
    }
  }

  public void exit() throws IOException {
    not_ended = false;
    sock.close();
  }
}
