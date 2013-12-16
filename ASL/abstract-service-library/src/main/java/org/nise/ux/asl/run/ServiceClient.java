package org.nise.ux.asl.run;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.data.ServiceResponse;
import org.nise.ux.asl.face.Worker;
import org.nise.ux.asl.face.WorkerFactory;
import org.nise.ux.asl.lib.DataStream;
import org.nise.ux.asl.lib.IndexedData;
import org.nise.ux.lib.WaitList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This is the Main Constructor Class of this service library.<br/>
 * To use this library create an instance of this class, then use {@link #create()} to start the service <h5>Example:</h5> <code>
 *     &nbsp;&nbsp;&nbsp;ServiceClient client = new ServiceClient(host, port, concurrency);<br/>
 *     &nbsp;&nbsp;&nbsp;TypeToken&lt;ReturnObject&gt; type4Return = new TypeToken&lt;ReturnObject&gt;() {<br/>
 *     &nbsp;&nbsp;&nbsp;};<br/>
 *     &nbsp;&nbsp;&nbsp;ReturnObject response = client.invokeServiceCommand(type4Return, "example", input);<br/><br/>
 * <b> OR </b>
 * <br/><br/>
 *     &nbsp;&nbsp;&nbsp;ServiceClient client = new ServiceClient(host, port, concurrency);<br/>
 *     &nbsp;&nbsp;&nbsp;ReturnObject response = client.invokeServiceCommand(ReturnObject.class, "example", input);<br/>
 * </code>
 * 
 * @see ServiceServerBuilder
 * @see WorkerFactory
 * @see Worker
 * @see MapCommand
 * @see CommandChain
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public class ServiceClient implements Closeable {
  private DataStream                dataStream;
  private final String              address;
  private final int                 port;
  private final int                 concurrency;
  private final Thread              th;
  private final Semaphore           lock      = new Semaphore(1);
  private WaitList<ServiceResponse> waitList;
  private Semaphore                 wait_lock = new Semaphore(0);
  private boolean                   closed    = false;

  public ServiceClient(String address, int port, int concurrency) throws IOException {
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
                Logger.getLogger(ServiceClient.class).info("dataStream.recieve==" + response.index + " " + response.data);
                waitList.put(response.data, response.index);
              } catch (IOException e) {
                Logger.getLogger(ServiceClient.class).error("Error while recieving response.", e);
              }
            }
          } catch (InterruptedException e) {
            Logger.getLogger(ServiceClient.class).error("Error while recieving response.", e);
          }
        }
      }
    }, "ServiceClient(" + address + ":" + port + ")");
    th.start();
  }

  /**
   * restarts connection to server
   * 
   * @throws IOException
   */
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
   * @throws Throwable
   *           the Exception/Error that has occurred during execution of service.
   */
  public <RD> RD invokeServiceCommand(TypeToken<RD> type4Return, String command, Object... data) throws Throwable {
    ServiceResponse response = __invokeServiceCommand(command, data);
    if (response.throwable != null)
      throw response.throwable;
    try {
      return new Gson().fromJson(response.data, type4Return.getType());
    } catch (Throwable t) {
      Logger.getLogger(getClass()).error("response.data=" + response.data + " command=" + command + " data=" + new Gson().toJson(data), t);
      throw t;
    }
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
   * @throws Throwable
   *           the Exception/Error that has occurred during execution of service.
   */
  public <RD> RD invokeServiceCommand(Class<RD> class4Return, String command, Object... data) throws Throwable {
    ServiceResponse response = __invokeServiceCommand(command, data);
    if (response.throwable != null)
      throw response.throwable;
    try {
      return new Gson().fromJson(response.data, class4Return);
    } catch (Throwable t) {
      Logger.getLogger(getClass()).error("response.data=" + response.data + " command=" + command + " data=" + new Gson().toJson(data), t);
      throw t;
    }
  }

  private int cnt = 0;

  private ServiceResponse __invokeServiceCommand(String command, Object... data) throws InterruptedException, IOException {
    int index = waitList.reserveIndex();
    lock.acquire();
    cnt++;
    lock.release();
    Logger.getLogger(ServiceClient.class).info("Already Inside:" + cnt + " started dataStream.sendCommand(" + command + "," + new Gson().toJson(data) + "," + index + ");");
    dataStream.sendCommand(index, command, data);
    wait_lock.release();
    ServiceResponse response = waitList.get(index);
    waitList.releaseReserve(index);
    lock.acquire();
    cnt--;
    lock.release();
    Logger.getLogger(ServiceClient.class).info("Already Inside:" + cnt + " ended dataStream.sendCommand(" + command + "," + new Gson().toJson(data) + "," + index + ") with response=" + response);
    return response;
  }

  /**
   * closes connection to server
   * 
   * @throws IOException
   */
  @SuppressWarnings("deprecation")
  @Override
  public void close() throws IOException {
    closed = true;
    th.stop();
    dataStream.close();
    wait_lock.release();
    Thread.yield();
    wait_lock = new Semaphore(0);
  }
}