package org.nise.ux.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

public class BasicConfigurations implements ConfigurationMXBean {
  private HashMap<String, String> configurations         = new HashMap<String, String>();
  private HashMap<String, String> configurationsHandlers = new HashMap<String, String>();
  /**
   * <p>
   * All default_coded_configurations are here.
   * </p>
   * <p>
   * Array of {&lt;configuration name&gt;,&lt;default value&gt;,&lt;on change handler&gt;}
   * </p>
   * <ul>
   * <li>
   * <p>
   * handlers are designed to give administrators ability to make system initialization time ServiceServerLiveConfigurations LIVE.
   * </p>
   * </li>
   * <li>
   * <p>
   * If you get configuration value every time you use it you don't need any handler; like using it inside a Thread.
   * </p>
   * </li>
   * <li>
   * <p>
   * but if you get it just to construct a parameter inside a live thread or in initiation only; you should make a handler or it would not be a live configuration parameter.
   * </p>
   * </li>
   * <p>
   * -----------------
   * </p>
   * <p>
   * For more details See {@link ConfigurationsHandler}
   * </p>
   */
  protected final String[][]      ALL_KEYS_DEFUALTS;

  public BasicConfigurations(String[][]... all_keys_defaults_set) {
    this(new String[][] {}, all_keys_defaults_set);
  }

  public BasicConfigurations(String[][] KEYS_DEFUALTS, String[][]... all_keys_defaults_set) {
    int len = KEYS_DEFUALTS.length;
    for (String[][] all_keys_defaults : all_keys_defaults_set) {
      len += all_keys_defaults.length;
    }
    ALL_KEYS_DEFUALTS = new String[len][];
    int olen = 0;
    len = KEYS_DEFUALTS.length;
    System.arraycopy(KEYS_DEFUALTS, 0, ALL_KEYS_DEFUALTS, olen, len);
    for (String[][] all_keys_defaults : all_keys_defaults_set) {
      len = all_keys_defaults.length;
      System.arraycopy(all_keys_defaults, 0, ALL_KEYS_DEFUALTS, olen, len);
      olen += len;
    }
    for (String[] key : ALL_KEYS_DEFUALTS) {
      configurations.put(key[0], key[1]);
      if (key.length > 2 && key[2] != null && key[2].length() > 0) {
        configurationsHandlers.put(key[0], key[2]);
      }
    }
  }

  @Override
  public final String[] getAllConfigurationKeys() {
    ArrayList<String> a = new ArrayList<String>(configurations.keySet());
    ArrayList<String> list = new ArrayList<String>();
    for (String[] row : ALL_KEYS_DEFUALTS) {
      list.add(row[0]);
      a.remove(row[0]);
    }
    list.addAll(a);
    return list.toArray(new String[] {});
  }

  @Override
  public final String[] getAllConfigKeys(String prefix) {
    ArrayList<String> keys = new ArrayList<String>();
    for (int i = 0; i < ALL_KEYS_DEFUALTS.length; i++) {
      if (ALL_KEYS_DEFUALTS[i][0].startsWith(prefix))
        keys.add(ALL_KEYS_DEFUALTS[i][0]);
    }
    return keys.toArray(new String[0]);
  }

  /**
   * مقدار تنظيمات مربوط به يک پارامتر خاص را برمی‌گرداند
   * 
   * @param key
   *          : پارامتر
   * @return تنظيمات مربوط به پارامتر
   */
  @Override
  public final String getConfiguration(String key) {
    return configurations.get(key);
  }

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت boolean برمی‌گرداند
   * </p>
   * 
   * @param key
   *          : کلید
   * @return مقدار تنظيم مربوط به پارامتر
   */
  @Override
  public final boolean getBooleanConfiguration(String key) {
    String configurationValue = configurations.get(key);
    return getConfigurationAsBoolean(configurationValue);
  }

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت boolean برمی‌گرداند
   * </p>
   * 
   * @param configurationValue
   *          مقدار تنظيم مربوط به پارامتر
   * @return <span dir=rtl>مقدار تنظيم مربوط به پارامتر بصورت boolean</span>
   */
  public static boolean getConfigurationAsBoolean(String configurationValue) {
    return configurationValue.trim().equalsIgnoreCase("yes") || configurationValue.trim().equalsIgnoreCase("on") || configurationValue.trim().equalsIgnoreCase("true");
  }

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت int برمی‌گرداند
   * </p>
   * 
   * @param key
   *          : کلید
   * @return مقدار تنظيم مربوط به پارامتر
   */
  @Override
  public final int getIntegerConfiguration(String key) {
    String configurationValue = configurations.get(key);
    return getConfigurationAsInteger(configurationValue);
  }

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت int برمی‌گرداند
   * </p>
   * 
   * @param configurationValue
   *          مقدار تنظيم مربوط به پارامتر
   * @return <span dir=rtl>مقدار تنظيم مربوط به پارامتر بصورت int</span>
   */
  public static int getConfigurationAsInteger(String configurationValue) {
    return Integer.parseInt(configurationValue.trim());
  }

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت double برمی‌گرداند
   * </p>
   * 
   * @param key
   *          : کلید
   * @return مقدار تنظيم مربوط به پارامتر
   */
  @Override
  public final double getDoubleConfiguration(String key) {
    return Double.parseDouble(configurations.get(key).trim());
  }

  /**
   * مقدار تنظيمات مربوط به پارامتری خاص، از طريق اين متد تنظيم می‌شود
   * 
   * @param key
   *          : پارامتر
   * @param config
   *          : مقدار تنظيمات
   */
  @Override
  public final void setConfiguration(String key, String config) {
    configurations.put(key, config);
    String handler = configurationsHandlers.get(key);
    if (handler != null) {
      handleConfiguration(key, config, handler);
    }
  }

  protected void handleConfiguration(String key, String configuration, String handler) {
    try {
      Logger.getLogger(this.getClass()).info(key + ":" + handler + ":" + Class.forName(handler) + ":" + (Class.forName(handler).newInstance()));
      ((ConfigurationsHandler) (Class.forName(handler).newInstance())).refreshConfig(key, configuration);
    } catch (InstantiationException e) {
      configurationsHandlers.remove(key);
    } catch (IllegalAccessException e) {
      configurationsHandlers.remove(key);
    } catch (ClassNotFoundException e) {
      configurationsHandlers.remove(key);
    } catch (Exception e) {
      Logger.getLogger(this.getClass()).error("error!", e);
    }
  }

  @Override
  public void reload() {
    String[] keys = configurationsHandlers.keySet().toArray(new String[] {});
    Vector<String> unique_values = new Vector<String>();
    for (String key : keys) {
      String value = configurationsHandlers.get(key);
      if (!unique_values.contains(value)) {
        unique_values.add(value);
      }
    }
    for (String handler : unique_values) {
      try {
        ((ConfigurationsHandler) (Class.forName(handler).newInstance())).refreshAll();
      } catch (Exception e) {
      }
    }
  }
}