from at.spot.jython.testing import Building
# Building object that subclasses a Java interface

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
		