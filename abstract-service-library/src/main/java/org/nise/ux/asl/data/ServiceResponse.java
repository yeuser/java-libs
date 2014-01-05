package org.nise.ux.asl.data;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ServiceResponse {
  public String data;
  public String throwable;

  public ServiceResponse(Object data) {
    this(data, null);
  }

  public ServiceResponse(Throwable throwable) {
    this(null, throwable);
  }

  public ServiceResponse(Object data, Throwable throwable) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      this.throwable = mapper.writeValueAsString(throwable);
    } catch (JsonGenerationException e) {
      Logger.getLogger(ServiceResponse.class).error("", e);
    } catch (JsonMappingException e) {
      Logger.getLogger(ServiceResponse.class).error("", e);
    } catch (IOException e) {
      Logger.getLogger(ServiceResponse.class).error("", e);
    }
    this.data = new Gson().toJson(data);
  }

  public Throwable getThrowable() {
    try {
      return new ObjectMapper().readValue(throwable, Throwable.class);
    } catch (Throwable t) {
      return new Gson().fromJson(throwable, Throwable.class);
    }
  }

  public String getThrowableAsString() {
    return throwable;
  }

  public <RD> RD getData(TypeToken<RD> type4Return) {
    return new Gson().fromJson(data, type4Return.getType());
  }

  public <RD> RD getData(Class<RD> class4Return) {
    return new Gson().fromJson(data, class4Return);
  }

  public String getDataAsJson() {
    return data;
  }

  @Override
  public String toString() {
    return "data=" + data + " throwable=" + throwable;
  }
}