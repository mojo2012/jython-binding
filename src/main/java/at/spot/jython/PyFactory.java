package at.spot.jython;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

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
	public <T> T createInstance(final Class<T> type, final String moduleName, final String className,
			final Object... args) {

		final PyObject importer = getImporter();
		setClasspath(importer);

		final PyObject instance = createObject(getPythonClass(importer, moduleName, className), args);

		// coerce into java type
		return (T) instance.__tojava__(type);
	}

	public <T> T createInstance(final Class<T> type, final Object... args) {
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

	@SuppressWarnings("unchecked")
	protected <T> T wrapPythonObject(final PyObject pyObject, final Class<T> type)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {

		final Class<T> proxy = (Class<T>) new ByteBuddy() //
				.subclass(JythonObjectProxy.class) //
				.implement(type) //
				// .method(ElementMatchers.not(ElementMatchers.isStatic())
				// .and(ElementMatchers.not(ElementMatchers.isAbstract()))
				// .and(ElementMatchers.isAnnotatedWith(PythonMethod.class))) //
				.method(ElementMatchers.isAnnotatedWith(PythonMethod.class)) //
				.intercept(MethodDelegation.to(MethodInterceptor.class)) //
				.make().load(getClass().getClassLoader()).getLoaded();

		final T instance = proxy.getConstructor(pyObject.getClass()).newInstance(pyObject);

		return instance;
	}

	protected static class MethodInterceptor {
		@RuntimeType
		public static Object intercept(@This final JythonObjectProxy instance, @Origin final Method method,
				@AllArguments final Object... args) {

			return instance.invokeMethod(method.getName(), args);
		}
	}

	protected PyObject getPythonClass(final PyObject importer, final String moduleName, final String className) {
		final PyObject module = importer.__call__(Py.newString(moduleName));
		final PyObject pyClass = module.__getattr__(className);

		return pyClass;
	}

	protected PyObject getImporter() {
		return new PySystemState().getBuiltins().__getitem__(Py.newString("__import__"));
	}

	protected Optional<PythonClass> getPythonClassAnnotation(final Class<?> type) {
		return Optional.of(type.getDeclaredAnnotation(PythonClass.class));
	}

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
	 * Instantiates the python class and returns the java representation.
	 */
	protected PyObject createObject(final PyObject pyClass, final Object[] args, final String[] keywords) {
		return pyClass.__call__(convertArgs(args), keywords);
	}

	public PyObject[] convertArgs(final Object... args) {
		final PyObject[] convertedArgs = new PyObject[args.length];

		for (int i = 0; i < args.length; i++) {
			convertedArgs[i] = Py.java2py(args[i]);
		}

		return convertedArgs;
	}

	/**
	 * Returns a {@link PySystemState} for the given type or creates one.
	 */
	protected <T> PySystemState getState(final Class<T> type) {
		PySystemState state = states.get(type);

		if (state == null) {
			state = new PySystemState();
			states.put(type, state);
		}

		return state;
	}
}
