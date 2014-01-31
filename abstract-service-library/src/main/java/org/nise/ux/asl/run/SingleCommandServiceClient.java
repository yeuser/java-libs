package org.nise.ux.asl.run;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.data.ServiceException;
import org.nise.ux.asl.data.ServiceResponse;
import org.nise.ux.asl.face.ServiceServer;
import org.nise.ux.asl.face.Worker;
import org.nise.ux.asl.face.WorkerFactory;
import org.nise.ux.asl.lib.DataStream;
import org.nise.ux.asl.lib.IndexedData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * A single command / non-concurrent service client
 * To use this library create an instance of Server class using {@link ServiceServerBuilder#create()} <br/>
 * <h5>Client Side code:</h5> <code>
 *     &nbsp;&nbsp;&nbsp;SingleCommandServiceClient client = new SingleCommandServiceClient(host, port);<br/>
 *     &nbsp;&nbsp;&nbsp;TypeToken&lt;ReturnObject&gt; type4Return = new TypeToken&lt;ReturnObject&gt;() {<br/>
 *     &nbsp;&nbsp;&nbsp;};<br/>
 *     &nbsp;&nbsp;&nbsp;ReturnObject response = client.invokeServiceCommand(type4Return, "example", input);<br/><br/>
 * <b> OR </b>
 * <br/><br/>
 *     &nbsp;&nbsp;&nbsp;SingleCommandServiceClient client = new SingleCommandServiceClient(host, port);<br/>
 *     &nbsp;&nbsp;&nbsp;ReturnObject response = client.invokeServiceCommand(ReturnObject.class, "example", input);<br/>
 * </code>
 * 
 * @see ServiceServerBuilder
 * @see ServiceServer
 * @see ServiceClient
 * @see WorkerFactory
 * @see Worker
 * @see MapCommand
 * @see CommandChain
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public class SingleCommandServiceClient implements Closeable {
  private DataStream   dataStream;
  private final String address;
  private final int    port;
  private boolean      closed = false;

  public SingleCommandServiceClient(String address, int port) throws IOException {
    this.address = address;
    this.port = port;
    closed = false;
    dataStream = new DataStream(address, port);
  }

  /**
   * restarts connection to server
   * 
   * @throws IOException
   */
  public void restart() throws IOException {
    this.close();
    dataStream = new DataStream(address, port);
    closed = false;
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
  public <RD> RD invokeServiceCommand(TypeToken<RD> type4Return, String command, Object... data) throws ServiceException, IOException {
    ServiceResponse response = __invokeServiceCommand(command, data);
    if (response.getThrowable() != null)
      throw new ServiceException(response.getThrowable());
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
  public <RD> RD invokeServiceCommand(Class<RD> class4Return, String command, Object... data) throws ServiceException, IOException {
    ServiceResponse response = __invokeServiceCommand(command, data);
    if (response.getThrowable() != null)
      throw new ServiceException(response.getThrowable());
    return response.getData(class4Return);
  }

  private int cnt = 0;

  private ServiceResponse __invokeServiceCommand(String command, Object... data) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }
    cnt++;
    int index = cnt;
    Logger.getLogger(SingleCommandServiceClient.class).info("Already Inside:" + cnt + " started dataStream.sendCommand(" + command + "," + new Gson().toJson(data) + "," + index + ");");
    dataStream.sendCommand(index, command, data);
    IndexedData<ServiceResponse> response = dataStream.recieveReturn();
    Logger.getLogger(SingleCommandServiceClient.class).info("dataStream.recieve==" + response.index + " " + (response.data == null ? null : response.data.getDataAsJson()) + " " + (response.data == null ? null : response.data.getThrowableAsString()));
    return response.data;
  }

  /**
   * closes connection to server
   * 
   * @throws IOException
   */
  @Override
  public void close() throws IOException {
    closed = true;
    dataStream.close();
    Thread.yield();
  }
}