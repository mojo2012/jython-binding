package at.spot.jython;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This is just marker interface.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PythonClass {
	String moduleName();

	String className();

	Class<?>[] constructorArgs() default {};
}
