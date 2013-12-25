package org.nise.ux.configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;

class OutputProfiler extends OutputStream {
  private Vector<Socket> output_listeners = new Vector<Socket>();
  private PrintStream    sysout           = System.out;

  OutputProfiler() {
  }

  public void addListener(Socket statusDataPipe) {
    output_listeners.add(statusDataPipe);
    Logger.getLogger(this.getClass()).info("Output listener added.");
  }

  @Override
  public void write(int b) throws IOException {
    sysout.write(b);
    int i = 0;
    while (i < output_listeners.size()) {
      try {
        output_listeners.elementAt(i).getOutputStream().write(b);
        i++;
      } catch (Exception e) {
        output_listeners.remove(i);
        Logger.getLogger(this.getClass()).error("Output listener error: ", e);
      }
    }
  }
}