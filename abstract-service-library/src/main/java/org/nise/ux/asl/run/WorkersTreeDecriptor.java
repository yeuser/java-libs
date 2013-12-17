package org.nise.ux.asl.run;

import java.util.ArrayList;
import java.util.List;

import org.nise.ux.asl.face.WorkerFactory;

class WorkersTreeDecriptor {
  private WorkerFactory           workerFactory;
  private String                  name;
  private String                  parentName;
  private List<WorkersTreeDecriptor> nextSet = new ArrayList<WorkersTreeDecriptor>();
  private WorkersTreeDecriptor       parent  = null;
  private int[]                   range   = null;

  public WorkersTreeDecriptor(WorkerFactory workerFactory, String name) {
    this.workerFactory = workerFactory;
    this.name = name;
    this.parentName = null;
  }

  public WorkersTreeDecriptor(WorkerFactory workerFactory, String name, String parentName) {
    this.workerFactory = workerFactory;
    this.name = name;
    this.parentName = parentName;
  }

  public void setParent(WorkersTreeDecriptor parent) {
    this.parent = parent;
  }

  public void addNext(WorkersTreeDecriptor next) {
    this.nextSet.add(next);
  }

  public String getName() {
    return name;
  }

  public List<WorkersTreeDecriptor> getNextSet() {
    return nextSet;
  }

  public String getParentName() {
    return parentName;
  }

  public WorkersTreeDecriptor getParent() {
    return parent;
  }

  public WorkerFactory getWorkerFactory() {
    return workerFactory;
  }

  public int[] getRange() {
    return range;
  }

  public void setRange(int[] range) {
    this.range = range;
  }
}