package org.nise.ux.lib;

import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

public class TimeProfiler {
  private Vector<String>             pause_keys   = new Vector<String>();
  private Vector<String>             resume_keys  = new Vector<String>();
  private HashMap<String, StopWatch> timeProfiles = new HashMap<String, StopWatch>();

  public void resume(String key) {
    if (timeProfiles.containsKey(key)) {
      timeProfiles.get(key).resume();
    } else {
      StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      timeProfiles.put(key, stopWatch);
    }
    if (!resume_keys.contains(key)) {
      resume_keys.add(key);
    }
  }

  public void pause(String key) {
    if (timeProfiles.containsKey(key)) {
      timeProfiles.get(key).suspend();
    } else {
      StopWatch stopWatch = new StopWatch();
      stopWatch.reset();
      timeProfiles.put(key, stopWatch);
    }
    pause_keys.remove(key);
    pause_keys.add(0, key);
  }

  /*
   * public void stop(String key) { if (timeProfiles.containsKey(key)) { timeProfiles.get(key).stop(); } }
   */
  public void reset() {
    timeProfiles.clear();
  }

  /*
   * public void remove(String key) { timeProfiles.remove(key); }
   */
  /*
   * public HashMap<String, StopWatch> getResults() { return timeProfiles; }
   */
  public void printAll(String additionalInfo, String[] keys) {
    String log = "";
    for (String key : keys) {
      if (timeProfiles.containsKey(key)) {
        log += (key + " : " + timeProfiles.get(key).getTime() + " ms,");
      } else {
        log += (key + " : " + 0 + " ms,");
      }
    }
    log = "{ info:" + additionalInfo + " " + log.substring(0, log.length() - 1) + "}";
    Logger.getLogger(this.getClass()).info(log);
  }

  public void printAllByFirstResume(String additionalInfo) {
    String log = "";
    for (String key : resume_keys) {
      if (timeProfiles.containsKey(key)) {
        log += (key + " : " + timeProfiles.get(key).getTime() + " ms,");
      } else {
        log += (key + " : " + 0 + " ms,");
      }
    }
    log = "{ info:" + additionalInfo + " " + log.substring(0, log.length() - 1) + "}";
    Logger.getLogger(this.getClass()).info(log);
  }

  public void printAllByLastPause(String additionalInfo) {
    String log = "";
    for (String key : pause_keys) {
      if (timeProfiles.containsKey(key)) {
        log += (key + " : " + timeProfiles.get(key).getTime() + " ms,");
      } else {
        log += (key + " : " + 0 + " ms,");
      }
    }
    log = "{ info:" + additionalInfo + " " + log.substring(0, log.length() - 1) + "}";
    Logger.getLogger(this.getClass()).info(log);
  }
}