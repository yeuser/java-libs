package org.nise.ux.asl.run;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.data.ServiceResponse;
import org.nise.ux.asl.face.DataConnection;
import org.nise.ux.asl.face.Worker;

class WorkerNode extends DistributerNode {
  private Map<String, Method>       commandMethodMap      = new HashMap<String, Method>();
  private Map<String, Method>       commandTestMethodMap  = new HashMap<String, Method>();
  private Map<String, List<Method>> commandChainMethodMap = new HashMap<String, List<Method>>();
  private Worker                    worker;
  private String                    name;

  public WorkerNode(int id, QueueFace<DataConnection> inQueue, Worker worker, String name) {
    super(id, inQueue, name);
    this.name = name;
    for (Method method : worker.getClass().getMethods()) {
      if (method.isAnnotationPresent(MapCommand.class)) {
        Logger.getLogger(WorkerNode.class).debug("Annotation MapCommand in Method " + method + " : ");
        MapCommand methodAnno = method.getAnnotation(MapCommand.class);
        if (methodAnno.test()) {
          commandTestMethodMap.put(methodAnno.command(), method);
        } else {
          commandMethodMap.put(methodAnno.command(), method);
        }
      }
      if (method.isAnnotationPresent(CommandChain.class)) {
        Logger.getLogger(WorkerNode.class).debug("Annotation CommandChain in Method " + method + " : ");
        CommandChain methodAnno = method.getAnnotation(CommandChain.class);
        if (commandChainMethodMap.get(methodAnno.after()) == null)
          commandChainMethodMap.put(methodAnno.after(), new ArrayList<Method>());
        commandChainMethodMap.get(methodAnno.after()).add(method);
      }
    }
    this.worker = worker;
  }

  @Override
  protected void handleDataConnection(DataConnection dataConnection) {
    String command = dataConnection.getCommand();
    Method method = commandMethodMap.get(command);
    if (method != null) {
      invoke(dataConnection, command);
    } else {
      method = commandMethodMap.get(MapCommand.COMMAND_DEFAULT);
      if (method != null) {
        invoke(dataConnection, command);
      } else {
        super.handleDataConnection(dataConnection);
      }
    }
  }

  private void invoke(DataConnection dataConnection, String command) {
    Method method = commandMethodMap.get(command);
    Class<?>[] input_args = method.getParameterTypes();
    Object[] args = dataConnection.getRequestArgs(input_args);
    try {
      Object obj = method.invoke(worker, args);
      Logger.getLogger(WorkerNode.class).trace("@worker= " + name + " invoked {{" + method.toString() + "}} with args=" + args + " & returned obj=" + obj);
      try {
        dataConnection.send(new ServiceResponse(obj));
      } catch (Throwable t) {
        Logger.getLogger(WorkerNode.class).error(t.getMessage(), t);
      }
      invokeAfter(command, obj, null);
    } catch (Throwable t) {
      Logger.getLogger(WorkerNode.class).error(t.getMessage(), t);
      try {
        dataConnection.send(new ServiceResponse(t));
      } catch (Throwable t2) {
        Logger.getLogger(WorkerNode.class).error(t2.getMessage(), t2);
      }
      invokeAfter(command, null, t);
    }
  }

  private void invokeAfter(String commandName, Object returnObject, Throwable throwable) {
    List<Method> methods = commandChainMethodMap.get(commandName);
    if (methods != null) {
      for (Method method : methods) {
        CommandChain methodAnno = method.getAnnotation(CommandChain.class);
        try {
          Object obj = method.invoke(worker, returnObject, throwable);
          Logger.getLogger(WorkerNode.class).trace("@worker= " + name + " invoked {{" + method.toString() + "}} with args={" + returnObject + "," + throwable + "} & returned obj=" + obj);
          invokeAfter(methodAnno.name(), obj, null);
        } catch (Throwable t) {
          Logger.getLogger(WorkerNode.class).error(t.getMessage(), t);
          invokeAfter(methodAnno.name(), null, t);
        }
      }
    }
  }

  public String[] getCommandSet() {
    return commandMethodMap.keySet().toArray(new String[] {});
  }
}