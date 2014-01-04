package org.nise.ux.asl.run;

import java.util.Map;

import org.apache.log4j.Logger;
import org.nise.ux.asl.face.ServiceServer;
import org.nise.ux.asl.face.ServiceServerMXBean;
import org.nise.ux.configuration.BasicConfigurations;
import org.nise.ux.configuration.ConfigurationsHandler;

abstract class ServiceServerAbstract implements ConfigurationsHandler, ServiceServer, ServiceServerMXBean {
  private int            inSystem = 0;
  private long           clients  = 0;
  private Configurations configurations;

  public ServiceServerAbstract(Map<String, String> configurations) {
    String[][] configurationSet = Map2Array(configurations);
    this.configurations = new Configurations(this, configurationSet);
  }

  private String[][] Map2Array(Map<String, String> configurations) {
    String[] keys = configurations.keySet().toArray(new String[] {});
    String[][] configurationSet = new String[keys.length][];
    for (int i = 0; i < configurationSet.length; i++) {
      configurationSet[i] = new String[] { keys[i], configurations.get(keys[i]) };
    }
    return configurationSet;
  }

  /**
   * Set a configuration using its name
   * 
   * @param key
   *          name of configuration
   * @param value
   *          value of configuration
   */
  @Override
  public final void setConfiguration(String key, String value) {
    this.configurations._setConfiguration(key, value);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#getBooleanConfiguration(java.lang.String)
   */
  @Override
  public final boolean getBooleanConfiguration(String key) {
    return this.configurations.getBooleanConfiguration(key);
  }

  /**
   * Get integer value of a configuration using its name
   * 
   * @param key
   *          name of configuration
   * @return value of configuration
   */
  @Override
  public final int getIntegerConfiguration(String key) {
    return this.configurations.getIntegerConfiguration(key);
  }

  @Override
  public final String getConfiguration(String key) {
    return this.configurations.getConfiguration(key);
  }

  @Override
  public final String[] getConfigurationKeys() {
    return this.configurations.getAllConfigurationKeys();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#getListeningPort()
   */
  @Override
  public final int getListeningPort() {
    return getIntegerConfiguration(LISTENING_PORT);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#getMax_clients()
   */
  @Override
  public final int getMax_clients() {
    return getIntegerConfiguration(MAX_CLIENTS);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#isTest_mode()
   */
  @Override
  public final boolean isTest_mode() {
    return getBooleanConfiguration(TEST_MODE);
  }

  public final void setListeningPort(int port) {
    setConfiguration(LISTENING_PORT, String.valueOf(port));
  }

//  public final void setFetcherNo(int count) {
//    setConfiguration(FETCHER_NO, String.valueOf(count));
//  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#setMax_clients(int)
   */
  @Override
  public final void setMax_clients(int max_clients) {
    setConfiguration(MAX_CLIENTS, String.valueOf(max_clients));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#setTest_mode(boolean)
   */
  @Override
  public final void setTest_mode(boolean test_mode) {
    setConfiguration(TEST_MODE, String.valueOf(test_mode));
  }

  final synchronized int incInSystem() {
    inSystem++;
    clients++;
    return inSystem;
  }

  final synchronized int decInSystem() {
    inSystem--;
    return inSystem;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#getInSystem()
   */
  @Override
  public final int getInSystem() {
    return inSystem;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#getClients()
   */
  @Override
  public final long getClients() {
    return clients;
  }

  protected abstract void start();

  class Configurations extends BasicConfigurations {
    private ConfigurationsHandler handler;

    public Configurations(ConfigurationsHandler handler, String[][]... configurationSet) {
      super(configurationSet);
      this.handler = handler;
    }

    private void _setConfiguration(String key, String configuration) {
      super.setConfiguration(key, configuration);
      try {
        Logger.getLogger(this.getClass()).info(key + ":" + configuration + ":" + handler);
        ConfigurationsHandler configHandler = handler;
        configHandler.refreshConfig(key, configuration);
      } catch (Exception e) {
        Logger.getLogger(this.getClass()).error("error!", e);
      }
    }
  }
}