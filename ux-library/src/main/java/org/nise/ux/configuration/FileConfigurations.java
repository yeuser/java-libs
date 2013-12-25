package org.nise.ux.configuration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

public class FileConfigurations extends BasicConfigurations {
  private String CONFIG_CONF;

  public FileConfigurations(String config_conf, String[][]... all_keys_defualts_set) {
    super(all_keys_defualts_set);
    this.CONFIG_CONF = config_conf;
    // load ServiceServerLiveConfigurations from <config file>
    loadConfigurations();
    // store all ServiceServerLiveConfigurations {HardCoded or loaded} into <config file>
    storeConfigurations();
  }

  public final void loadConfigurations() {
    try {
      BufferedReader in = new BufferedReader(new FileReader(CONFIG_CONF));
      String str = in.readLine();
      while (str != null) {
        if (str.startsWith("#")) {
          // do noting
        } else {
          //          String[] configurationKeys = getAllConfigurationKeys();
          //          for (String key : configurationKeys) {
          //            if (str.startsWith(key + ":")) {
          int idx = str.indexOf(':');
          String key = str.substring(0, idx).trim();
          String value = str.substring(idx + 1).trim();
          setConfiguration(key, value);
          //              break;
          //            }
          //          }
        }
        str = in.readLine();
      }
      in.close();
    } catch (IOException e1) {
    }
  }

  public final void storeConfigurations() {
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG_CONF));
      String[] configurationKeys = getAllConfigurationKeys();
      for (String key : configurationKeys) {
        bw.write(key + ": " + getConfiguration(key));
        bw.newLine();
      }
      bw.flush();
      bw.close();
    } catch (IOException e) {
      Logger.getLogger(this.getClass()).error("error!", e);
    }
  }

  public void reload() {
    loadConfigurations();
    super.reload();
  }
}