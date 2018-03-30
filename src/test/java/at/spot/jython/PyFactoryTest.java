package at.spot.jython;

import org.junit.Assert;
import org.junit.Test;

import at.spot.jython.testing.Building;
import at.spot.jython.testing.CPythonExample;
import at.spot.jython.testing.Car;

public class PyFactoryTest {

	@Test
	public void testPythonClassWithInterface() {
		final String pyClassName = "Building";
		final int buildingId = 1;
		final String buildingName = "Building 1";
		final String buildingAddress = "address";

		final Building building = PyFactory.getInstance().createInstance(Building.class, pyClassName, pyClassName, 1,
				buildingName, buildingAddress);

		Assert.assertNotNull(building);
		Assert.assertEquals(building.getBuildingId(), buildingId);
		Assert.assertEquals(building.getBuildingName(), buildingName);
		Assert.assertEquals(building.getBuildingAddress(), buildingAddress);
	}

	@Test
	public void testPythonClassWithoutInterfaceWithConstructorArgument() {
		final String numberPlate = "test plate";

		final Car car = PyFactory.getInstance().createProxyInstance(Car.class, numberPlate);

		Assert.assertNotNull(car);
		Assert.assertEquals(numberPlate, car.getNumberPlate());
	}

	@Test
	public void testPythonClassWithoutInterfaceWithoutConstructorArgument() {
		final Car car = PyFactory.getInstance().createProxyInstance(Car.class);

		Assert.assertNotNull(car);
		Assert.assertNull(car.getNumberPlate());
	}

	@Test
	public void testCPythonModule() {
		final CPythonExample cPython = PyFactory.getInstance().createProxyInstance(CPythonExample.class);

		Assert.assertNotNull(cPython.getResponse());
	}
}
