package org.nise.ux.asl.face;

/**
 * Interface of a Server side Service object.
 * 
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public interface ServiceServer extends ServiceBaseInterface {
  /**
   * constant value equal to {@value #LISTENING_PORT}
   */
  public final static String LISTENING_PORT = "LISTENING_PORT";
  /**
   * constant value equal to {@value #MAX_CLIENTS}
   */
  public static final String MAX_CLIENTS    = "MAX_CLIENTS";
  /**
   * constant value equal to {@value #TEST_MODE}
   */
  public static final String TEST_MODE      = "TEST_MODE";
}