package org.nise.ux.configuration;

import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;

class StatusProfiler implements Runnable {
  private Vector<Socket> status_listeners = new Vector<Socket>();

  StatusProfiler() {
    new Thread(this).start();
  }

  public void addListener(Socket clientDataPipe) {
    status_listeners.add(clientDataPipe);
    Logger.getLogger(this.getClass()).info("Status listener added.");
  }

  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      int i = 0;
      while (i < status_listeners.size()) {
        try {
          // Global.printState(status_listeners.elementAt(i).getOutputStream());
          i++;
        } catch (Exception e) {
          status_listeners.remove(i);
          Logger.getLogger(this.getClass()).error("Status listener error: ", e);
        }
      }
    }
  }
}