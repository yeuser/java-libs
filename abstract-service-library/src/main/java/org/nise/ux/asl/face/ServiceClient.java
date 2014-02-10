package org.nise.ux.asl.face;

import java.io.IOException;

import org.nise.ux.asl.data.ServiceException;

import com.google.gson.reflect.TypeToken;

public interface ServiceClient {
  final static String  HELP_GET_COMMAND_SETS = "HELP_GET_COMMAND_SETS";
  final static int HELP_DESCRIBE_COMMAND = 100;

  /**
   * restarts connection to server
   * 
   * @throws IOException
   */
  public void restart() throws IOException;

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
  public <RD> RD invokeServiceCommand(TypeToken<RD> type4Return, String command, Object... data) throws ServiceException, IOException;

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
  public <RD> RD invokeServiceCommand(Class<RD> class4Return, String command, Object... data) throws ServiceException, IOException;

  /**
   * closes connection to server
   * 
   * @throws IOException
   */
  public void close() throws IOException;

  public String[] getPossibleCommands() throws IOException, ServiceException;

  public String[] describeCommand(String command) throws IOException, ServiceException;
}