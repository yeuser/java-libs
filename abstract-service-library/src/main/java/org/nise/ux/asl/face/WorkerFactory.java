package org.nise.ux.asl.face;

/**
 * Interface used to create Worker instances. <br/>
 * Because Managed Servicing System needs to create and destroy serving threads dynamically you're required to implement your own factory to let system produce instances of {@link Worker Worker} at runtime.
 * 
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public interface WorkerFactory {
  /**
   * Create a new instance of {@link Worker Worker}
   * 
   * @return a new simple service responser object
   */
  public Worker getWorker();
}