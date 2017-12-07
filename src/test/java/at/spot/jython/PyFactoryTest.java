package at.spot.jython;

import org.junit.Assert;
import org.junit.Test;

import at.spot.jython.PyFactory;
import at.spot.jython.testing.Building;

public class PyFactoryTest {

	@Test
	public void testFactory() {
		Building building = PyFactory.getInstance().createInstance(Building.class, "Building", "Building", 1, "name",
				"address");

		Assert.assertNotNull(building);
		Assert.assertEquals(building.getBuildingId(), 1);
		Assert.assertEquals(building.getBuildingName(), "name");
		Assert.assertEquals(building.getBuildingAddress(), "address");
	}
}
