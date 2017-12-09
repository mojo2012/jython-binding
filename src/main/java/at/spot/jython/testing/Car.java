package at.spot.jython.testing;

import at.spot.jython.PythonMethod;
import at.spot.jython.PythonClass;

@PythonClass(moduleName = "Car", className = "Car", constructorArgs = { String.class })
public interface Car {

	@PythonMethod
	String getNumberPlate();
}
