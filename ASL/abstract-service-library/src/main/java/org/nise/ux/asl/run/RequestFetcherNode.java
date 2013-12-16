package org.nise.ux.asl.run;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.nise.ux.asl.face.DataConnection;
import org.nise.ux.asl.lib.DataStream;
import org.nise.ux.lib.Living;
import org.nise.ux.lib.RoundQueue;

class RequestFetcherNode extends Living {
  private final List<RoundQueue<DataConnection>> consumersQueue = new ArrayList<RoundQueue<DataConnection>>();
  private final RoundQueue<DataStream>           inQueue;

  public RequestFetcherNode(int id, RoundQueue<DataStream> inQueue) {
    super(id);
    this.inQueue = inQueue;
    initialize();
  }

  public void initialize() {
    initialized = true;
  }

  public void addConsumers(List<RoundQueue<DataConnection>> consumersQueue) {
    this.consumersQueue.addAll(consumersQueue);
  }

  public void addConsumer(RoundQueue<DataConnection> consumerQueue) {
    this.consumersQueue.add(consumerQueue);
  }

  private boolean initialized = false;

  @Override
  protected void init() {
    while (!initialized) {
      Thread.yield();
    }
    super.init();
  }

  @Override
  protected void runtimeBehavior() throws Throwable {
    // Get a client from the queue
    DataStream dataStream = inQueue.syncPop();
    if (dataStream == null) {
      // Waiting System in Queue couldn't handle something, we handle it here.
      // Possibility of this kind of error is one in million...
      Thread.yield();
    } else {
      try {
        DataConnection dataConnection = dataStream.extractCommand();
        while (dataConnection != null) {
          for (RoundQueue<DataConnection> consumerQueue : consumersQueue) {
            while (!consumerQueue.syncPush(dataConnection)) {
              // Waiting System in Queue couldn't handle something, we handle it here.
              // Possibility of this kind of error is one in million...
            }
          }
          dataConnection = dataStream.extractCommand();
        }
      } catch (ClosedChannelException e) {
        // Client has closed its channel.
        Logger.getLogger(this.getClass()).trace("Client has exited.", e);
      } catch (Exception e) {
        Logger.getLogger(this.getClass()).error("error!", e);
        try {
          dataStream.close();
        } catch (IOException e1) {
        }
      }
    }
  }
}