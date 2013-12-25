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
import org.nise.ux.lib.Living;
import org.nise.ux.lib.RoundQueue;

public class ServiceServerImpl extends ServiceServerAbstract {
  private static final String             DISTRIBUTER_NODE_NAME          = "DISTRIBUTER_NODE_";
  private static final String             NODE_NAME_PRIFIX               = "NODE_";
  private static final String             _NO                            = "_NO";
  private static final String             _QUEUE_LENGTH                  = "_QUEUE_LENGTH";
  private static final String             CONST_I_                       = "CONST_I_";
  private static final String             fetcherWorkerName              = "REQUEST_FETCHER";
  private static final String             CONST_I_WORKER_QUEUE_LENGTH    = CONST_I_ + fetcherWorkerName + _QUEUE_LENGTH;
  private static final String             fetcherWorkerConfigurationName = NODE_NAME_PRIFIX + fetcherWorkerName + _NO;
  private final int                       i_fetcher_worker_queue_length  = Integer.parseInt(validateConfiguration(CONST_I_WORKER_QUEUE_LENGTH, "2"));
  protected Map<String, ASLStructureHub>  workersListMap                 = new HashMap<String, ASLStructureHub>();
  protected List<String>                  workersNameList                = new ArrayList<String>();
  protected List<ListenerNode>            aslListeners                   = new ArrayList<ListenerNode>();
  protected List<RequestFetcherNode>      aslRequestFetchers             = new ArrayList<RequestFetcherNode>();
  private final QueueFace<DataConnection> rootWorkerQueue;
  private final String                    rootWorkerName;
  private QueueFace<DataStream>           aslRequestFetcherQueue;
  private int[]                           fetcherNo                      = { 5, 1000 };
  private int                             workerId                       = 1;
  private int                             distributerId                  = 1;
  private boolean                         stopped                        = false;

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
      String distrName = DISTRIBUTER_NODE_NAME + distributerId++;
      rootWorkerName = distrName;
      ASLStructureHub listASLDistributer = new ASLStructureHub(null, distrName, new int[] { 5, 1000 });
      rootWorkerQueue = listASLDistributer.getInQueue();
      addWorkerCompany(distrName, listASLDistributer);
      for (WorkersTreeDecriptor node : l1Nodes) {
        /* QueueFace<DataConnection> childWorkerQueue = */initWorkerList(listASLDistributer, node);
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

  private QueueFace<DataConnection> initWorkerList(ASLStructureHub parent, WorkersTreeDecriptor workersTreeDecriptor) {
    String workerName = workersTreeDecriptor.getName();
    WorkerFactory workerFactory = workersTreeDecriptor.getWorkerFactory();
    List<WorkersTreeDecriptor> workerList = workersTreeDecriptor.getNextSet();
    int[] range = workersTreeDecriptor.getRange();
    ASLStructureHub aslNode;
    if (workerFactory == null) {
      aslNode = new ASLStructureHub(parent, workerName, range);
    } else {
      aslNode = new ASLStructureHub(parent, workerName, range, workerFactory);
    }
    addWorkerCompany(workerName, aslNode);
    if (workerList.size() > 1) {
      String distrName = DISTRIBUTER_NODE_NAME + distributerId++;
      ASLStructureHub aslMiddleNode = new ASLStructureHub(aslNode, distrName, range);
      addWorkerCompany(distrName, aslMiddleNode);
      for (WorkersTreeDecriptor node : workerList) {
        /* QueueFace<DataConnection> childWorkerQueue = */initWorkerList(aslMiddleNode, node);
      }
    } else {
      if (workerList.size() == 1) {
        /* QueueFace<DataConnection> childWorkerQueue = */initWorkerList(aslNode, workerList.get(0));
      }
    }
    return aslNode.getInQueue();
  }

  private void addWorkerCompany(String workerName, ASLStructureHub aslNode) {
    workersNameList.add(workerName);
    workersListMap.put(workerName, aslNode);
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
    aslRequestFetcherQueue = new FetcherQueueController();
    refreshAll();
    aslListeners.add(new ListenerNode(getListeningPort(), 1, this, aslRequestFetcherQueue));
    Logger.getLogger(ServiceServerImpl.class).trace("Created a ListenerNode");
  }

  private void refreshFetchers() {
    int fetcher_no = Configurations.getConfigurationAsInteger(validateConfiguration(fetcherWorkerConfigurationName, String.valueOf(fetcherNo[0])));
    while (aslRequestFetchers.size() > fetcher_no) {
      Logger.getLogger(ServiceServerImpl.class).trace("Removed a RequestFetcher");
      aslRequestFetchers.remove(aslRequestFetchers.size() - 1).die();
    }
    while (aslRequestFetchers.size() < fetcher_no) {
      Logger.getLogger(ServiceServerImpl.class).trace("Created a RequestFetcher");
      RequestFetcherNode aslRequestFetcher = new RequestFetcherNode(aslRequestFetchers.size(), aslRequestFetcherQueue);
      aslRequestFetcher.addConsumer(rootWorkerQueue);
      aslRequestFetchers.add(aslRequestFetcher);
    }
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
    for (String workerName : workersNameList) {
      ASLStructureHub worker = workersListMap.get(workerName);
      if (worker != null) {
        worker.refreshWorkers();
      }
    }
    refreshFetchers();
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
    if (key.startsWith(NODE_NAME_PRIFIX) && key.endsWith(_NO)) {
      String workerName = key.substring(NODE_NAME_PRIFIX.length(), key.length() - _NO.length());
      if (workerName.equals(fetcherWorkerName)) {
        refreshFetchers();
      } else {
        ASLStructureHub worker = workersListMap.get(workerName);
        if (worker != null) {
          worker.refreshWorkers();
        }
      }
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
      stats.put(prefix + workersName + "_iQueue_Length", String.valueOf(workersListMap.get(workersName).getInQueueCount()));
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

  public class FetcherQueueController extends Living implements QueueFace<DataStream> {
    private final RoundQueue<DataStream> queue                     = new RoundQueue<DataStream>(i_fetcher_worker_queue_length);
    private final int                    minimum_number_of_workers = fetcherNo[0];
    private final int                    maximum_number_of_workers = fetcherNo[1];
    private final static long            QUEUE_CHECK_DELAY         = 1 * 1000;
    private static final int             FULL_TRIGGER_COUNT        = 1;
    private int                          full_cnt                  = 0;
    private static final int             EMPTY_TRIGGER_COUNT       = 100;
    private int                          empty_cnt                 = 0;

    public FetcherQueueController() {
      super("FetcherQueueController");
    }

    @Override
    public DataStream pop() {
      return queue.syncPop();
    }

    @Override
    public boolean push(DataStream data) {
      if (queue.isFull()) {
        changeConfigurationValue(queue.getLimit() + 1);
      }
      return queue.syncPush(data);
    }

    @Override
    public String[] getCommands() {
      return rootWorkerQueue.getCommands();
    }

    @Override
    public boolean hasCommand(String command) {
      return rootWorkerQueue.hasCommand(command);
    }

    private void changeConfigurationValue(int delta) {
      int configurationFromValue = getIntegerConfiguration(fetcherWorkerConfigurationName);
      int configurationToValue = (int) Math.round(configurationFromValue + delta);
      configurationToValue = Math.min(configurationToValue, maximum_number_of_workers);
      configurationToValue = Math.max(configurationToValue, minimum_number_of_workers);
      if (configurationFromValue == configurationToValue)
        return;
      if (Logger.getLogger(QueueController.class).isDebugEnabled()) {
        Logger.getLogger(QueueController.class).debug(//
            "Changing ConfigurationValue for config: " + fetcherWorkerConfigurationName//
                + " from configurationValue=" + configurationFromValue//
                + " to configurationValue=" + configurationToValue//
        );
      }
      setConfiguration(fetcherWorkerConfigurationName, String.valueOf(configurationToValue));
    }

    @Override
    protected void runtimeBehavior() throws Throwable {
      try {
        Thread.sleep(QUEUE_CHECK_DELAY);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (queue.isEmpty()) {
        empty_cnt++;
        full_cnt = 0;
      } else {
        empty_cnt = 0;
        full_cnt++;
      }
      if (empty_cnt > EMPTY_TRIGGER_COUNT) {
        changeConfigurationValue(-1);
      }
      if (full_cnt > FULL_TRIGGER_COUNT) {
        changeConfigurationValue(queue.getCount() + 1);
      }
    }
  }

  class ASLStructureHub {
    private final List<DistributerNode>           hubWorkers          = new ArrayList<DistributerNode>();
    private final List<QueueFace<DataConnection>> nextNodeWorkerQueue = new ArrayList<QueueFace<DataConnection>>();
    private final List<String>                    possibleCommands    = new ArrayList<String>();
    private final WorkerFactory                   workerFactory;
    private final String                          configurationName;
    private final QueueFace<DataConnection>       inQueue;
    private final ASLStructureHub                 parent;
    private final String                          name;
    private final RoundQueue<DataConnection>      queue;

    public ASLStructureHub(ASLStructureHub parent, String name, int[] range) {
      this(parent, name, range, null);
    }

    public ASLStructureHub(ASLStructureHub parent, String name, int[] range, WorkerFactory workerFactory) {
      this.parent = parent;
      this.name = name;
      this.workerFactory = workerFactory;
      this.configurationName = NODE_NAME_PRIFIX + name + _NO;
      validateConfiguration(configurationName, String.valueOf(range[0]));
      if (range == null || range.length == 0) {
        range = new int[] { 5, 1000 };
      }
      String CONST_I_WORKER_QUEUE_LENGTH = CONST_I_ + name + _QUEUE_LENGTH;
      int i_worker_queue_length = Integer.parseInt(validateConfiguration(CONST_I_WORKER_QUEUE_LENGTH, "10"));
      queue = new RoundQueue<DataConnection>(i_worker_queue_length);
      if (range.length > 1) {
        int minimum_number_of_workers = range[0];
        int maximum_number_of_workers = range[1];
        this.inQueue = new QueueController(this, queue, configurationName, maximum_number_of_workers, minimum_number_of_workers);
      } else {
        this.inQueue = new RoundQueue2QueueFace(this, queue);
      }
      if (parent != null) {
        parent.addNextNodeHub(this.inQueue);
        System.out.println(parent.name + " > " + this.name);
      }
    }

    public int getInQueueCount() {
      return this.queue.getCount();
    }

    private void addNextNodeHub(QueueFace<DataConnection> consumerQueue) {
      nextNodeWorkerQueue.add(consumerQueue);
    }

    private void addHubWorker() {
      DistributerNode aslDistributer;
      if (workerFactory == null) {
        aslDistributer = new DistributerNode(distributerId++, inQueue, name);
      } else {
        WorkerNode aslWorkerNode = new WorkerNode(workerId++, inQueue, workerFactory.getWorker(), name);
        for (String command : aslWorkerNode.getCommandSet()) {
          addCommand(command);
        }
        aslDistributer = aslWorkerNode;
      }
      aslDistributer.addConsumers(nextNodeWorkerQueue);
      hubWorkers.add(aslDistributer);
    }

    public QueueFace<DataConnection> getInQueue() {
      return inQueue;
    }

    private void addCommand(String command) {
      if (Logger.getLogger(ServiceServerImpl.class).isTraceEnabled()) {
        Logger.getLogger(ServiceServerImpl.class).trace("Worker: " + name + " addCommand on: " + command + "@" + possibleCommands);
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
        Logger.getLogger(ServiceServerImpl.class).trace("Worker: " + name + " hasCommand on: " + command + "@" + possibleCommands);
      }
      if (possibleCommands.contains(command)) {
        return true;
      }
      if (possibleCommands.contains(MapCommand.COMMAND_DEFAULT)) {
        return true;
      }
      return false;
    }

    public void removeHubWorker() {
      hubWorkers.remove(hubWorkers.size() - 1).die();
    }

    public void refreshWorkers() {
      while (hubWorkers.size() > getWorkerNo()) {
        Logger.getLogger(ASLStructureHub.class).trace("Removed a HubWorker#" + name);
        removeHubWorker();
      }
      while (hubWorkers.size() < getWorkerNo()) {
        Logger.getLogger(ASLStructureHub.class).trace("Created a HubWorker#" + name);
        addHubWorker();
      }
    }

    private int getWorkerNo() {
      return stopped ? 0 : getIntegerConfiguration(configurationName);
    }
  }

  class QueueController extends Living implements QueueFace<DataConnection> {
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
      super("QueueController#" + config_name);
      this.aslStructureHub = aslStructureHub;
      this.queue = queue;
      this.config_name = config_name;
      double rate = (dec_margin * (1 - alpha) + (inc_margin - dec_margin) * alpha);
      queueMean = queue.getLimit() * rate;
      this.maximum_number_of_workers = maximum_number_of_workers;
      this.minimum_number_of_workers = minimum_number_of_workers;
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
      if (Logger.getLogger(QueueController.class).isTraceEnabled()) {
        Logger.getLogger(QueueController.class).trace( //
            "Started ASL QueueControler for config: " + config_name//
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
          changeConfigurationValue(dec_rate);
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

    @Override
    public String[] getCommands() {
      return aslStructureHub.possibleCommands.toArray(new String[] {});
    }

    @Override
    public boolean hasCommand(String command) {
      return aslStructureHub.hasCommand(command);
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

    @Override
    public String[] getCommands() {
      return aslStructureHub.possibleCommands.toArray(new String[] {});
    }

    @Override
    public boolean hasCommand(String command) {
      return aslStructureHub.hasCommand(command);
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