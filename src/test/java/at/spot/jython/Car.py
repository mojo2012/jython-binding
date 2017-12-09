class Car():
	numberPlate = None

	def __init__(self, numberPlate = None):
		self.numberPlate = numberPlate
	
	def getNumberPlate(self):
		return self.numberPlate

	def setNumberPlate(self, numberPlate):
		self.numberPlate = numberPlate
		