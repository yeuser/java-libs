import java.util.Map;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.nise.ux.asl.data.ChainException;
import org.nise.ux.asl.face.ServiceServer;
import org.nise.ux.asl.face.Worker;
import org.nise.ux.asl.face.WorkerFactory;
import org.nise.ux.asl.run.ServiceServerBuilder;

public class TesterServer {
  public static void main(String[] args) throws ChainException {
    PropertyConfigurator.configure("log4j-server.properties");
    ServiceServerBuilder server = new ServiceServerBuilder(15015);
    server.addWorkerFactory(new WorkerFactory() {
      @Override
      public Worker getWorker() {
        return new TestWorker();
      }
    }, "worker1");
    server.addWorkerFactory(new WorkerFactory() {
      @Override
      public Worker getWorker() {
        return new TestWorker2();
      }
    }, "worker2");
    final ServiceServer serviceServer = server.create();
//    new Thread() {
//      public void run() {
//        while (true) {
//          Map<String, String> stats = serviceServer.getAllStats();
//          Set<String> keySet = stats.keySet();
//          for (String key : keySet) {
//            System.out.println(key + ": " + stats.get(key));
//          }
//          try {
//            Thread.sleep(5 * 1000);
//          } catch (InterruptedException e) {
//            e.printStackTrace();
//          }
//        }
//      }
//    }.start();
  }
}

class DataIn {
  String dai;
  int    ti;

  public DataIn(String dai, int ti) {
    this.dai = dai;
    this.ti = ti;
  }
}

class DataOut {
  String dao;
  int    to;

  public DataOut(String dao, int to) {
    this.dao = dao;
    this.to = to;
  }
}

class DataIn2 {
  String dai2;
  int    ti2;

  public DataIn2(String dai2, int ti2) {
    this.dai2 = dai2;
    this.ti2 = ti2;
  }
}

class DataOut2 {
  String dao2;
  int    to2;

  public DataOut2(String dao2, int to2) {
    this.dao2 = dao2;
    this.to2 = to2;
  }
}