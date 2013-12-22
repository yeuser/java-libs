package org.nise.ux.asl.run;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.nise.ux.asl.face.DataConnection;
import org.nise.ux.lib.Living;

class DistributerNode extends Living {
  private final List<QueueFace<DataConnection>> consumersQueue = new ArrayList<QueueFace<DataConnection>>();
  private final QueueFace<DataConnection>       inQueue;
  private String                                name;

  public DistributerNode(int id, QueueFace<DataConnection> inQueue, String name) {
    super(id);
    this.name = name;
    this.inQueue = inQueue;
    initialize();
  }

  public void initialize() {
    initialized = true;
  }

  public void addConsumers(List<QueueFace<DataConnection>> consumersQueue) {
    this.consumersQueue.addAll(consumersQueue);
  }

  public void addConsumer(QueueFace<DataConnection> consumerQueue) {
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
    DataConnection dataConnection = inQueue.pop();
    if (dataConnection == null) {
      // Waiting System in Queue couldn't handle something, we handle it here.
      // Possibility of this kind of error is one in million...
      Thread.yield();
    } else {
      Logger.getLogger(DistributerNode.class).debug("@node=" + name + " loaded DataConnection: command=" + dataConnection.getCommand() + " index=" + dataConnection.getIndex() + " dataConnection=" + dataConnection);
      try {
        if (dataConnection != null) {
          handleDataConnection(dataConnection);
        }
      } catch (Exception e) {
        Logger.getLogger(this.getClass()).error("error!", e);
      }
    }
  }

  protected void handleDataConnection(DataConnection dataConnection) {
    Logger.getLogger(DistributerNode.class).debug("handleDataConnection: command=" + dataConnection.getCommand() + " index=" + dataConnection.getIndex() + " dataConnection=" + dataConnection);
    for (QueueFace<DataConnection> consumerQueue : consumersQueue) {
      if (consumerQueue.hasCommand(dataConnection.getCommand())) {
        while (!consumerQueue.push(dataConnection)) {
          // Waiting System in Queue couldn't handle something, we handle it here.
          // Possibility of this kind of error is one in million...
        }
      }
    }
  }
}