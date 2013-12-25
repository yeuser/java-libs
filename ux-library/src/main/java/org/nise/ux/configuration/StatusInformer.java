package org.nise.ux.configuration;

import java.util.Map;

public interface StatusInformer {

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
}