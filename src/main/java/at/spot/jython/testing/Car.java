package at.spot.jython.testing;

import at.spot.jython.PythonClass;
import at.spot.jython.PythonMethod;

@PythonClass(moduleName = "Car", className = "Car")
public interface Car {

	@PythonMethod
	String getNumberPlate();
}
