import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.MapCommand;
import org.nise.ux.asl.face.Worker;

public class TestWorker2 implements Worker {
  Random r = new Random();

  @MapCommand(command = "salam4")
  public DataOut function1(DataIn input) {
    return new DataOut("dao" + input.dai, input.ti);
  }

  @MapCommand(command = "salam5")
  public DataOut2 function2(DataIn2 input) {
    return new DataOut2("dao2" + input.dai2, input.ti2);
  }

  @MapCommand(command = "salam6")
  public Map<String, List<String>> function3(List<String> input) {
    Map<String, List<String>> ret = new HashMap<String, List<String>>();
    ret.put("[0]", input);
    for (int i = 1; i < 2; i++) {
      ArrayList<String> al = new ArrayList<String>();
      al.add("8ursjkadfcsjkdl" + r.nextInt());
      al.add("8dfklgisdlgsjkdl" + r.nextInt());
      al.add("adfcsjkd234535l" + r.nextInt());
      al.add("8ur123123sjsjkdl" + r.nextInt());
      ret.put("[" + i + "]", al);
    }
    return ret;
  }

  @CommandChain(after = "salam6", name = "dummy")
  public void test(Map<String, List<String>> ret, Throwable t) {
//    Logger.getLogger(TestWorker2.class).info("test @CommandChain: return=" + ret + ", t=" + t);
  }
}