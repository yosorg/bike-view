from neopixel import *
from http.server import BaseHTTPRequestHandler, HTTPServer
import time

# LED strip configuration:
LED_COUNT      = 8       # Number of LED pixels.
LED_PIN        = 18      # GPIO pin connected to the pixels (must support PWM!).
LED_FREQ_HZ    = 800000  # LED signal frequency in hertz (usually 800khz)
LED_DMA        = 10      # DMA channel to use for generating signal (try 10)
LED_BRIGHTNESS = 255     # Set to 0 for darkest and 255 for brightest
LED_INVERT     = False   # True to invert the signal (when using NPN transistor level shift)
LED_CHANNEL    = 0
#LED_STRIP      = ws.SK6812_STRIP_RGBW
#LED_STRIP      = ws.SK6812W_STRIP
LED_STRIP      = ws.WS2811_STRIP_GRB

LED_2_COUNT      = 8      # Number of LED pixels.
LED_2_PIN        = 13      # GPIO pin connected to the pixels (must support PWM! GPIO 13 or 18 on RPi 3).
LED_2_FREQ_HZ    = 800000  # LED signal frequency in hertz (usually 800khz)
LED_2_DMA        = 11      # DMA channel to use for generating signal (Between 1 and 14)
LED_2_BRIGHTNESS = 255     # Set to 0 for darkest and 255 for brightest
LED_2_INVERT     = False   # True to invert the signal (when using NPN transistor level shift)
LED_2_CHANNEL    = 1       # 0 or 1
LED_2_STRIP      = ws.WS2811_STRIP_GRB

# Colores
BLANK	       = Color(0, 0, 0)
TURN_COLOR     = Color(255, 170, 0)
TURN_COLOR2    = Color(255, 170, 0, 25)
RED	      	   = Color(255, 0, 0)
RED2	       = Color(255, 0, 0, 50)
BREAK_COLOR    = Color(190, 10, 48)
BREAK_COLOR2   = Color(187, 10, 48, 25)


def allRed(strip):
	for i in range(strip.numPixels()):
		strip.setPixelColor(i, RED)
		strip.show()

def allBlank(strip):
	for i in range(strip.numPixels()):
		strip.setPixelColor(i, BLANK)
		strip.show()

def turnLeft(strip, strip2, color, turnTime=0.1):
	TURN_TIME = 0.1
	for i in range((strip.numPixels()//4)+1):
		strip.setPixelColor(i, color)
		strip.setPixelColor((strip.numPixels()//2)-i, color)
		strip.show()
		time.sleep(turnTime/(strip.numPixels()//4))
	for i in range(strip2.numPixels()):
		strip2.setPixelColor(i, color)
		strip2.show()
		time.sleep(turnTime/(strip2.numPixels()))
	time.sleep(0.3)
	allBlank(strip)
	allBlank(strip2)
	time.sleep(0.3)

def turnRight(strip, strip2, color, turnTime=0.2):
	strip.setPixelColor(0, color)
	#strip.show()
	for i in range((strip.numPixels()//4)+1):
		strip.setPixelColor(strip.numPixels()-i, color)
		strip.setPixelColor((strip.numPixels()//2)+i, color)
		strip.show()
		time.sleep(turnTime/(strip.numPixels()//4))
	time.sleep(0.3)
	allBlank(strip)
	allBlank(strip2)
	time.sleep(0.3)

def brakeLight(strip, strip2, color):
	for j in range(0, 255, 25):
		for i in range(strip.numPixels()):
			strip.setPixelColor(i, color)
			strip.setBrightness(j)
			strip.show()
		for i in range(strip2.numPixels()):
			strip2.setPixelColor(i, color)
			strip2.setBrightness(j)
			strip2.show()
	time.sleep(0.3)
	allBlank(strip)
	allBlank(strip2)

# HTTPRequestHandler class
class testHTTPServer_RequestHandler(BaseHTTPRequestHandler):

	def do_POST(self):
		content_length = int(self.headers['Content-Length'])
		received = self.rfile.read(content_length)
		self.send_response(200)
		self.end_headers()
		if received == b'r': # Turn right lights
			turnRight(ledRing, sideStrips, TURN_COLOR)
		elif received == b'l': # Turn left lights
			turnLeft(ledRing, sideStrips, TURN_COLOR)
		elif received == b'n': # Night lights
			allRed(ledRing)
			allRed(sideStrips)
		elif received == b'b': # Brake lights
			brakeLight(ledRing, sideStrips, RED)
		elif received == b'o': # Lights off
			allBlank(ledRing)
			allBlank(sideStrips)
		#print(str(received))
		#self.wfile.write(received)



# Main program logic follows:
if __name__ == '__main__':
	# Create NeoPixel object with appropriate configuration.
	ledRing = Adafruit_NeoPixel(LED_COUNT, LED_PIN, LED_FREQ_HZ, LED_DMA, LED_INVERT, LED_BRIGHTNESS, LED_CHANNEL, LED_STRIP)
	sideStrips = Adafruit_NeoPixel(LED_2_COUNT, LED_2_PIN, LED_2_FREQ_HZ, LED_2_DMA, LED_2_INVERT, LED_2_BRIGHTNESS, LED_2_CHANNEL, LED_2_STRIP)

	# Intialize the library (must be called once before other functions).
	ledRing.begin()
	sideStrips.begin()
	print ('Press Ctrl-C to quit.')
	while True:
		"""for j in range(1,5):
			turnRight(ledRing, sideStrips, TURN_COLOR2)
		for j in range(1,5):
			turnLeft(ledRing, sideStrips, TURN_COLOR)
		time.sleep(2)
		brakeLight(ledRing, sideStrips, RED)
		time.sleep(2)
		allRed(ledRing)
		allRed(sideStrips)
		time.sleep(2)
		brakeLight(ledRing, sideStrips, RED)
		time.sleep(5)"""


		print('starting server...')
		import socket
		# get self ip print((([ip for ip in socket.gethostbyname_ex(socket.gethostname())[2] if not ip.startswith("127.")] or [[(s.connect(("8.8.8.8", 53)), s.getsockname()[0], s.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]]) + ["no IP found"])[0])
		server_address = ('10.42.0.200', 80)
		httpd = HTTPServer(server_address, testHTTPServer_RequestHandler)
		print('running server...')
		httpd.serve_forever()
