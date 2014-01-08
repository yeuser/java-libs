package org.nise.ux.asl.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nise.ux.asl.data.ChainException;
import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.face.ServiceServer;
import org.nise.ux.asl.face.Worker;
import org.nise.ux.asl.face.WorkerFactory;

/**
 * This is the Main Constructor Class of this service library.<br/>
 * To use this library create an instance of this class, then use {@link #create()} to start the service <h5>Example:</h5> <code>
 * ServiceServerBuilder server = new ServiceServerBuilder(15015);<br/>
 * server.addWorkerFactory(new WorkerFactory() {<br/>
 * &nbsp;&nbsp;&nbsp;<b>@Override</b><br/>
 * &nbsp;&nbsp;&nbsp;public Worker getWorker() {<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return new Worker();<br/>
 * &nbsp;&nbsp;&nbsp;}<br/>
 * }, "worker1");<br/>
 * server.addWorkerFactory(new WorkerFactory() {<br/>
 * &nbsp;&nbsp;&nbsp;<b>@Override</><br/>
 * &nbsp;&nbsp;&nbsp;public Worker getWorker() {<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return new Worker2();<br/>
 * &nbsp;&nbsp;&nbsp;}<br/>
 * }, "worker2");<br/>
 * ServiceServer serviceServer = server.create();
 * </code>
 * 
 * @see WorkerFactory
 * @see Worker
 * @see MapCommand
 * @see CommandChain
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public class ServiceServerBuilder {
  private Map<String, WorkersTreeDecriptor> WorkerFactoriesNamedMap = new HashMap<String, WorkersTreeDecriptor>();
  private Map<String, int[]>                ranges                  = new HashMap<String, int[]>();
  private int                               serverPort;
  private int                               max_clients             = 50;
  private boolean                           test_mode               = false;
  private int[]                             fetcherNo               = { 2, 50 };

  /**
   * @param serverPort
   *          Server ServiceServer Port
   */
  public ServiceServerBuilder(int serverPort) {
    this.serverPort = serverPort;
  }

  /**
   * Adds workers generated from this factory right after library listener, as first layer of workers
   * 
   * @param workerFactory
   * @param name
   * @return
   */
  public ServiceServerBuilder addWorkerFactory(WorkerFactory workerFactory, String name) {
    this.addWorkerFactory(workerFactory, name, null);
    return this;
  }

  /**
   * Adds workers generated from this factory right after workers of name {parentName}
   * 
   * @param workerFactory
   * @param name
   * @param parentName
   * @return
   */
  public ServiceServerBuilder addWorkerFactory(WorkerFactory workerFactory, String name, String parentName) {
    this.WorkerFactoriesNamedMap.put(name, new WorkersTreeDecriptor(workerFactory, name, parentName));
    return this;
  }

  private void setRanges(String key, int[] range) {
    this.ranges.put(key, range);
  }

  /**
   * Set value of configuration for {worker}_no as a constant factor
   * 
   * @param worker
   *          name of worker
   * @param num
   *          number of workers
   * @return
   */
  public ServiceServerBuilder setConstantWorkerNo(String worker, int num) {
    setRanges(worker, new int[] { Math.max(1, num) });
    return this;
  }

  /**
   * Set value of configuration for {worker}_no with automatic management with lower/upper bands specified.
   * 
   * @param worker
   *          name of worker
   * @param num
   *          number of workers
   * @return
   */
  public ServiceServerBuilder setAutoWorkerNo(String worker, int lower_bound, int upper_bound) {
    int low = Math.max(1, lower_bound);
    int high = Math.min(Math.max(low, upper_bound), 50);
    setRanges(worker, new int[] { low, high });
    return this;
  }

  /**
   * Set value of configuration for request_fetcher_no with automatic management with lower/upper bands specified.
   * 
   * @param num
   *          number of request-fetchers
   * @return
   */
  public ServiceServerBuilder setAutoFetcherNo(int lower_bound, int upper_bound) {
    int low = Math.max(1, lower_bound);
    int high = Math.min(Math.max(low, upper_bound), 50);
    fetcherNo = new int[] { low, high };
    return this;
  }

  /**
   * Creates a server listening on defined LISTENING_PORT
   * 
   * @return reference to main server object
   * @throws ChainException
   *           if connection of workers is wrong
   */
  public ServiceServer create() throws ChainException {
    List<WorkersTreeDecriptor> firstLayerNodes = new ArrayList<WorkersTreeDecriptor>();
    for (String key : WorkerFactoriesNamedMap.keySet()) {
      WorkersTreeDecriptor workerFactoryNamed = WorkerFactoriesNamedMap.get(key);
      int[] workerRange = ranges.get(workerFactoryNamed.getName());
      if (workerRange == null) {
        workerRange = new int[] { 1, 50 };
      }
      workerFactoryNamed.setRange(workerRange);
      String parentName = workerFactoryNamed.getParentName();
      WorkersTreeDecriptor parentWorkerFactoryNamed = WorkerFactoriesNamedMap.get(parentName);
      if (parentName == null) {
        firstLayerNodes.add(workerFactoryNamed);
      } else if (parentWorkerFactoryNamed != null) {
        linkParentHoodNodes(parentWorkerFactoryNamed, workerFactoryNamed);
      } else {
        throw new ChainException("Unchained Parrent: " + parentName);
      }
    }
    for (WorkersTreeDecriptor workersTreeDecriptor : firstLayerNodes) {
      workersTreeDecriptor.setParent(null);
    }
    ServiceServerImpl server = new ServiceServerImpl(firstLayerNodes, fetcherNo);
    server.setListeningPort(serverPort);
    server.setMax_clients(max_clients);
    server.setTest_mode(test_mode);
    server.start();
    return server;
  }

  private void linkParentHoodNodes(WorkersTreeDecriptor root, WorkersTreeDecriptor workerFactoryNamed) {
    root.addNext(workerFactoryNamed);
    workerFactoryNamed.setParent(root);
  }
}