package at.spot.jython.testing;

import at.spot.jython.PythonClass;
import at.spot.jython.PythonMethod;

@PythonClass(moduleName = "CPythonExample", className = "CPythonExample")
public interface CPythonExample {

	@PythonMethod
	String getResponse();
}
