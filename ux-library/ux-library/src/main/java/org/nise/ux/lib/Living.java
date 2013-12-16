package org.nise.ux.lib;

import org.apache.log4j.Logger;

/**
 * اين کلاس برای مديريت‫ threadها استفاده می‌شود.
 */
public abstract class Living implements Runnable {
  private boolean running = true;
  private Thread  th      = new Thread(this);

  public Living(int id) {
    th.setName(this.getClass().getName() + "_#" + id);
    th.setDaemon(false);
    th.start();
  }

  public Living(String name) {
    th.setName(name);
    th.setDaemon(false);
    th.start();
  }

  @Override
  public void run() {
    init();
    while (running) {
      try {
        runtimeBehavior();
      } catch (Throwable t) {
        Logger.getLogger(this.getClass()).error("", t);
      }
    }
    Logger.getLogger(this.getClass()).trace(th.getName() + " died.");
  }

  protected void init() {
    // Possible first time initializations for overriding classes.
  }

  protected abstract void runtimeBehavior() throws Throwable;

  public void die() {
    running = false;
  }
}