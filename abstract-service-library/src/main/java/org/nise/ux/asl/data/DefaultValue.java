package org.nise.ux.asl.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to give default value for parameters of mapped commands,
 * All Parameters of all methods in Worker can implement this annotation:
 * <p>
 * <b>Note:</b> given value is a String object containing JSON representation of the object
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
@Target(ElementType.PARAMETER)
public @interface DefaultValue {
  String value();
}