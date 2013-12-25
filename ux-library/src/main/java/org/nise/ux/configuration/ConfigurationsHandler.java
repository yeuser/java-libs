package org.nise.ux.configuration;

public interface ConfigurationsHandler {
  public void refreshAll();

  public void refreshConfig(String key, String config);
}