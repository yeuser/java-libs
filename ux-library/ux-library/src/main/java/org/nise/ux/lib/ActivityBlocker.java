package org.nise.ux.lib;

import java.util.Vector;
import java.util.concurrent.Semaphore;

/**
 * <p dir=rtl>
 * کلاس کمکی برای ایجاد قفل در منطقه بحرانی وابسته به یک کلید.<br/>
 * به کمک این کلاس میتوان مناطق بحرانی اجرای عملیات با استفاده از یک کلید خاص ایجاد کرد.<br/>
 * ظرفیت هر منطقه بحران در این کلاس تنها یکی خواهد بود.
 * </p>
 */
public class ActivityBlocker {
  private Semaphore      lock     = new Semaphore(1);
  private Vector<String> blockeds = new Vector<String>();

  /**
   * درخواست اجازه برای ورود به منطقه بحرانی با استفاده از کلید داده شده.
   * 
   * @param key
   *          کلید ورود به منطقه بحرانی
   * @throws InterruptedException
   *           تنها وقتی که سیستم توانایی ایجاد سمافور نداشته باشد
   */
  public void acquire(String key) throws InterruptedException {
    while (true) {
      lock.acquire();
      if (!blockeds.contains(key)) {
        blockeds.add(key);
        lock.release();
        break;
      }
      lock.release();
      Thread.yield();
    }
  }

  /**
   * رها سازی اجازه گرفته شده و خروج از منطقه بحرانی
   * 
   * @param key
   *          کلید ورود به منطقه بحرانی
   */
  public void release(String key) {
    blockeds.remove(key);
  }
}
