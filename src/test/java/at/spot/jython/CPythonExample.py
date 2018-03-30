'''
 * BASED ON https://raw.githubusercontent.com/Stewori/JyNI/master/JyNI-Demo/src/JyNIDemo.py
'''

import DemoExtension

class CPythonExample():
	def getResponse(self):
		print "getResponse function called"
		uc = u'a\xac\u1234\u20ac\U00008000'
		return DemoExtension.unicodeTest(uc)
		
