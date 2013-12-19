package org.nise.ux.asl.data;

public class ServiceException extends Exception {
  private static final long serialVersionUID = 2110480929989484038L;

  public ServiceException(Throwable cause) {
    super("Server-side Service Exception/Error Occurred.", cause);
  }
}
