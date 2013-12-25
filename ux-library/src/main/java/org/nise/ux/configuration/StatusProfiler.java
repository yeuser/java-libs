package org.nise.ux.configuration;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nise.ux.lib.Living;

class StatusProfiler extends Living {
  private List<Socket>   status_listeners = new ArrayList<Socket>();
  private StatusInformer statusInformer;

  StatusProfiler(StatusInformer statusInformer) {
    super("StatusProfiler");
    this.statusInformer = statusInformer;
  }

  public void addListener(Socket clientDataPipe) {
    status_listeners.add(clientDataPipe);
    Logger.getLogger(this.getClass()).info("Status listener added.");
  }

  @Override
  protected void runtimeBehavior() throws Throwable {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    Map<String, String> stats = statusInformer.getAllStats();
    StringBuilder sb = new StringBuilder('{');
    boolean b = false;
    for (String key : stats.keySet()) {
      b = true;
      sb.append(',');
      sb.append(key);
      sb.append(':');
      sb.append(stats.get(key));
    }
    if (b) {
      sb.deleteCharAt(1);
    }
    sb.append("}\r\n");
    byte[] line = sb.toString().getBytes();
    int i = 0;
    while (i < status_listeners.size()) {
      try {
        status_listeners.get(i).getOutputStream().write(line);
        i++;
      } catch (Exception e) {
        status_listeners.remove(i);
        Logger.getLogger(this.getClass()).error("Status listener error: ", e);
      }
    }
  }
}