package at.spot.jython;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PythonMethod {

	/**
	 * The name of the python class method.
	 */
	String name() default "";
}
