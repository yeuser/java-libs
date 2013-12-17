package org.nise.ux.asl.face;

import java.lang.reflect.Type;

public interface DataConnection {
  public String getCommand();

  public int getIndex();

  public <SD> void send(SD data) throws Exception;

  public Object[] getRequestArgs(Type[] requestTypes);

  public Object[] getRequestArgs(Class<?>[] requestClasses);
}