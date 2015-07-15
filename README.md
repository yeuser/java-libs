Abstract Service Library
=========
Abstract Service Library or ASL is a socket based replacement for restful libraries with consideration for high throughput and high concurrancy.
ASL uses json as the means of transfering objects and newlines as message separator.

### Installation
Dependencies can be find [here](https://github.com/yeuser/java-libs/tree/master/abstract-service-library/lib), and here is the link to ASL 
[latest release](https://github.com/yeuser/java-libs/tree/master/abstract-service-library/dist/abstract-service-library-2.4.5-java6).

Add these jar files to your build path or pom file.

### Usage:
For a quick start see javadocs, sample code and the below explainations:

[javadocs](https://yeuser.github.io/java-libs/abstract-service-library/dist/abstract-service-library-2.4.5-java6/api/)


[Sample Code in Test Folder](https://github.com/yeuser/java-libs/blob/master/abstract-service-library/src/test/java/)

Class ```org.nise.ux.asl.run.ServiceServerBuilder``` is used for creating a server listener and starting server.

```java
ServiceServerBuilder server = new ServiceServerBuilder(PORT_NUMBER);
server.addWorkerFactory(new WorkerFactory() {
   @Override
   public Worker getWorker() {
      return new Worker();
   }
}, "worker1");
server.addWorkerFactory(new WorkerFactory() {
   @Override
   public Worker getWorker() {
      return new Worker2();
   }
}, "worker2");
ServiceServer serviceServer = server.create();
```

Annotation of ```@MapCommand(command = "<command>")``` is used inside each Worker class to specify functions that handle commands with given command name. 

Code below show a sample Test Worker which outputs inputs given with adding some random text into it.
* ```DataIn``` and ```DataIn2``` are test input classes.
* ```DataOut``` and ```DataOut2``` are test output classes.

```java
public class TestWorker implements Worker {
  Random r = new Random();

  @MapCommand(command = "c1")
  public DataOut function1(DataIn input) {
    return new DataOut("dao" + input.dai, input.ti);
  }

  @MapCommand(command = "c2")
  public DataOut2 function2(DataIn2 input) {
    return new DataOut2("dao2" + input.dai2, input.ti2);
  }

  @MapCommand(command = "c3")
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

  @MapCommand(command = "c4")
  public DataOut2 function4(String dai, @DefaultValue("22") int ti) {
    return new DataOut2("dao2" + dai, ti);
  }
}
```

Class ```org.nise.ux.asl.run.ParallelServiceClient``` is used for creating a client and a connection to server in parallel mode. This Class runs each command remotely without blocking next commands. 

Code below demonstrates parallel commanding well.
```java
ParallelServiceClient client = new ParallelServiceClient(host, port, concurrency);
new Thread(new Runnable(){
    @Override
    public void run() {
        System.out.println("First Thread start...");
        DataOut data_out = client.invokeServiceCommand(DataOut.class, command, args...);
        // Blocks here until data_out is formed, but does not block other commands.
        System.out.println("First Thread done!" + data_out);
    }
}).start();
new Thread(new Runnable(){
    @Override
    public void run() {
        System.out.println("Second Thread start...");
        DataOut data_out = client.invokeServiceCommand(DataOut.class, command, args...);
        // Blocks here until data_out is formed, but does not block other commands.
        System.out.println("Second Thread done!" + data_out);
    }
}).start();
```

Class ```org.nise.ux.asl.run.SequentialServiceClient``` is used for creating a client and a connection to server in sequential mode. This Class runs all commands in a queue meaning that each command is sent to server only when result for last command is recieved. 
```java
SequentialServiceClient client = new SequentialServiceClient(host, port, concurrency);
DataOut data_out = client.invokeServiceCommand(DataOut.class, command, args...);
```
