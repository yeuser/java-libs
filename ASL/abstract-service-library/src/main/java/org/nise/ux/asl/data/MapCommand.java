package org.nise.ux.asl.data;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to map commands with their handler functions,
 * All Methods in Worker can implement this annotation:
 * <p>
 * <b>Note:</b> Client should be aware of Server side &lt;command::function&gt; set. (type and number of arguments and return object of each function/command)
 * </p>
 * Example: <code>
 * <br/>
 * <b>@MapCommand(command = "example")</b><br/>
 * public <b>ReturnObject</b> someFunction(List<String> input) {<br/>
 *     &nbsp;&nbsp;&nbsp;<b>ReturnObject</b> returnObject = new <b>ReturnObject</b>();<br/>
 *     &nbsp;&nbsp;&nbsp;...<br/>
 *     &nbsp;&nbsp;&nbsp;return returnObject;<br/>
 * } </code>
 * 
 * @author Yaser Eftekhari ( ^ - ^ )
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MapCommand {
  public static final String COMMAND_DEFAULT = "@default";

  String command();

  boolean test() default false;
}