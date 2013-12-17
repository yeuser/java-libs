package org.nise.ux.asl.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to chain commands with their after execution handlers,
 * <ol>
 * Methods implementing this annotation must have two input arguments:
 * <li>one Object for return object of (command) function</li>
 * <li>one Throwable input for possible {Exception or Error} occured</li>
 * </ol>
 * Example: <code>
 * <br/>
 * <b>@MapCommand(command = "example")</b><br/>
 * public <b>ReturnObject</b> someFunction(List<String> input) {<br/>
 *     &nbsp;&nbsp;&nbsp;<b>ReturnObject</b> returnObject = new <b>ReturnObject</b>();<br/>
 *     &nbsp;&nbsp;&nbsp;...<br/>
 *     &nbsp;&nbsp;&nbsp;return returnObject;<br/>
 * }<br/>
 * <br/>
 * <b>@CommandChain(after = "example", name = "dummy")</b><br/>
 * public void test(<b>ReturnObject</b> returnObject, Throwable t) {<br/>
 *     &nbsp;&nbsp;&nbsp;...<br/>
 * } </code>
 * 
 * @author Yaser Eftekhari ( ^ - ^ )
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandChain {
  String after();

  String name();
}