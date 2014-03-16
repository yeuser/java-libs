package org.nise.ux.configuration;

public interface ConfigurationMXBean {
  public String[] getAllConfigurationKeys();

  public String[] getAllConfigKeys(String prefix);

  /**
   * مقدار تنظيمات مربوط به يک پارامتر خاص را برمی‌گرداند
   * 
   * @param key
   *          : پارامتر
   * @return تنظيمات مربوط به پارامتر
   */
  public String getConfiguration(String key);

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت boolean برمی‌گرداند
   * </p>
   * 
   * @param key
   *          : کلید
   * @return مقدار تنظيم مربوط به پارامتر
   */
  public boolean getBooleanConfiguration(String key);

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت int برمی‌گرداند
   * </p>
   * 
   * @param key
   *          : کلید
   * @return مقدار تنظيم مربوط به پارامتر
   */
  public int getIntegerConfiguration(String key);

  /**
   * <p dir=rtl>
   * مقدار تنظيمات مربوط به يک پارامتر خاص را بصورت double برمی‌گرداند
   * </p>
   * 
   * @param key
   *          : کلید
   * @return مقدار تنظيم مربوط به پارامتر
   */
  public double getDoubleConfiguration(String key);

  /**
   * مقدار تنظيمات مربوط به پارامتری خاص، از طريق اين متد تنظيم می‌شود
   * 
   * @param key
   *          : پارامتر
   * @param config
   *          : مقدار تنظيمات
   */
  public void setConfiguration(String key, String config);

  public void reload();
}