package org.nise.ux.asl.face;

import org.nise.ux.configuration.StatusInformer;


public interface ServiceServerMXBean extends StatusInformer {
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
}