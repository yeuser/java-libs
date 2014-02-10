package org.nise.ux.asl.run;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.DefaultValue;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.data.ServiceResponse;
import org.nise.ux.asl.face.DataConnection;
import org.nise.ux.asl.face.ServiceClient;
import org.nise.ux.asl.face.Worker;
import org.nise.ux.lib.Living;
import org.nise.ux.lib.RoundQueue;

import com.google.gson.Gson;

class WorkerNode extends DistributerNode {
  private static final Logger         LOGGER                = Logger.getLogger(WorkerNode.class);
  private final Gson                  GSON                  = new Gson();
  private Map<String, MethodInfo>     commandMethodMap      = new HashMap<String, MethodInfo>();
  private Map<String, MethodInfo>     commandTestMethodMap  = new HashMap<String, MethodInfo>();
  private Map<String, List<Method>>   commandChainMethodMap = new HashMap<String, List<Method>>();
  private Worker                      worker;
  private String                      name;
  private InvokeAfterLivingController invokeAfterLivingController;

  public WorkerNode(int id, QueueFace<DataConnection> inQueue, Worker worker, String name) {
    super(id, inQueue, name);
    this.name = name + "#" + id;
    for (Method method : worker.getClass().getMethods()) {
      if (method.isAnnotationPresent(MapCommand.class)) {
        LOGGER.debug("Annotation MapCommand in Method " + method + " : ");
        MapCommand methodAnno = method.getAnnotation(MapCommand.class);
        if (methodAnno.test()) {
          putMethodIntoMap(commandTestMethodMap, methodAnno.command(), method);
        } else {
          putMethodIntoMap(commandMethodMap, methodAnno.command(), method);
        }
      }
      if (method.isAnnotationPresent(CommandChain.class)) {
        LOGGER.debug("Annotation CommandChain in Method " + method + " : ");
        CommandChain methodAnno = method.getAnnotation(CommandChain.class);
        if (commandChainMethodMap.get(methodAnno.after()) == null)
          commandChainMethodMap.put(methodAnno.after(), new ArrayList<Method>());
        commandChainMethodMap.get(methodAnno.after()).add(method);
      }
    }
    invokeAfterLivingController = new InvokeAfterLivingController();
    this.worker = worker;
  }

  private void putMethodIntoMap(Map<String, MethodInfo> map, String command, Method method) {
    MethodInfo methodInfo = generateMethodInfo(method);
    map.put(command, methodInfo);
  }

  private MethodInfo generateMethodInfo(Method method) {
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    Type[] input_args = method.getGenericParameterTypes();
    List<Object> dvs = new ArrayList<Object>();
    for (int i = parameterAnnotations.length - 1; i >= 0; i--) {
      boolean has = false;
      for (int j = 0; j < parameterAnnotations[i].length; j++) {
        if (parameterAnnotations[i][j] instanceof DefaultValue) {
          dvs.add(GSON.fromJson(((DefaultValue) parameterAnnotations[i][j]).value(), input_args[i]));
          has = true;
        }
      }
      if (!has) {
        break;
      }
    }
    return new MethodInfo(method, input_args, dvs.toArray());
  }

  class MethodInfo {
    Method   method;
    Type[]   input_args;
    Object[] dvs;

    public MethodInfo(Method method, Type[] input_args, Object[] dvs) {
      this.method = method;
      this.input_args = input_args;
      this.dvs = dvs;
    }
  }

  @Override
  protected void handleDataConnection(DataConnection dataConnection) {
    String command = dataConnection.getCommand();
    MethodInfo methodInfo = commandMethodMap.get(command);
    if (methodInfo != null) {
      if (dataConnection.isHelp()) {
        help(dataConnection, command);
      } else {
        invoke(dataConnection, command);
      }
    } else {
      methodInfo = commandMethodMap.get(MapCommand.COMMAND_DEFAULT);
      if (methodInfo != null) {
        if (dataConnection.isHelp()) {
          help(dataConnection, command);
        } else {
          invoke(dataConnection, command);
        }
      } else {
        super.handleDataConnection(dataConnection);
      }
    }
  }

  private void help(DataConnection dataConnection, String command) {
    MethodInfo methodInfo = commandMethodMap.get(command);
    Object[] args = dataConnection.getRequestArgs(new Class[] { int.class }, new Object[] { null, });
    if (args[0] == null) {
      try {
        dataConnection.send(ServiceResponse.getThrowableResponse(new IllegalArgumentException("Help type is UnKnown!")));
      } catch (Throwable t) {
        LOGGER.error("@worker= " + name + " Error_message: " + t.getMessage(), t);
      }
    } else {
      switch ((Integer) args[0]) {
        case ServiceClient.HELP_DESCRIBE_COMMAND:
          Class<?> class1 = methodInfo.method.getReturnType();
          List<String> type_strings = new ArrayList<String>();
          type_strings.add(class1.getName());
          Class<?>[] classes = methodInfo.method.getParameterTypes();
          for (Class<?> class2 : classes) {
            type_strings.add(class2.getName());
          }
          try {
            dataConnection.send(ServiceResponse.getDataResponse(type_strings));
          } catch (Throwable t) {
            LOGGER.error("@worker= " + name + " Error_message: " + t.getMessage(), t);
          }
          break;
        default:
          try {
            dataConnection.send(ServiceResponse.getThrowableResponse(new IllegalArgumentException("Help type is UnKnown!")));
          } catch (Throwable t) {
            LOGGER.error("@worker= " + name + " Error_message: " + t.getMessage(), t);
          }
          break;
      }
    }
  }

  private void invoke(DataConnection dataConnection, String command) {
    try {
      MethodInfo methodInfo = commandMethodMap.get(command);
      Object[] args = dataConnection.getRequestArgs(methodInfo.input_args, methodInfo.dvs);
      try {
        Object obj = methodInvoker(methodInfo.method, args);
        LOGGER.trace("@worker= " + name + " invoked {{" + methodInfo.method.toString() + "}} with args=" + args + " & returned obj=" + obj);
        try {
          dataConnection.send(ServiceResponse.getDataResponse(obj));
        } catch (Throwable t) {
          LOGGER.error("@worker= " + name + " Error_message: " + t.getMessage(), t);
        }
        invokeAfter(command, obj, null, args);
      } catch (InvocationTargetException ite) {
        LOGGER.error("@worker= " + name + " Error_message: " + ite.getMessage(), ite);
        try {
          dataConnection.send(ServiceResponse.getThrowableResponse(ite.getCause()));
        } catch (Throwable t2) {
          LOGGER.error("@worker= " + name + " Error_message: " + t2.getMessage(), t2);
        }
        invokeAfter(command, null, ite.getCause(), args);
      } catch (Throwable t) {
        LOGGER.error("@worker= " + name + " Error_message: " + t.getMessage(), t);
        try {
          dataConnection.send(ServiceResponse.getThrowableResponse(t));
        } catch (Throwable t2) {
          LOGGER.error("@worker= " + name + " Error_message: " + t2.getMessage(), t2);
        }
        invokeAfter(command, null, t, args);
      }
    } catch (Throwable t) {
      LOGGER.error("@worker= " + name + " Error_message: " + t.getMessage(), t);
      try {
        dataConnection.send(ServiceResponse.getThrowableResponse(t));
      } catch (Throwable t2) {
        LOGGER.error("@worker= " + name + " Error_message: " + t2.getMessage(), t2);
      }
    }
  }

  private synchronized Object methodInvoker(Method method, Object... args) throws InvocationTargetException, IllegalAccessException {
    boolean hadError = false;
    while (true) {
      try {
        LOGGER.trace("@worker= " + name + " invoking {{" + method.toString() + "}} with args={" + args + "}");
        Object ret = method.invoke(worker, args);
        if (hadError) {
          LOGGER.error("@worker= " + name + " invoke succeeded {{" + method.toString() + "}} with args={" + GSON.toJson(args) + "}");
        }
        return ret;
      } catch (InvocationTargetException e) {
        throw e;
      } catch (IllegalAccessException e) {
        throw e;
      } catch (IllegalArgumentException e) {
        throw e;
      } catch (Throwable t) {
        hadError = true;
        if (method == null && args == null) {
          LOGGER.error("@worker= " + name + " invoking {method=null} with {args=null}");
          return null;
        }
        if (method == null) {
          LOGGER.error("@worker= " + name + " invoking {method=null} with args={" + args + "}");
          return null;
        }
        if (args == null) {
          LOGGER.error("@worker= " + name + " invoking {{" + method.toString() + "}} with {args=null}");
          return null;
        }
        LOGGER.error("@worker= " + name + " invoking {{" + method.toString() + "}} with args={" + GSON.toJson(args) + "}", t);
        Thread.yield();
      }
    }
  }

  private void invokeAfter(String commandName, Object returnObject, Throwable throwable, Object... args) {
    while (!invokeAfterLivingController.push(new InvokeAfterData(commandName, returnObject, throwable, args))) {
    }
  }

  public String[] getCommandSet() {
    return commandMethodMap.keySet().toArray(new String[] {});
  }

  @Override
  public void die() {
    invokeAfterLivingController.die();
    super.die();
  }

  private class InvokeAfterLiving extends Living {
    public InvokeAfterLiving(int id) {
      super(id, name);
    }

    private void _invokeAfter(InvokeAfterData invokeAfterData) {
      List<Method> methods = commandChainMethodMap.get(invokeAfterData.commandName);
      if (methods != null) {
        for (Method method : methods) {
          CommandChain commandChain = method.getAnnotation(CommandChain.class);
          try {
            if (commandChain.withInputs()) {
              Object[] _args = new Object[invokeAfterData.args.length + 2];
              _args[0] = invokeAfterData.returnObject;
              _args[1] = invokeAfterData.throwable;
              System.arraycopy(invokeAfterData.args, 0, _args, 2, invokeAfterData.args.length);
              Object obj = methodInvoker(method, _args);
              LOGGER.trace("@worker= " + name + " invoked {{" + method.toString() + "}} with args={" + invokeAfterData.returnObject + "," + invokeAfterData.throwable + "} & returned obj=" + obj);
              invokeAfter(commandChain.name(), obj, null, invokeAfterData.returnObject, invokeAfterData.throwable, invokeAfterData.args);
            } else {
              Object obj = methodInvoker(method, invokeAfterData.returnObject, invokeAfterData.throwable);
              LOGGER.trace("@worker= " + name + " invoked {{" + method.toString() + "}} with args={" + invokeAfterData.returnObject + "," + invokeAfterData.throwable + "} & returned obj=" + obj);
              invokeAfter(commandChain.name(), obj, null, invokeAfterData.returnObject, invokeAfterData.throwable);
            }
          } catch (InvocationTargetException ite) {
            LOGGER.error("@worker= " + name + " Error_message: " + ite.getMessage(), ite);
            invokeAfter(commandChain.name(), null, ite.getCause(), invokeAfterData.args);
          } catch (IllegalArgumentException ite) {
            LOGGER.error("" + method.getGenericParameterTypes().length, ite);
            invokeAfter(commandChain.name(), null, ite.getCause(), invokeAfterData.args);
          } catch (Throwable t) {
            LOGGER.error("@worker= " + name + " Error_message: " + t.getMessage(), t);
            invokeAfter(commandChain.name(), null, t, invokeAfterData.args);
          }
        }
      }
    }

    @Override
    protected void runtimeBehavior() throws Throwable {
      InvokeAfterData data = invokeAfterLivingController.pop();
      if (data != null) {
        _invokeAfter(data);
      }
    }
  }

  private class InvokeAfterData {
    String    commandName;
    Object    returnObject;
    Throwable throwable;
    Object[]  args;

    public InvokeAfterData(String commandName, Object returnObject, Throwable throwable, Object... args) {
      this.commandName = commandName;
      this.returnObject = returnObject;
      this.throwable = throwable;
      this.args = args;
    }
  }

  private class InvokeAfterLivingController extends Living implements QueueFace<InvokeAfterData> {
    private final Logger                LOGGER                    = Logger.getLogger(InvokeAfterLivingController.class);
    private List<InvokeAfterLiving>     invokeAfterLivings        = new ArrayList<InvokeAfterLiving>();
    private List<InvokeAfterLiving>     deadInvokeAfterLivings    = new ArrayList<InvokeAfterLiving>();
    private long                        lastPushPop;
    private double                      queueMean;
    private long                        system_start_time;
    private final static long           QUEUE_CHECK_DELAY         = 5 * /* 60 * */1000;
    private final static long           WARM_UP_PERIOD            = 10 * /* 60 * */1000;
    private final static double         alpha                     = 0.9;
    private final static double         inc_margin                = 0.8;
    private final static double         dec_margin                = 0.4;
    private final static double         inc_rate                  = 1.5;
    private final static double         dec_rate                  = 0.66667;
    private RoundQueue<InvokeAfterData> queue                     = new RoundQueue<InvokeAfterData>(100);
    private int                         minimum_number_of_workers = 1;
    private int                         maximum_number_of_workers = 10;

    public InvokeAfterLivingController() {
      super("QueueController#" + name);
      double rate = (dec_margin * (1 - alpha) + (inc_margin - dec_margin) * alpha);
      queueMean = queue.getLimit() * rate;
      refresh(minimum_number_of_workers);
    }

    @Override
    protected void init() {
      system_start_time = System.currentTimeMillis();
      try {
        Thread.sleep(WARM_UP_PERIOD);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      lastPushPop = System.currentTimeMillis();
      //system_start_time = lastPushPop - WARM_UP_PERIOD;
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace( //
            "Started ASL QueueControler for config: " + name//
                + " with queueMean=" + queueMean//
                + " and lastPushPop=" + lastPushPop//
                + " and system_start_time=" + system_start_time);
      }
    }

    @Override
    protected void runtimeBehavior() throws Throwable {
      try {
        Thread.sleep(Math.max(QUEUE_CHECK_DELAY, lastPushPop - System.currentTimeMillis()) + QUEUE_CHECK_DELAY);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (lastPushPop + QUEUE_CHECK_DELAY <= System.currentTimeMillis() + 5) { // use 5ms as margin of java sleep error
        if (queueMean < dec_margin * queue.getLimit()) {
          changeCountValue(dec_rate);
        }
      }
    }

    InvokeAfterData popFromQueue() {
      InvokeAfterData dp = queue.syncPop();
      changeQueueMean();
      return dp;
    }

    private void changeCountValue(double rate) {
      int countFromValue = invokeAfterLivings.size();
      int countToValue = (int) Math.round(countFromValue * rate);
      countToValue = Math.min(countToValue, Math.max((int) (queueMean * 3), maximum_number_of_workers));
      countToValue = Math.max(countToValue, minimum_number_of_workers);
      if (countFromValue == countToValue)
        return;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(//
            "Changing CountValue for " + name//
                + " from countValue=" + countFromValue//
                + " to countValue=" + countToValue//
                + " and queueMean=" + queueMean//
            );
      }
      //   refresh(countToValue);
    }

    public void refresh(int count) {
      if (getCommands().length > 0) {
        //        while (invokeAfterLivings.size() > count) {
        //          LOGGER.trace("Removed a HubWorker#" + name);
        //          invokeAfterLivings.remove(invokeAfterLivings.size() - 1).die();
        //        }
        //        while (invokeAfterLivings.size() < count) {
        //          LOGGER.trace("Created a HubWorker#" + name);
        //          invokeAfterLivings.add(new InvokeAfterLiving(invokeAfterLivings.size()));
        //        }
        if (invokeAfterLivings.size() > count) {
          int cnt_ = invokeAfterLivings.size();
          while (invokeAfterLivings.size() > count) {
            InvokeAfterLiving node = invokeAfterLivings.remove(invokeAfterLivings.size() - 1);
            node.die();
            deadInvokeAfterLivings.add(node);
          }
          LOGGER.trace("Updated Count [" + cnt_ + "->" + count + "] Removed " + (cnt_ - count) + " nodes type: HubWorker#" + name);
          while (deadInvokeAfterLivings.size() > 0) {
            synchronized (this) {
              for (int i = 0; i < deadInvokeAfterLivings.size(); i++) {
                invokeAfterLivingController.push(null);
              }
            }
            int i = 0;
            while (i < deadInvokeAfterLivings.size()) {
              if (deadInvokeAfterLivings.get(i).isRipped()) {
                deadInvokeAfterLivings.remove(i);
              } else {
                i++;
              }
            }
          }
        }
        if (invokeAfterLivings.size() < count) {
          int cnt_ = invokeAfterLivings.size();
          while (invokeAfterLivings.size() < count) {
            invokeAfterLivings.add(new InvokeAfterLiving(invokeAfterLivings.size()));
          }
          LOGGER.trace("Updated Count [" + cnt_ + "->" + count + "]Created " + (count - cnt_) + " nodes type: HubWorker#" + name);
        }
      }
    }

    private void changeQueueMean() {
      int count = queue.getCount();
      long time = System.currentTimeMillis();
      double beta = (1 - alpha) * (time - lastPushPop) / (time - system_start_time);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(//
            "Changing queueMean for " + name//
                + " from queueMean=" + queueMean//
                + " to queueMean=" + (queueMean * (1 - beta) + count * beta)//
                + " with (lastPushPop - system_start_time)=" + (lastPushPop - system_start_time)//
                + " and (time - system_start_time)=" + (time - system_start_time)//
                + " and alpha_time=" + beta//
                + " and count=" + count//
            );
      }
      queueMean = queueMean * alpha * beta + count * (1 - alpha * beta);
      lastPushPop = time;
    }

    @Override
    public InvokeAfterData pop() {
      return popFromQueue();
    }

    @Override
    public boolean push(InvokeAfterData invokeAfterData) {
      if (invokeAfterData == null)
        return this.queue.syncPush(null);
      if (!hasCommand(invokeAfterData.commandName))
        return true;
      if (queue.isFull()) {
        changeCountValue(1 + inc_rate);
      }
      boolean b = this.queue.syncPush(invokeAfterData);
      if (b) {
        changeQueueMean();
      } else {
        changeCountValue(1 + inc_rate);
      }
      if (queueMean > inc_margin * queue.getLimit()) {
        changeCountValue(inc_rate);
      }
      return b;
    }

    @Override
    public String[] getCommands() {
      return commandChainMethodMap.keySet().toArray(new String[] {});
    }

    @Override
    public boolean hasCommand(String command) {
      return commandChainMethodMap.get(command) != null;
    }

    @Override
    public void die() {
      refresh(0);
      super.die();
    }
  }
}