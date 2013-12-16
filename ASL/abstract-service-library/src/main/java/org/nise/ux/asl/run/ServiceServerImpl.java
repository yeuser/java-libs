package org.nise.ux.asl.run;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.ChainException;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.face.DataConnection;
import org.nise.ux.asl.face.ServiceServerMXBean;
import org.nise.ux.asl.face.WorkerFactory;
import org.nise.ux.asl.lib.DataStream;
import org.nise.ux.lib.RoundQueue;

public class ServiceServerImpl extends ServiceServerAbstract {
  private static final String                       ASL_STRUCTURE_HUB          = "ASLStructureHub_";
  private static final String                       ASL_STRUCTURE_HUB_CONSUMER = "ASLStructureHub_Consumer_";
  private static final String                       _NO                        = "_No";
  protected Map<String, RoundQueue<DataConnection>> Worker_iQueues             = new HashMap<String, RoundQueue<DataConnection>>();
  protected Map<String, ASLStructureHub>            workersListMap             = new HashMap<String, ASLStructureHub>();
  protected List<String>                            workersNameList            = new ArrayList<String>();
  protected List<ListenerNode>                      aslListeners               = new ArrayList<ListenerNode>();
  protected List<RequestFetcherNode>                aslRequestFetchers         = new ArrayList<RequestFetcherNode>();
  private final RoundQueue<DataConnection>          rootWorkerQueue;
  private final String                              rootWorkerName;
  private RoundQueue<DataStream>                    aslRequestFetcherQueue;
  private int[]                                     fetcherNo                  = { 1, 1000 };
  private int                                       workerId                   = 1;
  private int                                       distributerId              = 1;
  private boolean                                   stopped                    = false;

  public ServiceServerImpl(List<WorkersTreeDecriptor> firstLayerNodes) throws ChainException {
    this(firstLayerNodes, new HashMap<String, String>());
  }

  public ServiceServerImpl(List<WorkersTreeDecriptor> firstLayerNodes, Map<String, String> configurationSet) throws ChainException {
    super(configurationSet);
    List<WorkersTreeDecriptor> l1Nodes = new ArrayList<WorkersTreeDecriptor>();
    for (WorkersTreeDecriptor workersTreeDecriptor : firstLayerNodes) {
      WorkersTreeDecriptor node = checkWorkerFactoryNode(workersTreeDecriptor);
      if (node != null) {
        l1Nodes.add(node);
      }
    }
    if (l1Nodes.size() <= 0) {
      throw new ChainException("No Worker Node could be reached.");
    } else if (l1Nodes.size() == 1) {
      rootWorkerQueue = initWorkerList(null, l1Nodes.get(0));
      rootWorkerName = l1Nodes.get(0).getName();
    } else {
      String distrName = ASL_STRUCTURE_HUB + distributerId++;
      rootWorkerQueue = createWorkerQueue(distrName);
      rootWorkerName = distrName;
      ASLStructureHub listASLDistributer = new ASLStructureHub(null, distrName, new int[] { 1, 1000 }, rootWorkerQueue);
      //      DistributerNode routeNode = new DistributerNode(distributerId++, Worker_iQueues.get(distrName));
      addWorkerCompany(distrName, listASLDistributer);
      for (WorkersTreeDecriptor node : l1Nodes) {
        RoundQueue<DataConnection> childWorkerQueue = initWorkerList(listASLDistributer, node);
        listASLDistributer.addNextNodeHub(childWorkerQueue);
        System.out.println(distrName + " > " + node.getName());
      }
    }
  }

  private WorkersTreeDecriptor checkWorkerFactoryNode(WorkersTreeDecriptor workersTreeDecriptor) {
    List<WorkersTreeDecriptor> next = workersTreeDecriptor.getNextSet();
    List<WorkersTreeDecriptor> nextSet = new ArrayList<WorkersTreeDecriptor>();
    for (WorkersTreeDecriptor child : next) {
      if (child != null) {
        nextSet.add(child);
      }
    }
    WorkerFactory workerFactory = workersTreeDecriptor.getWorkerFactory();
    if (nextSet.size() == 0) {
      if (workerFactory == null) {
        return null;
      }
      return workersTreeDecriptor;
    }
    if (workerFactory == null && nextSet.size() == 1) {
      return checkWorkerFactoryNode(nextSet.get(0));
    }
    next.clear();
    for (WorkersTreeDecriptor child : nextSet) {
      WorkersTreeDecriptor _child = checkWorkerFactoryNode(child);
      if (_child != null) {
        next.add(_child);
      }
    }
    return workersTreeDecriptor;
  }

  private RoundQueue<DataConnection> initWorkerList(ASLStructureHub parent, WorkersTreeDecriptor workersTreeDecriptor) {
    String workerName = workersTreeDecriptor.getName();
    WorkerFactory workerFactory = workersTreeDecriptor.getWorkerFactory();
    List<WorkersTreeDecriptor> workerList = workersTreeDecriptor.getNextSet();
    RoundQueue<DataConnection> workerQueue = createWorkerQueue(workerName);
    int[] range = workersTreeDecriptor.getRange();
    ASLStructureHub aslNode;
    if (workerFactory == null) {
      aslNode = new ASLStructureHub(parent, workerName, range, workerQueue);
    } else {
      aslNode = new ASLStructureHub(parent, workerName, range, workerFactory, workerQueue);
    }
    addWorkerCompany(workerName, aslNode);
    if (workerList.size() > 1) {
      String distrName = ASL_STRUCTURE_HUB + distributerId++;
      RoundQueue<DataConnection> aslMiddleNodeQueue = createWorkerQueue(distrName);
      aslNode.addNextNodeHub(aslMiddleNodeQueue);
      ASLStructureHub aslMiddleNode = new ASLStructureHub(aslNode, distrName, range, aslMiddleNodeQueue);
      System.out.println(workerName + " > " + distrName);
      addWorkerCompany(distrName, aslMiddleNode);
      for (WorkersTreeDecriptor node : workerList) {
        RoundQueue<DataConnection> childWorkerQueue = initWorkerList(aslMiddleNode, node);
        aslMiddleNode.addNextNodeHub(childWorkerQueue);
        System.out.println(distrName + " > " + node.getName());
      }
    } else {
      if (workerList.size() == 1) {
        RoundQueue<DataConnection> childWorkerQueue = initWorkerList(aslNode, workerList.get(0));
        aslNode.addNextNodeHub(childWorkerQueue);
        System.out.println(workerName + " > " + workerList.get(0).getName());
      }
    }
    return workerQueue;
  }

  private void addWorkerCompany(String workerName, ASLStructureHub aslNode) {
    workersNameList.add(workerName);
    workersListMap.put(workerName, aslNode);
  }

  private RoundQueue<DataConnection> createWorkerQueue(String workerName) {
    String CONST_I_WORKER_QUEUE_LENGTH = "CONST_I_" + workerName + "_QUEUE_LENGTH";
    int i_worker_queue_length = Integer.parseInt(validateConfiguration(CONST_I_WORKER_QUEUE_LENGTH, "100"));
    RoundQueue<DataConnection> queue = new RoundQueue<DataConnection>(i_worker_queue_length);
    Worker_iQueues.put(workerName, queue);
    return queue;
  }

  @Override
  protected void start() {
    Logger.getLogger(ServiceServerImpl.class).info("RequestFetcherNode > " + rootWorkerName);
    Logger.getLogger(ServiceServerImpl.class).info("ListenerNode > RequestFetcherNode");
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName(ServiceServerImpl.class.getPackage().getName() + ":type=" + ServiceServerImpl.class.getSimpleName());
      ServiceServerMXBean mbean = this;
      mbs.registerMBean(mbean, name);
      Logger.getLogger(ServiceServerImpl.class).info("ServiceServerMXBean successfully registered.");
    } catch (Throwable t) {
      Logger.getLogger(ServiceServerImpl.class).error("Problem: registering ServiceServerMXBean successfully.", t);
    }
    refreshAll();
    Logger.getLogger(ServiceServerImpl.class).trace("Created a ListenerNode");
    String workerName = "ASL_REQUEST_FETCHER";
    String CONST_I_WORKER_QUEUE_LENGTH = "CONST_I_" + workerName + "_QUEUE_LENGTH";
    int i_worker_queue_length = Integer.parseInt(validateConfiguration(CONST_I_WORKER_QUEUE_LENGTH, "100"));
    aslRequestFetcherQueue = new RoundQueue<DataStream>(i_worker_queue_length);
    for (int i = 1; i <= fetcherNo[0]; i++) {
      RequestFetcherNode aslRequestFetcher = new RequestFetcherNode(i, aslRequestFetcherQueue);
      aslRequestFetcher.addConsumer(rootWorkerQueue);
      aslRequestFetchers.add(aslRequestFetcher);
    }
    aslListeners.add(new ListenerNode(getListeningPort(), 1, this, aslRequestFetcherQueue));
  }

  private String validateConfiguration(String key, String value) {
    String val = getConfiguration(key);
    if (val != null)
      return val;
    setConfiguration(key, value);
    return value;
  }

  /**
   * Re-Constructs or Destroys ServiceServer Threads based on numbers defined in configuration parameters
   */
  @Override
  public void refreshAll() {
    for (String workersName : workersNameList) {
      workersListMap.get(workersName).refreshWorkers();
    }
  }

  /**
   * Re-Constructs or Destroys ServiceServer Threads of given configuration parameter.
   * 
   * @param key
   *          name of configuration. This parameter can be one of values below:
   * @param config
   *          value of configuration
   */
  @Override
  public void refreshConfig(String key, String config) {
    if (key.startsWith(ASL_STRUCTURE_HUB_CONSUMER) && key.endsWith(_NO)) {
      String workerName = key.substring(ASL_STRUCTURE_HUB_CONSUMER.length(), key.length() - _NO.length());
      workersListMap.get(workerName).refreshWorkers();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.nise.ux.asl.run.ServiceServer#getAllStats()
   */
  @Override
  public Map<String, String> getAllStats() {
    return getAllStats(null);
  }

  /**
   * Gives all system status in a Map of "{prefix}{Status name}" -> "{Status value}"
   * 
   * @return all system status
   */
  @Override
  public Map<String, String> getAllStats(String prefix) {
    if (prefix == null) {
      prefix = "";
    } else {
      prefix = prefix.replaceAll("\\s+", " ").trim() + " ";
    }
    HashMap<String, String> stats = new HashMap<String, String>();
    stats.put(prefix + "Clients", String.valueOf(this.getClients()));
    for (String workersName : workersNameList) {
      stats.put(prefix + workersName + "_Consumers_No", String.valueOf(workersListMap.get(workersName).getWorkerNo()));
      stats.put(prefix + workersName + "_iQueue_Length", String.valueOf(Worker_iQueues.get(workersName).getCount()));
    }
    stats.put(prefix + "Current InSystem", String.valueOf(this.getInSystem()));
    return stats;
  }

  @Override
  public int getWorkerNo(String worker) {
    ASLStructureHub aslStructureHub = workersListMap.get(worker);
    if (aslStructureHub == null)
      return 0;
    return aslStructureHub.getWorkerNo();
  }

  @Override
  public String[] getWorkerNames() {
    return workersNameList.toArray(new String[] {});
  }

  class ASLStructureHub {
    private final List<DistributerNode>           aSLHubWorkers       = new ArrayList<DistributerNode>();
    private final List<QueueFace<DataConnection>> nextNodeWorkerQueue = new ArrayList<QueueFace<DataConnection>>();
    private final List<String>                    possibleCommands    = new ArrayList<String>();
    private final WorkerFactory                   workerFactory;
    private final String                          configurationName;
    private int                                   consumerNo;
    private final QueueFace<DataConnection>       aslQueue;
    private final ASLStructureHub                 parent;
    private final String                          name;

    public ASLStructureHub(ASLStructureHub parent, String name, int[] range, RoundQueue<DataConnection> inQueue) {
      this(parent, name, range, null, inQueue);
    }

    public ASLStructureHub(ASLStructureHub parent, String name, int[] range, WorkerFactory workerFactory, RoundQueue<DataConnection> inQueue) {
      this.parent = parent;
      this.name = name;
      this.workerFactory = workerFactory;
      this.configurationName = ASL_STRUCTURE_HUB_CONSUMER + name + _NO;
      this.consumerNo = Integer.parseInt(validateConfiguration(configurationName, String.valueOf(range[0])));
      if (range == null || range.length == 0) {
        range = new int[] { 1, 1000 };
      }
      if (range.length > 1) {
        int minimum_number_of_workers = range[0];
        int maximum_number_of_workers = range[1];
        this.aslQueue = new QueueController(this, inQueue, configurationName, maximum_number_of_workers, minimum_number_of_workers);
      } else {
        this.aslQueue = new RoundQueue2QueueFace(this, inQueue);
      }
    }

    public void addNextNodeHub(RoundQueue<DataConnection> consumerQueue) {
      nextNodeWorkerQueue.add(new RoundQueue2QueueFace(this, consumerQueue));
    }

    public void addHubWorker() {
      DistributerNode aslDistributer;
      if (workerFactory == null) {
        aslDistributer = new DistributerNode(distributerId++, aslQueue, name);
      } else {
        WorkerNode aslWorkerNode = new WorkerNode(workerId++, aslQueue, workerFactory.getWorker(), name);
        for (String command : aslWorkerNode.getCommandSet()) {
          addCommand(command);
        }
        aslDistributer = aslWorkerNode;
      }
      aslDistributer.addConsumers(nextNodeWorkerQueue);
      aSLHubWorkers.add(aslDistributer);
    }

    private void addCommand(String command) {
      if (Logger.getLogger(ServiceServerImpl.class).isTraceEnabled()) {
        Logger.getLogger(ServiceServerImpl.class).trace(//
            "Worker: " + name + " addCommand on: " + command + "@" + possibleCommands);
      }
      if (!possibleCommands.contains(command)) {
        possibleCommands.add(command);
        if (parent != null) {
          parent.addCommand(command);
        }
      }
    }

    private boolean hasCommand(String command) {
      if (Logger.getLogger(ServiceServerImpl.class).isTraceEnabled()) {
        Logger.getLogger(ServiceServerImpl.class).trace(//
            "Worker: " + name + " hasCommand on: " + command + "@" + possibleCommands);
      }
      if (possibleCommands.contains(command)) {
        return true;
      }
      if (possibleCommands.contains(MapCommand.COMMAND_DEFAULT)) {
        return true;
      }
      return false;
    }

    public void removeDistributer() {
      aSLHubWorkers.remove(aSLHubWorkers.size() - 1).die();
    }

    public void refreshWorkers() {
      while (aSLHubWorkers.size() > getWorkerNo()) {
        Logger.getLogger(ASLStructureHub.class).trace("Removed a ASLHubConsumer");
        removeDistributer();
      }
      while (aSLHubWorkers.size() < getWorkerNo()) {
        Logger.getLogger(ASLStructureHub.class).trace("Created a ASLHubConsumer");
        addHubWorker();
      }
    }

    private int getWorkerNo() {
      return stopped ? 0 : consumerNo;
    }
  }

  class QueueController implements Runnable, QueueFace<DataConnection> {
    private final RoundQueue<DataConnection> queue;
    private final String                     config_name;
    private long                             lastPushPop;
    private double                           queueMean;
    private long                             system_start_time;
    private final static long                QUEUE_CHECK_DELAY = 5 * /* 60 * */1000;
    private final static long                WARM_UP_PERIOD    = 10 * /* 60 * */1000;
    private final static double              alpha             = 0.9;
    private final static double              inc_margin        = 0.8;
    private final static double              dec_margin        = 0.4;
    private final static double              inc_rate          = 1.5;
    private final static double              dec_rate          = 0.66667;
    private final int                        maximum_number_of_workers;
    private final int                        minimum_number_of_workers;
    private final ASLStructureHub            aslStructureHub;

    public QueueController(ASLStructureHub aslStructureHub, RoundQueue<DataConnection> queue, String config_name, int maximum_number_of_workers, int minimum_number_of_workers) {
      this.aslStructureHub = aslStructureHub;
      this.queue = queue;
      this.config_name = config_name;
      double rate = (dec_margin * (1 - alpha) + (inc_margin - dec_margin) * alpha);
      queueMean = queue.getLimit() * rate;
      this.maximum_number_of_workers = maximum_number_of_workers;
      this.minimum_number_of_workers = minimum_number_of_workers;
      new Thread(this).start();
    }

    @Override
    public void run() {
      system_start_time = System.currentTimeMillis();
      try {
        Thread.sleep(WARM_UP_PERIOD);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      lastPushPop = System.currentTimeMillis();
      //system_start_time = lastPushPop - WARM_UP_PERIOD;
      if (Logger.getLogger(QueueController.class).isTraceEnabled()) {
        Logger.getLogger(QueueController.class).trace( //
            "Started ASL QueueControler for config: " + config_name//
                + " with queueMean=" + queueMean//
                + " and lastPushPop=" + lastPushPop//
                + " and system_start_time=" + system_start_time);
      }
      while (true) {
        try {
          Thread.sleep(Math.max(QUEUE_CHECK_DELAY, lastPushPop - System.currentTimeMillis()) + QUEUE_CHECK_DELAY);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (lastPushPop + QUEUE_CHECK_DELAY <= System.currentTimeMillis() + 5) { // use 5ms as margin of java sleep error
          if (queueMean < dec_margin * queue.getLimit()) {
            changeConfigurationValue(dec_rate);
          }
        }
      }
    }

    boolean pushIntoQueue(DataConnection dataConnection) {
      if (queue.isFull()) {
        changeConfigurationValue(1 + inc_rate);
      }
      boolean b = this.queue.syncPush(dataConnection);
      if (b) {
        changeQueueMean();
      } else {
        changeConfigurationValue(1 + inc_rate);
      }
      if (queueMean > inc_margin * queue.getLimit()) {
        changeConfigurationValue(inc_rate);
      }
      return b;
    }

    DataConnection popFromQueue() {
      DataConnection dp = queue.syncPop();
      changeQueueMean();
      return dp;
    }

    private void changeConfigurationValue(double rate) {
      int configurationFromValue = getIntegerConfiguration(config_name);
      int configurationToValue = (int) Math.round(configurationFromValue * rate);
      configurationToValue = Math.min(configurationToValue, Math.max((int) (queueMean * 3), maximum_number_of_workers));
      configurationToValue = Math.max(configurationToValue, minimum_number_of_workers);
      if (configurationFromValue == configurationToValue)
        return;
      if (Logger.getLogger(QueueController.class).isDebugEnabled()) {
        Logger.getLogger(QueueController.class).debug(//
            "Changing ConfigurationValue for config: " + config_name//
                + " from configurationValue=" + configurationFromValue//
                + " to configurationValue=" + configurationToValue//
                + " and queueMean=" + queueMean//
        );
      }
      setConfiguration(config_name, String.valueOf(configurationToValue));
    }

    private void changeQueueMean() {
      int count = queue.getCount();
      long time = System.currentTimeMillis();
      double beta = (1 - alpha) * (time - lastPushPop) / (time - system_start_time);
      if (Logger.getLogger(QueueController.class).isTraceEnabled()) {
        Logger.getLogger(QueueController.class).trace(//
            "Changing queueMean for config: " + config_name//
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
    public DataConnection pop() {
      return popFromQueue();
    }

    @Override
    public boolean push(DataConnection dataConnection) {
      if (aslStructureHub.hasCommand(dataConnection.getCommand())) {
        return pushIntoQueue(dataConnection);
      }
      return true;
    }
  }

  class RoundQueue2QueueFace implements QueueFace<DataConnection> {
    private final RoundQueue<DataConnection> queue;
    private final ASLStructureHub            aslStructureHub;

    public RoundQueue2QueueFace(ASLStructureHub aslStructureHub, RoundQueue<DataConnection> queue) {
      this.aslStructureHub = aslStructureHub;
      this.queue = queue;
    }

    @Override
    public DataConnection pop() {
      return queue.syncPop();
    }

    @Override
    public boolean push(DataConnection dataConnection) {
      if (aslStructureHub.hasCommand(dataConnection.getCommand())) {
        return queue.syncPush(dataConnection);
      }
      return true;
    }
  }

  @Override
  public void close() throws IOException {
    this.exit(false);
  }

  @Override
  public void exit(boolean force) {
    stopped = true;
    for (ListenerNode aslListener : aslListeners) {
      aslListener.die();
      try {
        aslListener.exit();
      } catch (IOException e) {
        Logger.getLogger(getClass()).error("Error exiting listener.", e);
      }
    }
    for (RequestFetcherNode aslRequestFetcher : aslRequestFetchers) {
      aslRequestFetcher.die();
    }
    refreshAll();
  }
}