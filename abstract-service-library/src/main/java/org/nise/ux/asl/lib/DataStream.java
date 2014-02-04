package org.nise.ux.asl.lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.security.InvalidParameterException;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.ServiceResponse;
import org.nise.ux.asl.face.DataConnection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

//VERSION 0//{JSON}
//VERSION 1//{INDEX} {JSON}
//VERSION 2//-2 {VERSION==2} {INDEX} {COMMAND} {JSON}
public class DataStream implements Closeable {
  private Socket         socket;
  private BufferedWriter writer = null;
  private BufferedReader reader = null;
  private boolean        closed = false;

  public DataStream(String host, int port) throws IOException {
    Socket socket = new Socket(host, port);
    init(socket);
  }

  public DataStream(Socket socket) throws IOException {
    init(socket);
  }

  private void init(Socket socket) throws IOException {
    this.socket = socket;
    InputStream inStream = socket.getInputStream();
    OutputStream outStream = socket.getOutputStream();
    reader = new BufferedReader(new InputStreamReader(inStream));
    writer = new BufferedWriter(new OutputStreamWriter(outStream));
  }

  //VERSION 2//-2 {VERSION==2} {INDEX} {COMMAND} {JSON}
  public void sendCommand(int index, String command, Object... data) throws IOException {
    if (closed)
      throw new ClosedChannelException();
    StringBuilder sb = new StringBuilder();
    sb.append("-2 2 ");
    sb.append(index);
    sb.append(' ');
    sb.append(command);
    sb.append(' ');
    String[] json_reps = new String[data.length];
    for (int i = 0; i < json_reps.length; i++) {
      json_reps[i] = new Gson().toJson(data[i]);
    }
    String line = new Gson().toJson(json_reps);
    sb.append(line);
    Logger.getLogger(DataStream.class).debug("Sent: " + sb.toString());
    sb.append("\r\n");
    writer.append(sb);
    writer.flush();
  }

  public void sendReturn(int index, Object returnData) throws IOException {
    send(ServiceResponse.getDataResponse(returnData), index);
  }

  public void sendReturn(int index, Throwable throwable) throws IOException {
    send(ServiceResponse.getThrowableResponse(throwable), index);
  }

  public IndexedData<ServiceResponse> recieveReturn() throws IOException {
    return recieve(ServiceResponse.class);
  }

  public void send(Object data, int index) throws IOException {
    if (closed)
      throw new ClosedChannelException();
    StringBuilder sb = new StringBuilder();
    if (index >= 0) {
      //VERSION 1//{INDEX} {JSON}
      sb.append(index);
      sb.append(' ');
    } else {
      //VERSION 0//{JSON}
    }
    String line = new Gson().toJson(data);
    sb.append(line);
    Logger.getLogger(DataStream.class).debug("Sent: " + sb.toString());
    sb.append("\r\n");
    writer.append(sb);
    writer.flush();
  }

  public DataConnection extractCommand() throws IOException {
    if (closed)
      throw new ClosedChannelException();
    String line = reader.readLine();
    if (line == null) {
      this.close();
      throw new ClosedChannelException();
    }
    Logger.getLogger(DataStream.class).debug("Received: " + line);
    int order = -1;
    if (line.charAt(0) == '{') {
      // OLD Version
      //VERSION 0//{JSON}
      return new DataConnectionImpl(this, null, line, -1);
    } else {
      int i = line.indexOf(' ');
      if (i == -1) {
        this.close();
        throw new ClosedChannelException();
      }
      String intPart = line.substring(0, i);
      order = Integer.parseInt(intPart);
      line = line.substring(i + 1).trim();
      if (order == -2) {
        //VERSION 2//-2 {VERSION} ...
        i = line.indexOf(' ');
        if (i == -1) {
          this.close();
          throw new ClosedChannelException();
        }
        String versionPart = line.substring(0, i);
        line = line.substring(i + 1).trim();
        int version = Integer.parseInt(versionPart);
        if (version == 2) {
          //VERSION 2//-2 {VERSION==2} {INDEX} {COMMAND} {JSON}
          i = line.indexOf(' ');
          if (i == -1) {
            this.close();
            throw new ClosedChannelException();
          }
          String indexPart = line.substring(0, i);
          line = line.substring(i + 1).trim();
          i = line.indexOf(' ');
          if (i == -1) {
            this.close();
            throw new ClosedChannelException();
          }
          String commandPart = line.substring(0, i);
          line = line.substring(i + 1).trim();
          return new DataConnectionImpl(this, commandPart, line, Integer.parseInt(indexPart));
        }
        //UNKNOWN VERSION
        return null;
      } else {
        //VERSION 1//{INDEX} {JSON}
        return new DataConnectionImpl(this, null, line, order);
      }
    }
  }

  public <RD> IndexedData<RD> recieve(TypeToken<RD> type4R) throws IOException {
    if (closed)
      throw new ClosedChannelException();
    String line = reader.readLine();
    if (line == null) {
      this.close();
      throw new ClosedChannelException();
    }
    Logger.getLogger(DataStream.class).debug("Received: " + line);
    int order = -1;
    if (line.charAt(0) == '{') {
      // OLD Version
    } else {
      int i = line.indexOf(' ');
      if (i == -1) {
        this.close();
        throw new ClosedChannelException();
      }
      String intPart = line.substring(0, i);
      try {
        order = Integer.parseInt(intPart);
        line = line.substring(i + 1);
      } catch (NumberFormatException nfe) {
        Logger.getLogger(DataStream.class).error("NumberFormatException: " + intPart);
      }
    }
    RD response = new Gson().fromJson(line, type4R.getType());
    return new IndexedData<RD>(response, order);
  }

  public <RD> IndexedData<RD> recieve(Class<RD> type4R) throws IOException {
    if (closed)
      throw new ClosedChannelException();
    String line = reader.readLine();
    if (line == null) {
      this.close();
      throw new ClosedChannelException();
    }
    Logger.getLogger(DataStream.class).debug("Received: " + line);
    int order = -1;
    if (line.charAt(0) == '{') {
      // OLD Version
    } else {
      int i = line.indexOf(' ');
      if (i == -1) {
        this.close();
        throw new ClosedChannelException();
      }
      String intPart = line.substring(0, i);
      try {
        order = Integer.parseInt(intPart);
        line = line.substring(i + 1);
      } catch (NumberFormatException nfe) {
        Logger.getLogger(DataStream.class).error("NumberFormatException: " + intPart);
      }
    }
    RD response = new Gson().fromJson(line, type4R);
    return new IndexedData<RD>(response, order);
  }

  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() throws IOException {
    closed = true;
    reader.close();
    writer.close();
    socket.close();
  }

  class DataConnectionImpl implements Cloneable, DataConnection {
    private final String     json_data;
    private final String     command;
    private final DataStream dataStream;
    private final int        index;

    public DataConnectionImpl(DataStream dataStream, String command, String json_data, int index) {
      this.dataStream = dataStream;
      this.json_data = json_data;
      this.command = command;
      this.index = index;
    }

    @Override
    public DataConnection clone() {
      return new DataConnectionImpl(dataStream, new String(command), new String(json_data), index);
    }

    @Override
    public String getCommand() {
      return command;
    }

    public String getRequest() {
      return json_data;
    }

    @Override
    public int getIndex() {
      return index;
    }

    @Override
    public <SD> void send(SD data) throws Exception {
      Logger.getLogger(DataConnectionImpl.class).trace("Sending Data=" + new Gson().toJson(data));
      dataStream.send(data, index);
    }

    private <RD> RD getRequestArg(String json_data, Type requestType) {
      try {
        return new Gson().fromJson(json_data, requestType);
      } catch (Exception e) {
        InvalidParameterException invalidParameterException = new InvalidParameterException("Expected " + requestType.toString() + " but got {" + json_data + "}");
        invalidParameterException.initCause(e);
        throw invalidParameterException;
      }
    }

    private <RD> RD getRequestArg(String json_data, Class<RD> requestClass) {
      try {
        return new Gson().fromJson(json_data, requestClass);
      } catch (Exception e) {
        InvalidParameterException invalidParameterException = new InvalidParameterException("Expected " + requestClass.toString() + " but got {" + json_data + "}");
        invalidParameterException.initCause(e);
        throw invalidParameterException;
      }
    }

    @Override
    public Object[] getRequestArgs(Type[] requestTypes) {
      String[] json_datas = new Gson().fromJson(json_data, new TypeToken<String[]>() {
      }.getType());
      if (requestTypes.length != json_datas.length) {
        return null;
      }
      Object[] args = new Object[json_datas.length];
      for (int i = 0; i < args.length; i++) {
        args[i] = getRequestArg(json_datas[i], requestTypes[i]);
      }
      return args;
    }

    @Override
    public Object[] getRequestArgs(Class<?>[] requestClasses) {
      String[] json_datas = new Gson().fromJson(json_data, new TypeToken<String[]>() {
      }.getType());
      if (requestClasses.length != json_datas.length) {
        return null;
      }
      Object[] args = new Object[json_datas.length];
      for (int i = 0; i < args.length; i++) {
        args[i] = getRequestArg(json_datas[i], requestClasses[i]);
      }
      return args;
    }

    @Override
    public String toString() {
      return "DataStream=@(" + dataStream + ") command=" + command + " index=" + index + " json_data=" + json_data;
    }
  }
}