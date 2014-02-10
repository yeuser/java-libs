import org.apache.log4j.PropertyConfigurator;
import org.nise.ux.asl.run.SequentialServiceClient;

public class TesterClientSingle2 {
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("log4j-client.properties");
    int port = 15015;
    String host = "127.0.0.1";
    final SequentialServiceClient client = new SequentialServiceClient(host, port);
    String[] commands = client.getPossibleCommands();
    for (String command : commands) {
      String[] description = client.describeCommand(command);
      System.out.print(command + ": returns: " + description[0] + " \r\n\tinputs:\t");
      for (int i = 1; i < description.length; i++) {
        System.out.print(description[i] + " ");
      }
      System.out.println();
    }
    System.out.println("-------------FIN--------------");
    client.close();
    //    System.exit(0);
  }
}