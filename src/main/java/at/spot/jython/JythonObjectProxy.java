package at.spot.jython;

import org.python.core.PyInstance;
import org.python.core.PyObject;

/**
 * This proxy class can be subclasses and implement an interface that conforms
 * to a given python object.
 */
public abstract class JythonObjectProxy {

	protected PyInstance pyObject;

	public JythonObjectProxy(final PyInstance pyObject) {
		this.pyObject = pyObject;
	}

	public Object invokeMethod(final String name, final Object... args) {
		final PyObject ret = pyObject.invoke(name, PyFactory.getInstance().convertArgs2Python(args));

		return PyFactory.getInstance().convertArgs2Java(ret);
	}

	public <T> T invokeMethod(final String name, final Class<T> returnType, final Object... args) {
		return invokeMethod(name, returnType, args);
	}

	public PyInstance getPyObject() {
		return this.pyObject;
	}
}
