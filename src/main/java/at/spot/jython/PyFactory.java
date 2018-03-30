package at.spot.jython;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;

import JyNI.PySystemStateJyNI;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

public class PyFactory {
	protected static final PyFactory INSTANCE = new PyFactory();
	protected PySystemState state;

	private PyFactory() {
	}

	public static PyFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Creates an instance of the python class that implements the interface and
	 * converts it to a java object. No proxying is used here.
	 * 
	 * @param type
	 *            the interface implemented by the python class
	 * @param moduleName
	 *            the module the python class is implemented in
	 * @param className
	 *            the python class name
	 * @param args
	 *            the constructor arguments used for instantiation
	 * @return a java instance of the python class.
	 */
	public <T> T createInstance(final Class<T> type, final String moduleName, final String className,
			final Object... args) {

		final PyObject importer = getImporter();
		setClasspath(importer);

		final PyObject instance = createObject(getPythonClass(importer, moduleName, className), args);

		// coerce into java type
		return (T) instance.__tojava__(type);
	}

	/**
	 * Creates an instance of the python class that implements the interface and
	 * converts it to a java object. No proxying is used here.
	 * 
	 * @param type
	 *            the interface implemented by the python class. It has to be
	 *            annotated with {@link PythonClass} containing the information
	 *            about how to instantiate the python class
	 * @param args
	 *            the constructor arguments used for instantiation
	 * 
	 * @return a java instance of the python class.
	 * 
	 * @see this{@link #createInstance(Class, String, String, Object...)}.
	 */
	public <T> T createInstance(final Class<T> type, final Object... args) {
		final Optional<PythonClass> ann = getPythonClassAnnotation(type);

		if (ann.isPresent()) {
			return createInstance(type, ann.get().moduleName(), ann.get().className(), args);
		} else {
			throw new IllegalArgumentException(String.format("Interface %s has no @% annotation", type.getName(),
					PythonClass.class.getSimpleName()));
		}
	}

	/**
	 * Returns an instance of a python class configured using the
	 * {@link PythonClass} annotation on the given interface type.
	 * 
	 * Use this to communicate with objects that don't implement a java interface.
	 * 
	 * @param the
	 *            interface of the generated proxy wrapper.
	 * @param the
	 *            constructor arguments used for instantiation
	 * @return an instance of the given interface (subclass of
	 *         {@link JythonObjectProxy} proxying all method calls to the underlying
	 *         python object.
	 */
	public <T> T createProxyInstance(final Class<T> type, final Object... args) {
		final PyObject importer = getImporter();
		setClasspath(importer);

		final Optional<PythonClass> ann = getPythonClassAnnotation(type);

		if (ann.isPresent()) {
			final PyObject instance = createObject(
					getPythonClass(importer, ann.get().moduleName(), ann.get().className()), args);

			try {
				return wrapPythonObject(instance, type);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException(
						String.format("Could not create python object proxy of type %s (module %s, class %s)",
								type.getName(), ann.get().moduleName(), ann.get().className()),
						e);
			}
		} else {
			throw new IllegalArgumentException(
					String.format("Type %s has no @% annotation", type.getName(), PythonClass.class.getSimpleName()));
		}
	}

	/**
	 * Creates a proxy ({@link JythonObjectProxy}) for a given python object.
	 */
	@SuppressWarnings("unchecked")
	protected <T> T wrapPythonObject(final PyObject pyObject, final Class<T> type)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {

		final Class<T> proxy = (Class<T>) new ByteBuddy() //
				.subclass(JythonObjectProxy.class) //
				.implement(type) //
				.method(not(isStatic()).and(isAnnotatedWith(PythonMethod.class))) //
				.intercept(MethodDelegation.to(MethodInterceptor.class)) //
				.make().load(getClass().getClassLoader()).getLoaded();

		final T instance = proxy.getConstructor(pyObject.getClass()).newInstance(pyObject);

		return instance;
	}

	/**
	 * Intercepts methods on the proxy that should be forwarded to the python
	 * object.
	 */
	protected static class MethodInterceptor {
		@RuntimeType
		public static Object intercept(@This final JythonObjectProxy instance, @Origin final Method method,
				@AllArguments final Object... args) {

			return instance.invokeMethod(method.getName(), args);
		}
	}

	/**
	 * Creates python class from the given definitions.
	 */
	protected PyObject getPythonClass(final PyObject importer, final String moduleName, final String className) {
		final PyObject module = importer.__call__(Py.newString(moduleName));
		final PyObject pyClass = module.__getattr__(className);

		return pyClass;
	}

	protected PyObject getImporter() {
		return getSystemState().getBuiltins().__getitem__(Py.newString("__import__"));
	}

	protected PySystemState getSystemState() {
		if (state == null) {
			state = new PySystemStateJyNI();
		}

		return state;
	}

	/**
	 * Returns the {@link PythonClass} annotation for the given type.
	 */
	protected Optional<PythonClass> getPythonClassAnnotation(final Class<?> type) {
		return Optional.of(type.getDeclaredAnnotation(PythonClass.class));
	}

	/**
	 * Adds the current folder and the java classpath to the python sys.path. This
	 * is necessary to find a python in your codebase.
	 */
	protected void setClasspath(final PyObject importer, final String... paths) {
		// get the sys module
		final PyObject sysModule = importer.__call__(Py.newString("sys"));

		// get the sys.path list
		final PyList path = (PyList) sysModule.__getattr__("path");

		// add the current directory to the path
		final PyString pythonPath = Py.newString(getClass().getResource(".").getPath());
		path.add(pythonPath);

		final String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
		final List<PyString> classPath = Stream.of(classpath).map(s -> Py.newString(s)).collect(Collectors.toList());

		path.addAll(classPath);
	}

	protected PyObject createObject(final PyObject pyClass, final Object[] args) {
		return createObject(pyClass, args, Py.NoKeywords);
	}

	/**
	 * Instantiates the python class with the given constructor arguments.
	 */
	protected PyObject createObject(final PyObject pyClass, final Object[] args, final String[] keywords) {
		return pyClass.__call__(convertArgs2Python(args), keywords);
	}

	/**
	 * Converts the given arguments to python objects.
	 */
	public PyObject[] convertArgs2Python(final Object... args) {
		final PyObject[] convertedArgs = new PyObject[args.length];

		for (int i = 0; i < args.length; i++) {
			convertedArgs[i] = Py.java2py(args[i]);
		}

		return convertedArgs;
	}

	/**
	 * Converts the given python object arguments to java objects. If only one
	 * argument is given, the corresponding java object is returned. In case
	 * multiple arguments are given, an array is returned.
	 */
	public Object convertArgs2Java(final PyObject... args) {
		Object ret = null;

		if (args.length == 1) {
			ret = args[0].__tojava__(Object.class);
		} else {
			final Object[] convertedArgs = new Object[args.length];

			for (int i = 0; i < args.length; i++) {
				convertedArgs[i] = args[i].__tojava__(Object.class);
			}

			ret = convertedArgs;
		}

		return ret;
	}

}
