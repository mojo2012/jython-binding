# jython-binding
This micro-framework allows you to create native java object instances of python classes and interact with them as if they were real java objects.

## Usage
There are two different approaches to use python class instances:
* The python class implements a java interface and can therefore be directly used in java
* Use a java proxy to wrap any python object

The latter has some negative performance impact but is more flexible.

### The python object factory
The easiest approach is to use a custom interface that holds the necessary python class meta data to create a new python object.

```
@PythonClass(moduleName = "Car", className = "Car")
public interface Car {
	String getNumberPlate();
}
```

> The python class also has to implement that interface.


Simply annotate your interface with the `@PythonClass` to allow the python factory to create a proxy object. The `moduleName` represents the python file name (must be in the classpath). The `className` represents the actual class name of the python class.

You can then instantiate the python class using the `PyFactory`:
  
```
Car car = PyFactory.getInstance().createInstance(Car.class);
```

If you can't customize the interface you can also directly pass the `moduleName` and `className` info to the `PyFactory`:
```
Car car = PyFactory.getInstance().createInstance(YourInterface.class, "Car", "Car");
```

When you can't customize the python class (and hence can't implement an interface), you can create your own interface matching the python class's API and create a proxy instance using the `PyFactory`:
```
Car car = PyFactory.getInstance().createProxyInstance(Car.class, numberPlate);
```

For the proxy to be able to forward method calls you have to annotate the methods with `@PythonMethod`. The method name and the arguments have to match the python method (except for the first `self` parameter):
```
@PythonClass(moduleName = "Car", className = "Car")
public interface Car {

	@PythonMethod
	String getNumberPlate();
}
```
> There is also a factory method availalbe that allows you to pass in `moduleName` and `className` separately.

### Custom python class that implements a java interface
When you write custom python classes, the best way to use them in java, is to implement a java interface. This allows you to directly access the python object using the interface methods.

Interface **Building.java**
```
public interface Building {
	public String getBuildingName();
	public void setBuildingName(String name);
	public String getBuildingAddress();
	public void setBuildingAddress(String address);
	public int getBuildingId();
	public void setBuildingId(int i);
}
```

The **Building.py** that sub-classes a Java interface:
```
from at.spot.jython.testing import Building

class Building(Building):
	def __init__(self, id = -1, name = None, address = None):
		self.id = id
		self.name = name
		self.address = address

	def getBuildingName(self):
		return self.name

	def setBuildingName(self, name):
		self.name = name;

	def getBuildingAddress(self):
		return self.address
	   
	def setBuildingAddress(self, address):
		self.address = address

	def getBuildingId(self):
		return self.id

	def setBuildingId(self, id):
		self.id = id		
```

The python class has to implement the java interface methods. This allows jython to coerce the python instance to a realy java object:
```
Building building = PyFactory.getInstance().createInstance(Building.class, pyClassName, pyClassName, 1, buildingName, buildingAddress);
```

### Using a proxy to access use any python object
If you are not able to customize the python classes you want to use, there is another way using a proxy object. The proxy object contains the actual python object and forwards all method calls - returning appropriate java objects. 

The interface:
```
@PythonClass(moduleName = "Car", className = "Car", constructorArgs = { String.class })
public interface Car {
	@PythonMethod
	String getNumberPlate();
}
```

Car.py:
```
class Car():
	numberPlate = None

	def __init__(self, numberPlate = None):
		self.numberPlate = numberPlate
	
	def getNumberPlate(self):
		return self.numberPlate

	def setNumberPlate(self, numberPlate):
		self.numberPlate = numberPlate
```

The python class can be instantiated with:
```
Car car = PyFactory.getInstance().createProxyInstance(Car.class, "numberPlate");
```