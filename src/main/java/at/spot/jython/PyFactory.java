package at.spot.jython;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;

public class PyFactory {
	protected static final PyFactory INSTANCE = new PyFactory();
	protected Map<Class<?>, PySystemState> states = new HashMap<>();

	private PyFactory() {
	}

	public static PyFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Creates an instance of the python class that implements the type.
	 * 
	 * @param type
	 *            the interface that is being implemented by the python class
	 * @param moduleName
	 *            the module the python class is implemented in.
	 * @param className
	 *            the python class name
	 * @param args
	 *            the args that will be passed to the constructor
	 * @return a java instance of the python class.
	 */
	public <T> T createInstance(Class<T> type, String moduleName, String className, Object... args) {
		PyObject importer = getState(type).getBuiltins().__getitem__(Py.newString("__import__"));

		setClasspath(importer);

		PyObject module = importer.__call__(Py.newString(moduleName));
		PyObject pyClass = module.__getattr__(className);

		T instance = createObject(pyClass, type, args);

		return instance;
	}

	public <T> T createInstance(String moduleName, String className, Object... args) {
		PyObject importer = new PySystemState().getBuiltins().__getitem__(Py.newString("__import__"));

		setClasspath(importer);

		PyObject module = importer.__call__(Py.newString(moduleName));
		PyObject pyClass = module.__getattr__(className);

		T instance = createObject(pyClass, type, args);

		return instance;
	}

	protected void setClasspath(PyObject importer, String... paths) {
		// get the sys module
		PyObject sysModule = importer.__call__(Py.newString("sys"));

		// get the sys.path list
		PyList path = (PyList) sysModule.__getattr__("path");

		// add the current directory to the path
		final PyString pythonPath = Py.newString(getClass().getResource(".").getPath());
		path.add(pythonPath);

		String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
		List<PyString> classPath = Stream.of(classpath).map(s -> Py.newString(s)).collect(Collectors.toList());

		path.addAll(classPath);
	}

	protected <T> T createObject(PyObject pyClass, Class<T> type, Object... args) {
		return createObject(pyClass, type, args, Py.NoKeywords);
	}

	/**
	 * Instantiates the python class and returns the java representation.
	 */
	protected <T> T createObject(PyObject pyClass, Class<T> type, Object args[], String keywords[]) {
		PyObject convertedArgs[] = new PyObject[args.length];

		for (int i = 0; i < args.length; i++) {
			convertedArgs[i] = Py.java2py(args[i]);
		}

		return (T) pyClass.__call__(convertedArgs, keywords).__tojava__(type);
	}

	/**
	 * Returns a {@link PySystemState} for the given type or creates one.
	 */
	protected <T> PySystemState getState(Class<T> type) {
		PySystemState state = states.get(type);

		if (state == null) {
			state = new PySystemState();
			states.put(type, state);
		}

		return state;
	}
}
