import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.nise.ux.asl.run.ServiceClient;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class TesterClient {
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("log4j-client.properties");
    final int count = 1000;
    final Semaphore s = new Semaphore(1 - count);
    int concurrency = 100;
    final String host = "127.0.0.1";
    int port = 15015;
    final ServiceClient client = new ServiceClient(host, port, concurrency);
    Random random = new Random(System.currentTimeMillis());
    for (int i = 1; i <= count; i++) {
      final int qi = random.nextInt();
      final String query = "salam " + qi;
      Thread th = new Thread() {
        @Override
        public void run() {
          try {
            // DataOut2 response2 = client.invokeServiceCommand(DataOut2.class, "salam2", new DataIn2(query, qi));
            // Logger.getLogger(TesterClient.class).info(query + new GsonBuilder().setPrettyPrinting().create().toJson(response2));
            List<String> input = new ArrayList<String>();
            input.add(query + (qi * 3 + 0));
            input.add(query + (qi * 3 + 1));
            input.add(query + (qi * 3 + 2));
            TypeToken<Map<String, List<String>>> type4Return = new TypeToken<Map<String, List<String>>>() {
            };
            Map<String, List<String>> response3 = client.invokeServiceCommand(type4Return, "salam6", input);
            Logger.getLogger(TesterClient.class).info(query + new GsonBuilder().setPrettyPrinting().create().toJson(response3));
            //            DataOut response = client.invokeServiceCommand(DataOut.class, "salam5", new DataIn(query, qi));
            //            Logger.getLogger(TesterClient.class).info(query + new GsonBuilder().setPrettyPrinting().create().toJson(response));
          } catch (Throwable t) {
            Logger.getLogger(TesterClient.class).error("", t);
          }
          s.release();
        }
      };
      th.setDaemon(false);
      th.start();
    }
    s.acquire();
    System.out.println("-------------FIN--------------");
    client.close();
    //    System.exit(0);
  }
}