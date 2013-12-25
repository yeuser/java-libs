package org.nise.ux.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.nise.ux.lib.Living;

public class LiveConfigurations extends FileConfigurations implements Runnable {
  private static final String  OUTPUT_LIVE_PORT = "output_live_port";
  private static final String  STATUS_LIVE_PORT = "status_live_port";
  private static final String  CONFIG_LIVE_PORT = "config_live_port";
  private final StatusProfiler status;
  private final OutputProfiler output;

  public LiveConfigurations(int config_live_port, int status_live_port, int output_live_port, StatusInformer statusInformer, String config_conf, String[][] all_keys_defualts) {
    super(config_conf, all_keys_defualts, new String[][] {
        //
        { CONFIG_LIVE_PORT, String.valueOf(config_live_port) },//
        { STATUS_LIVE_PORT, String.valueOf(status_live_port) },//
        { OUTPUT_LIVE_PORT, String.valueOf(output_live_port) },//
    });
    status = new StatusProfiler(statusInformer);
    output = new OutputProfiler();
    System.setErr(new PrintStream(output));
    System.setOut(new PrintStream(output));
    final LiveConfigurations this_config = this;
    // Start Listening on CONFIG port.
    new Thread(this, "Live-Configuration-Thread").start();
    // Start Listening on STATUS writing/profiler port.
    new Thread("Live-Status-Thread") {
      @Override
      public void run() {
        ServerSocket sock = null;
        try {
          sock = new ServerSocket(this_config.getIntegerConfiguration(STATUS_LIVE_PORT)); // port to listen
          while (true) {
            Socket socket = sock.accept();
            // Add new status listener inside our status_profiler
            this_config.status.addListener(socket);
          }
        } catch (Exception e) {
          Logger.getLogger(this.getClass()).error("error!", e);
        }
        try {
          sock.close();
        } catch (IOException e) {
          Logger.getLogger(this.getClass()).error("error!", e);
        }
      }
    }.start();
    new Thread(new Runnable() {
      @Override
      public void run() {
        ServerSocket sock = null;
        try {
          sock = new ServerSocket(this_config.getIntegerConfiguration(OUTPUT_LIVE_PORT));
          while (true) {
            Socket statusDataPipe = sock.accept();
            // Add new status listener inside our status_profiler
            this_config.output.addListener(statusDataPipe);
          }
        } catch (Exception e) {
          Logger.getLogger(this.getClass()).error("error!", e);
        }
        try {
          sock.close();
        } catch (IOException e) {
          Logger.getLogger(this.getClass()).error("error!", e);
        }
      }
    }, "Live-Output-Thread").start();
  }

  /**
   * ?
   */
  public void run() {
    ServerSocket sock = null;
    try {
      sock = new ServerSocket(this.getIntegerConfiguration(CONFIG_LIVE_PORT));
      while (true) {
        Socket configDataPipe = sock.accept();
        // Make a new interpreter for this new administrator.
        new ConfigurationInterpreter(this, configDataPipe);
      }
    } catch (Exception e) {
      Logger.getLogger(this.getClass()).error("error!", e);
    }
    try {
      sock.close();
    } catch (IOException e) {
      Logger.getLogger(this.getClass()).error("error!", e);
    }
  }

  private class ConfigurationInterpreter extends Living {
    private BufferedReader     clientDataPipeReader;
    private LiveConfigurations serviceServerLiveConfigurations;
    private OutputStream       clientDataPipeWriter;

    private ConfigurationInterpreter(LiveConfigurations serviceServerLiveConfigurations, Socket configDataPipe) throws IOException {
      super("Configuration Interpreter");
      this.serviceServerLiveConfigurations = serviceServerLiveConfigurations;
      this.clientDataPipeReader = new BufferedReader(new InputStreamReader(configDataPipe.getInputStream()));
      this.clientDataPipeWriter = configDataPipe.getOutputStream();
    }

    @Override
    protected void runtimeBehavior() {
      try {
        final String str = clientDataPipeReader.readLine();
        if (str == null || str.startsWith("END")) {
          clientDataPipeReader.close();
          this.die();
          return;
        }
        if (str.startsWith("GET ALL CONFIGS")) {
          for (String key_row : this.serviceServerLiveConfigurations.getAllConfigurationKeys()) {
            String config = this.serviceServerLiveConfigurations.getConfiguration(key_row);
            this.clientDataPipeWriter.write((key_row + ": " + config + "\n\r").getBytes());
          }
          this.clientDataPipeWriter.flush();
        } else if (str.startsWith("GET ALL DEFUALTS")) {
          for (String[] key_row : this.serviceServerLiveConfigurations.ALL_KEYS_DEFUALTS) {
            this.clientDataPipeWriter.write((key_row[0] + ": " + key_row[1] + ": " + key_row[2] + "\n\r").getBytes());
          }
          this.clientDataPipeWriter.flush();
        } else if (str.startsWith("GET ALL ")) {
          this.clientDataPipeWriter.write(("Possible <GET ALL> commands are {GET ALL CONFIGS} & {GET ALL DEFUALTS} ...\n\r").getBytes());
          this.clientDataPipeWriter.flush();
        } else if (str.startsWith("GET ")) {
          String key = str.substring(4).trim();
          String config = this.serviceServerLiveConfigurations.getConfiguration(key);
          this.clientDataPipeWriter.write((key + ": " + config + "\n\r").getBytes());
          this.clientDataPipeWriter.flush();
        } else if (str.startsWith("SET ")) {
          String key = str.substring(4, str.indexOf('=')).trim();
          String config = str.substring(str.indexOf('=') + 1).trim();
          if (this.serviceServerLiveConfigurations.getConfiguration(key) == null) {
            this.clientDataPipeWriter.write(("Specified config cannot be found.\n\r").getBytes());
            this.clientDataPipeWriter.flush();
          } else {
            this.serviceServerLiveConfigurations.setConfiguration(key, config);
            this.clientDataPipeWriter.write((key + ": " + config + "\n\r").getBytes());
            this.clientDataPipeWriter.flush();
          }
        } else if (str.equalsIgnoreCase("help")) {
          this.clientDataPipeWriter.write(("Possible commands are:\n\r").getBytes());
          this.clientDataPipeWriter.write(("\tGET ALL CONFIGS: lists all ServiceServerLiveConfigurations set,\n\r").getBytes());
          this.clientDataPipeWriter.write(("\tGET ALL DEFUALTS: list all code implemented defaults,\n\r").getBytes());
          this.clientDataPipeWriter.write(("\tGET {config}: shows configuration specified,\n\r").getBytes());
          this.clientDataPipeWriter.write(("\tSET {config}={new configuration}: sets specified configuration to the new value,\n\r").getBytes());
          this.clientDataPipeWriter.write(("\thelp: prints out this message,\n\r").getBytes());
          this.clientDataPipeWriter.write(("\tRESET: reloads all ServiceServerLiveConfigurations from 'config.txt' file,\n\r").getBytes());
          this.clientDataPipeWriter.write(("\tSTORE CONFIGS: saves all Configurations in system run to 'config.txt' file,\n\r").getBytes());
          this.clientDataPipeWriter.write(("\tEND: closes this connection,\n\r").getBytes());
          this.clientDataPipeWriter.flush();
        } else if (str.startsWith("RESET")) {
          this.serviceServerLiveConfigurations.reload();
        } else if (str.startsWith("STORE CONFIGS")) {
          this.serviceServerLiveConfigurations.storeConfigurations();
        } else {
          this.clientDataPipeWriter.write(("Wrong Command...\n\r\tYou can type 'help' for a list of commands.\n\r").getBytes());
          this.clientDataPipeWriter.flush();
        }
      } catch (IOException e) {
        e.printStackTrace(new PrintWriter(this.clientDataPipeWriter));
        try {
          clientDataPipeReader.close();
          this.die();
        } catch (IOException e1) {
          this.die();
        }
      }
    }
  }
}