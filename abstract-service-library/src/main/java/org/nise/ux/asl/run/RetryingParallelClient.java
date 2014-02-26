package org.nise.ux.asl.run;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.ServiceException;
import org.nise.ux.asl.face.ServiceClient;

import com.google.gson.reflect.TypeToken;

public class RetryingParallelClient {
  private static final Logger logger = Logger.getLogger(RetryingParallelClient.class);
  private final ServiceClient client;

  public RetryingParallelClient(String address, int port, int concurrency) throws IOException {
    client = new ParallelServiceClient(address, port, concurrency);
  }

  public <RD> RD getServiceCommand(TypeToken<RD> type4Return, String command, Object... data) throws ServiceException, IOException {
    try {
      return client.invokeServiceCommand(type4Return, command, data);
    } catch (IOException ioe) {
      logger.error("", ioe);
      IOException ioException = ioe;
      try {
        client.close();
      } catch (Throwable t) {
        logger.error("", t);
      }
      for (int i = 0; i < 2; i++) {
        try {
          client.restart();
          return client.invokeServiceCommand(type4Return, command, data);
        } catch (IOException e) {
          logger.error("", e);
          ioException = e;
          try {
            client.close();
          } catch (Throwable t) {
            logger.error("", t);
          }
        }
      }
      throw ioException;
    }
  }

  public <RD> RD getServiceCommand(Class<RD> class4Return, String command, Object... data) throws ServiceException, IOException {
    try {
      return client.invokeServiceCommand(class4Return, command, data);
    } catch (IOException ioe) {
      logger.error("", ioe);
      IOException ioException = ioe;
      try {
        client.close();
      } catch (Throwable t) {
        logger.error("", t);
      }
      for (int i = 0; i < 2; i++) {
        try {
          client.restart();
          return client.invokeServiceCommand(class4Return, command, data);
        } catch (IOException e) {
          logger.error("", e);
          ioException = e;
          try {
            client.close();
          } catch (Throwable t) {
            logger.error("", t);
          }
        }
      }
      throw ioException;
    }
  }
}