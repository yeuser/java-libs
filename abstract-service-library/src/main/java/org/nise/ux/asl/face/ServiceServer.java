package org.nise.ux.asl.face;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Interface of a Server side Service object.
 * 
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public interface ServiceServer extends Closeable {
  /**
   * constant value equal to {@value #LISTENING_PORT}
   */
  public final static String LISTENING_PORT     = "LISTENING_PORT";
  /**
   * constant value equal to {@value #FETCHER_NO}
   */
  public final static String FETCHER_NO         = "FETCHER_NO";
  /**
   * constant value equal to {@value #MAX_CLIENTS}
   */
  public static final String MAX_CLIENTS        = "MAX_CLIENTS";
  /**
   * constant value equal to {@value #TEST_MODE}
   */
  public static final String TEST_MODE          = "TEST_MODE";
  /**
   * constant value equal to {@value #QUEUE_LENGTH_CONST}
   */
  public static final String QUEUE_LENGTH_CONST = "QUEUE_LENGTH_CONST";

  /**
   * Set value of a configuration
   * 
   * @param key
   *          name of configuration
   * @param value
   *          value of configuration
   */
  public void setConfiguration(String key, String value);

  /**
   * Get value of a configuration using its name
   * 
   * @param key
   *          name of configuration
   * @return value of configuration
   */
  public String getConfiguration(String key);

  /**
   * Get integer value of a configuration using its name
   * 
   * @param key
   *          name of configuration
   * @return integer value of configuration
   */
  public int getIntegerConfiguration(String key);

  /**
   * Get boolean value of a configuration using its name
   * 
   * @param key
   *          name of configuration
   * @return boolean value of configuration
   */
  public boolean getBooleanConfiguration(String key);

  /**
   * Get boolean value of a configuration using its name
   * 
   * @param key
   *          name of configuration
   * @return value of configuration
   */
  public String[] getConfigurationKeys();

  /**
   * Get number of ServiceServer Workers of name 'worker' configured.
   * 
   * @return number of ServiceServer Workers of name 'worker'
   */
  public int getWorkerNo(String worker);

  /**
   * Get a list of Workers.
   * 
   * @return list of Workers
   */
  public String[] getWorkerNames();

  /**
   * Get value of configuration {@link #LISTENING_PORT} <dd>equal to <code>getIntegerConfiguration({@link #LISTENING_PORT})</code></dd>
   * 
   * @return value of configuration {@link #LISTENING_PORT}
   */
  public int getListeningPort();

  /**
   * Get value of configuration {@link #MAX_CLIENTS} <dd>equal to <code>getIntegerConfiguration({@link #MAX_CLIENTS})</code></dd>
   * 
   * @return value of configuration {@link #MAX_CLIENTS}
   */
  public int getMax_clients();

  /**
   * Get value of configuration {@link #TEST_MODE} <dd>equal to <code>getBooleanConfiguration({@link #TEST_MODE})</code></dd>
   * 
   * @return value of configuration {@link #TEST_MODE}
   */
  public boolean isTest_mode();

  /**
   * Set value of configuration for {@link #MAX_CLIENTS} <dd>equal to <code>setConfiguration({@value #MAX_CLIENTS}, String.valueOf(max_clients))</code></dd>
   * 
   * @param max_clients
   *          value for configuration {@link #MAX_CLIENTS}
   */
  public void setMax_clients(int max_clients);

  /**
   * Set value of configuration for {@link #TEST_MODE} <dd>equal to <code>setConfiguration({@value #TEST_MODE}, String.valueOf(test_mode))</code></dd>
   * 
   * @param test_mode
   *          value for configuration {@link #TEST_MODE}
   */
  public void setTest_mode(boolean test_mode);

  public int getInSystem();

  public long getClients();

  /**
   * Gives all system status in a Map of "{Status name}" -> "{Status value}"
   * 
   * @return all system status
   */
  public Map<String, String> getAllStats();

  /**
   * Gives all system status in a Map of "{prefix}{Status name}" -> "{Status value}"
   * 
   * @return all system status
   */
  public Map<String, String> getAllStats(String prefix);

  /**
   * Closes service-server by ending all client requests if 'exit process' is not forced.
   * 
   * @param force
   *          If set true service is closed forcefully, and
   *          If set false service is closed gracefully.
   */
  public void exit(boolean force);

  /**
   * Closes service-server by ending all client requests gracefully.
   * <b>same as {@link #exit(boolean) exit(false)}</b>
   */
  @Override
  public void close() throws IOException;
}