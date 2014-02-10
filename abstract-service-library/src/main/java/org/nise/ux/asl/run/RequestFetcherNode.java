package org.nise.ux.asl.run;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.ServiceException;
import org.nise.ux.asl.data.ServiceResponse;
import org.nise.ux.asl.face.DataConnection;
import org.nise.ux.asl.face.ServiceClient;
import org.nise.ux.asl.lib.DataStream;
import org.nise.ux.lib.Living;

class RequestFetcherNode extends Living {
  private final List<QueueFace<DataConnection>> consumersQueue = new ArrayList<QueueFace<DataConnection>>();
  private final QueueFace<DataStream>           inQueue;

  public RequestFetcherNode(int id, QueueFace<DataStream> inQueue) {
    super(id);
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
    DataStream dataStream = inQueue.pop();
    if (dataStream == null) {
      // Waiting System in Queue couldn't handle something, we handle it here.
      // Possibility of this kind of error is one in million...
      Thread.yield();
    } else {
      try {
        DataConnection dataConnection = dataStream.extractCommand();
        while (dataConnection != null) {
          if (dataConnection.isHelp() && dataConnection.getCommand().equals(ServiceClient.HELP_GET_COMMAND_SETS)) {
            List<String> commandSet = new ArrayList<String>();
            for (QueueFace<DataConnection> consumerQueue : consumersQueue) {
              String[] commands = consumerQueue.getCommands();
              for (String command : commands) {
                if (!commandSet.contains(command)) {
                  commandSet.add(command);
                }
              }
            }
            dataConnection.send(ServiceResponse.getDataResponse(commandSet));
          } else {
            boolean has_handler = false;
            String command = dataConnection.getCommand();
            for (QueueFace<DataConnection> consumerQueue : consumersQueue) {
              if (consumerQueue.hasCommand(command)) {
                has_handler = true;
                while (!consumerQueue.push(dataConnection)) {
                  // Waiting System in Queue couldn't handle something, we handle it here.
                  // Possibility of this kind of error is one in million...
                }
              }
            }
            if (!has_handler) {
              dataConnection.send(ServiceResponse.getThrowableResponse(new ServiceException("Server has no implementation for command `" + command + "`")));
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