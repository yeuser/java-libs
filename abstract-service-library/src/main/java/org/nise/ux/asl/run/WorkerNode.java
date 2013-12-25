package org.nise.ux.asl.run;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
import org.nise.ux.asl.run.ServiceServerImpl.ASLStructureHub;
import org.nise.ux.lib.Living;
import org.nise.ux.lib.RoundQueue;

class WorkerNode extends DistributerNode {
  private Map<String, Method>         commandMethodMap      = new HashMap<String, Method>();
  private Map<String, Method>         commandTestMethodMap  = new HashMap<String, Method>();
  private Map<String, List<Method>>   commandChainMethodMap = new HashMap<String, List<Method>>();
  private Worker                      worker;
  private String                      name;
  private InvokeAfterLivingController invokeAfterLivingController;

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
    invokeAfterLivingController = new InvokeAfterLivingController();
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
    Type[] input_args = method.getGenericParameterTypes();
    Object[] args = dataConnection.getRequestArgs(input_args);
    try {
      Object obj = method.invoke(worker, args);
      Logger.getLogger(WorkerNode.class).trace("@worker= " + name + " invoked {{" + method.toString() + "}} with args=" + args + " & returned obj=" + obj);
      try {
        dataConnection.send(new ServiceResponse(obj));
      } catch (Throwable t) {
        Logger.getLogger(WorkerNode.class).error(t.getMessage(), t);
      }
      invokeAfter(command, obj, null, args);
    } catch (InvocationTargetException ite) {
      Logger.getLogger(WorkerNode.class).error(ite.getMessage(), ite);
      try {
        dataConnection.send(new ServiceResponse(ite.getCause()));
      } catch (Throwable t2) {
        Logger.getLogger(WorkerNode.class).error(t2.getMessage(), t2);
      }
      invokeAfter(command, null, ite.getCause(), args);
    } catch (Throwable t) {
      Logger.getLogger(WorkerNode.class).error(t.getMessage(), t);
      try {
        dataConnection.send(new ServiceResponse(t));
      } catch (Throwable t2) {
        Logger.getLogger(WorkerNode.class).error(t2.getMessage(), t2);
      }
      invokeAfter(command, null, t, args);
    }
  }

  private void invokeAfter(String commandName, Object returnObject, Throwable throwable, Object... args) {
    while (!invokeAfterLivingController.push(new InvokeAfterData(commandName, returnObject, throwable, args))) {
    }
  }

  public String[] getCommandSet() {
    return commandMethodMap.keySet().toArray(new String[] {});
  }

  private class InvokeAfterLiving extends Living {
    public InvokeAfterLiving(int id) {
      super(id);
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
              Object obj = method.invoke(worker, _args);
              Logger.getLogger(WorkerNode.class).trace("@worker= " + name + " invoked {{" + method.toString() + "}} with args={" + invokeAfterData.returnObject + "," + invokeAfterData.throwable + "} & returned obj=" + obj);
              invokeAfter(commandChain.name(), obj, null, invokeAfterData.returnObject, invokeAfterData.throwable, invokeAfterData.args);
            } else {
              Object obj = method.invoke(worker, invokeAfterData.returnObject, invokeAfterData.throwable);
              Logger.getLogger(WorkerNode.class).trace("@worker= " + name + " invoked {{" + method.toString() + "}} with args={" + invokeAfterData.returnObject + "," + invokeAfterData.throwable + "} & returned obj=" + obj);
              invokeAfter(commandChain.name(), obj, null, invokeAfterData.returnObject, invokeAfterData.throwable);
            }
          } catch (InvocationTargetException ite) {
            Logger.getLogger(WorkerNode.class).error(ite.getMessage(), ite);
            invokeAfter(commandChain.name(), null, ite.getCause(), invokeAfterData.args);
          } catch (IllegalArgumentException ite) {
            Logger.getLogger(WorkerNode.class).error("" + method.getGenericParameterTypes().length, ite);
            invokeAfter(commandChain.name(), null, ite.getCause(), invokeAfterData.args);
          } catch (Throwable t) {
            Logger.getLogger(WorkerNode.class).error(t.getMessage(), t);
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

  class InvokeAfterLivingController extends Living implements QueueFace<InvokeAfterData> {
    private List<InvokeAfterLiving>     invokeAfterLivings        = new ArrayList<InvokeAfterLiving>();
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
    private int                         maximum_number_of_workers = 100;

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
      if (Logger.getLogger(InvokeAfterLivingController.class).isTraceEnabled()) {
        Logger.getLogger(InvokeAfterLivingController.class).trace( //
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
      if (Logger.getLogger(InvokeAfterLivingController.class).isDebugEnabled()) {
        Logger.getLogger(InvokeAfterLivingController.class).debug(//
            "Changing CountValue for " + name//
                + " from countValue=" + countFromValue//
                + " to countValue=" + countToValue//
                + " and queueMean=" + queueMean//
        );
      }
      refresh(countToValue);
    }

    public void refresh(int count) {
      if (getCommands().length > 0) {
        while (invokeAfterLivings.size() > count) {
          Logger.getLogger(ASLStructureHub.class).trace("Removed a HubWorker#" + name);
          invokeAfterLivings.remove(invokeAfterLivings.size() - 1).die();
        }
        while (invokeAfterLivings.size() < count) {
          Logger.getLogger(ASLStructureHub.class).trace("Created a HubWorker#" + name);
          invokeAfterLivings.add(new InvokeAfterLiving(invokeAfterLivings.size()));
        }
      } else {
        this.die();
      }
    }

    private void changeQueueMean() {
      int count = queue.getCount();
      long time = System.currentTimeMillis();
      double beta = (1 - alpha) * (time - lastPushPop) / (time - system_start_time);
      if (Logger.getLogger(InvokeAfterLivingController.class).isTraceEnabled()) {
        Logger.getLogger(InvokeAfterLivingController.class).trace(//
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
  }
}