package org.nise.ux.asl.run;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.data.ServiceException;
import org.nise.ux.asl.data.ServiceResponse;
import org.nise.ux.asl.face.ServiceClient;
import org.nise.ux.asl.face.ServiceServer;
import org.nise.ux.asl.face.Worker;
import org.nise.ux.asl.face.WorkerFactory;
import org.nise.ux.asl.lib.DataStream;
import org.nise.ux.asl.lib.IndexedData;
import org.nise.ux.lib.WaitList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This is the Client Class of this service library.<br/>
 * To use this library create an instance of Server class using {@link ServiceServerBuilder#create()} <br/>
 * <h5>Client Side code:</h5> <code>
 *     &nbsp;&nbsp;&nbsp;ParallelServiceClient client = new ParallelServiceClient(host, port, concurrency);<br/>
 *     &nbsp;&nbsp;&nbsp;TypeToken&lt;ReturnObject&gt; type4Return = new TypeToken&lt;ReturnObject&gt;() {<br/>
 *     &nbsp;&nbsp;&nbsp;};<br/>
 *     &nbsp;&nbsp;&nbsp;ReturnObject response = client.invokeServiceCommand(type4Return, "example", input);<br/><br/>
 * <b> OR </b>
 * <br/><br/>
 *     &nbsp;&nbsp;&nbsp;ParallelServiceClient client = new ParallelServiceClient(host, port, concurrency);<br/>
 *     &nbsp;&nbsp;&nbsp;ReturnObject response = client.invokeServiceCommand(ReturnObject.class, "example", input);<br/>
 * </code>
 * 
 * @see ServiceServerBuilder
 * @see ServiceServer
 * @see SequentialServiceClient
 * @see WorkerFactory
 * @see Worker
 * @see MapCommand
 * @see CommandChain
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public class ParallelServiceClient implements Closeable, ServiceClient {
  private DataStream                dataStream;
  private final String              address;
  private final int                 port;
  private final int                 concurrency;
  private final Thread              th;
  private final Semaphore           lock      = new Semaphore(1);
  private WaitList<ServiceResponse> waitList;
  private Semaphore                 wait_lock = new Semaphore(0);
  private boolean                   closed    = false;

  public ParallelServiceClient(String address, int port, int concurrency) throws IOException {
    this.address = address;
    this.port = port;
    this.concurrency = concurrency;
    closed = false;
    waitList = new WaitList<ServiceResponse>(concurrency);
    dataStream = new DataStream(address, port);
    th = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!closed) {
          try {
            wait_lock.acquire();
            if (!closed) {
              try {
                IndexedData<ServiceResponse> response = dataStream.recieveReturn();
                Logger.getLogger(ParallelServiceClient.class).info("dataStream.recieve==" + response.index + " " + (response.data == null ? null : response.data.getDataAsJson()) + " " + (response.data == null ? null : response.data.getThrowableAsString()));
                waitList.put(response.data, response.index);
              } catch (IOException e) {
                Logger.getLogger(ParallelServiceClient.class).error("Error while recieving response.", e);
                closed |= dataStream.isClosed();
                if (closed) {
                  endReservedIndexes();
                }
              }
            }
          } catch (InterruptedException e) {
            Logger.getLogger(ParallelServiceClient.class).error("Error while recieving response.", e);
          }
        }
      }
    }, "ParallelServiceClient(" + address + ":" + port + ")");
    th.start();
  }

  /**
   * restarts connection to server
   * 
   * @throws IOException
   */
  @Override
  public void restart() throws IOException {
    this.close();
    waitList = new WaitList<ServiceResponse>(concurrency);
    dataStream = new DataStream(address, port);
    closed = false;
    th.start();
  }

  /**
   * invokes Command mapped on server-side function and returns the result, or the Exception/Error that has occurred.
   * 
   * @param type4Return
   *          Type of Object to be returned
   * @param command
   *          server-side command
   * @param data
   *          input arguments
   * @return server-side service return object
   * @throws ServiceException
   *           The Exception/Error that has occurred during execution of service.
   * @throws IOException
   *           Occurs when Connection to server is dropped or lost.
   */
  @Override
  public <RD> RD invokeServiceCommand(TypeToken<RD> type4Return, String command, Object... data) throws ServiceException, IOException {
    ServiceResponse response = __invokeServiceCommand(command, data);
    Throwable throwable = response.getThrowable();
    if (throwable != null)
      throw new ServiceException(throwable);
    return response.getData(type4Return);
  }

  /**
   * invokes Command mapped on server-side function and returns the result, or the Exception/Error that has occurred.
   * 
   * @param class4Return
   *          Class of Object to be returned
   * @param command
   *          server-side command
   * @param data
   *          input arguments
   * @return server-side service return object
   * @throws ServiceException
   *           The Exception/Error that has occurred during execution of service.
   * @throws IOException
   *           Occurs when Connection to server is dropped or lost.
   */
  @Override
  public <RD> RD invokeServiceCommand(Class<RD> class4Return, String command, Object... data) throws ServiceException, IOException {
    ServiceResponse response = __invokeServiceCommand(command, data);
    Throwable throwable = response.getThrowable();
    if (throwable != null)
      throw new ServiceException(throwable);
    return response.getData(class4Return);
  }

  private List<Integer> reservedIndexes          = new ArrayList<Integer>();
  private Semaphore     reservedIndexesSemaphore = new Semaphore(1);
  private int           cnt                      = 0;

  private ServiceResponse __invokeServiceCommand(String command, Object... data) throws IOException {
    return __invokeServiceCommand(false, command, data);
  }

  private ServiceResponse __invokeServiceCommand(boolean helping, String command, Object... data) throws IOException {
    boolean inited = false;
    int index = -1;
    while (!inited) {
      if (closed) {
        throw new ClosedChannelException();
      }
      try {
        index = waitList.reserveIndex();
        if (!closed) {
          reservedIndexesSemaphore.acquire();
          reservedIndexes.add(index);
          reservedIndexesSemaphore.release();
        }
        inited = true;
      } catch (InterruptedException e) {
        Thread.yield();
      }
    }
    if (closed) {
      throw new ClosedChannelException();
    }
    inited = false;
    while (!inited) {
      try {
        lock.acquire();
        inited = true;
      } catch (InterruptedException e) {
        Thread.yield();
      }
    }
    cnt++;
    lock.release();
    Logger.getLogger(ParallelServiceClient.class).info("Already Inside:" + cnt + " started dataStream.sendCommand(" + command + "," + new Gson().toJson(data) + "," + index + ");");
    dataStream.sendCommand(helping, index, command, data);
    wait_lock.release();
    ServiceResponse response = waitList.get(index);
    inited = false;
    while (!inited) {
      try {
        waitList.releaseReserve(index);
        inited = true;
      } catch (InterruptedException e) {
        Thread.yield();
      }
    }
    inited = false;
    while (!inited) {
      try {
        lock.acquire();
        inited = true;
      } catch (InterruptedException e) {
        Thread.yield();
      }
    }
    cnt--;
    lock.release();
    Logger.getLogger(ParallelServiceClient.class).info("Already Inside:" + cnt + " ended dataStream.sendCommand(" + command + "," + new Gson().toJson(data) + "," + index + ") with response=" + new Gson().toJson(response));
    return response;
  }

  /**
   * closes connection to server
   * 
   * @throws IOException
   */
  @Override
  @SuppressWarnings("deprecation")
  public void close() throws IOException {
    closed = true;
    endReservedIndexes();
    th.stop();
    dataStream.close();
    wait_lock.release();
    Thread.yield();
    wait_lock = new Semaphore(0);
  }

  private void endReservedIndexes() {
    try {
      reservedIndexesSemaphore.acquire();
      for (Integer index : reservedIndexes) {
        waitList.put(ServiceResponse.getDataResponse(null), index);
      }
      reservedIndexesSemaphore.release();
    } catch (InterruptedException ie) {
      Logger.getLogger(ParallelServiceClient.class).error("Error while closing failed responses.", ie);
    }
  }

  @Override
  public String[] getPossibleCommands() throws IOException, ServiceException {
    ServiceResponse response = __invokeServiceCommand(true, HELP_GET_COMMAND_SETS);
    Throwable throwable = response.getThrowable();
    if (throwable != null)
      throw new ServiceException(throwable);
    return response.getData(String[].class);
  }

  @Override
  public String[] describeCommand(String command) throws IOException, ServiceException {
    ServiceResponse response = __invokeServiceCommand(true, command, HELP_GET_COMMAND_SETS);
    Throwable throwable = response.getThrowable();
    if (throwable != null)
      throw new ServiceException(throwable);
    return response.getData(String[].class);
  }
}