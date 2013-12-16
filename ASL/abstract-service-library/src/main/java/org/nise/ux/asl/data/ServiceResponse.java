package org.nise.ux.asl.data;

import com.google.gson.Gson;

public class ServiceResponse {
  public String    data;
  public Throwable throwable;

  public ServiceResponse(Object data) {
    this(data, null);
  }

  public ServiceResponse(Throwable throwable) {
    this(null, throwable);
  }

  public ServiceResponse(Object data, Throwable throwable) {
    this.data = new Gson().toJson(data);
    this.throwable = throwable;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}