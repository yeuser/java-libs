package org.nise.ux.asl.face;

import org.nise.ux.asl.data.CommandChain;
import org.nise.ux.asl.data.MapCommand;

/**
 * Interface of worker objects.
 * All Methods in Worker that implement MapCommand & CommandChain annotations.
 * <p>
 * <b>Note:</b> Client should be aware of Server side &lt;command::function&gt; set. (type and number of arguments and return object of each function/command)
 * </p>
 * 
 * @see MapCommand
 * @see CommandChain
 * @author Yaser Eftekhari ( ^ - ^ )
 */
public interface Worker {
}