package org.nise.ux.asl.run;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.ServiceException;
import org.nise.ux.asl.face.ServiceClient;

import com.google.gson.reflect.TypeToken;

public class RetryingParallelClient {
  private static ServiceClient          client;
  private static RetryingParallelClient instance;
  private static Semaphore              instance_lock = new Semaphore(1);
  private static final Logger           logger        = Logger.getLogger(RetryingParallelClient.class);
  public static String                  address       = "127.0.0.1";
  public static int                     port          = 0;
  public static int                     concurrency   = 50;

  private RetryingParallelClient() throws IOException {
    client = new ParallelServiceClient(address, port, concurrency);
  }

  public static RetryingParallelClient getInstance() throws InterruptedException {
    if (instance == null) {
      instance_lock.acquire();
      if (instance == null) {
        try {
          instance = new RetryingParallelClient();
        } catch (IOException e) {
          logger.error("", e);
        }
      }
      instance_lock.release();
    }
    return instance;
  }

  public <RD> RD getServiceCommand(TypeToken<RD> type4Return, String command, Object... data) throws ServiceException, IOException {
    try {
      return client.invokeServiceCommand(type4Return, command, data);
    } catch (IOException ioe) {
      logger.error("", ioe);
      IOException ioException = ioe;
      ServiceClient _client = client;
      instance = null;
      try {
        _client.close();
      } catch (Throwable t) {
        logger.error("", t);
      }
      for (int i = 0; i < 2; i++) {
        try {
          try {
            RetryingParallelClient.getInstance();
          } catch (InterruptedException e) {
            logger.error("", e);
          }
          return client.invokeServiceCommand(type4Return, command, data);
        } catch (IOException e) {
          logger.error("", e);
          ioException = e;
          _client = client;
          instance = null;
          try {
            _client.close();
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
      ServiceClient _client = client;
      instance = null;
      try {
        _client.close();
      } catch (Throwable t) {
        logger.error("", t);
      }
      for (int i = 0; i < 2; i++) {
        try {
          try {
            RetryingParallelClient.getInstance();
          } catch (InterruptedException e) {
            logger.error("", e);
          }
          return client.invokeServiceCommand(class4Return, command, data);
        } catch (IOException e) {
          logger.error("", e);
          ioException = e;
          _client = client;
          instance = null;
          try {
            _client.close();
          } catch (Throwable t) {
            logger.error("", t);
          }
        }
      }
      throw ioException;
    }
  }
}