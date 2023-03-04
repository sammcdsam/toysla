import bluetooth
import time
import adafruit_gps
import serial
import picket
import RPi.GPIO as GPIO

 
# establish bluetooth socket connection with Android app/Blueterm
uuid =  "94f39d29-7d6d-437d-973b-fba39e49d4ee"
server_socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
port = bluetooth.PORT_ANY
server_socket.bind(("", port))
server_socket.listen(1)
port = server_socket.getsockname()[1]

bluetooth.advertise_service( server_socket, "SampleServer", 
            service_id = uuid, 
            service_classes = [uuid,bluetooth.SERIAL_PORT_CLASS],
            profiles = [bluetooth.SERIAL_PORT_PROFILE],
#           protocols = [OBEX_UUID
            )

client_socket,address = server_socket.accept()
client_socket.setblocking(0)
print("Accepted connection from ", address)
time.sleep(.1)

uart = serial.Serial("/dev/ttyUSB0", baudrate=9600, timeout=3000)   # open USB for GPS module
geofence = picket.Fence()                                           # create a geofence using the picket class
gps = adafruit_gps.GPS(uart, debug=False)                           # create the gps profuile using the USB
gps.send_command(b'PMTK314,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0')  # send the command to the GPS to only send required data. (different options in the datasheet)
gps.send_command(b'PMTK220,1000')                                   # set the gps update rate to 1 second

gps_active = False
geofence_active = False
infence = True
geofence_point_counter = 0

#acceleration controls setup
pwm_freq = 100                  # pwm frequency is 100Hz                               
a_mode = "normal"                 # Set acceleration mode to "normal" start or modified "soft" start

# WHERE ARE WE GETTING THIS VALUE INPUT FROM - read the comments, don't be a dick
#not being a dick, there was just no input for the pedal state. You said its the pedal state but where was the pedal read coming from
sig = 0                         # Pedal State: Pressed = 1, Not Pressed = 0 
dc0 = 0                         # driver motors
dc1 = 60                        # steering motor

# start GPIO procedures
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)

# driver motors
GPIO.setup(12, GPIO.OUT)        # Set GPIO pin 12 to output mode 
pwm0 = GPIO.PWM(12,pwm_freq)    # Intialize PWM on pin to pwm frequency variable
GPIO.setup(16, GPIO.OUT)        # Set GPIO pin 16 to output mode 
 
# steering motor
GPIO.setup(19, GPIO.OUT)        # pwm pin
pwm1 = GPIO.PWM(19, pwm_freq)
GPIO.setup(6, GPIO.OUT)         # dir pin

#Collision front sensor
GPIO.setup(4, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)

#collision rear sensor
GPIO.setup(17, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)

data = ""
i = 0 # steering wheel incrementer
i_max = 10
i_min = -10
prev_time = time.monotonic()    # record the time for acceleration controls.

car_forward = False
car_backward = False

# Sam changed this to a "Global" while loop so that way we dont have to copy
# the bluetooth and gps code.
current_time = time.monotonic()


try:
    while True:
        forward_collision = GPIO.input(4)
        backward_collision = GPIO.input(17)

        print(forward_collision)
        print(backward_collision)
        time.sleep(.2)
        try:
            data = client_socket.recv(1024)
            data = data.decode("utf-8").lower()
            if len(data) == 0:
                print("no data")
            print("Received: ", data)
        except bluetooth.btcommon.BluetoothError as ex:
            if(ord(ex.args[0][1]) + ord(ex.args[0][2]) == 98):
                if data == 'l' or data == 'r' or data == 'v':
                    data = ''
            else:
                try:
                    client_socket,address = server_socket.accept()
                    client_socket.setblocking(0)
                    print("HELLO")
                except any as dumbass:
                    print(ex)
                    print(dumbass)
                    break
        
        
        gps.update()                                    # update the gps location
        
        if not gps.has_fix:
                # Try again if we don't have a fix yet.
                # print('GEOFENCE NOT ACTIVE')
                geofence_active = False
                #send message to parent (WAITING ON GPS FIX)
                
        else: 

            point = (gps.latitude , gps.longitude)
            
            # if bluetooth says add a point to the fence.
            if data == 'v': 
                geofence.add_point(point)                               # add the current gps point to the fence
                geofence_point_counter += 1                             # increment the counter to make sure there are 3 points
                print(geofence_point_counter)

            if geofence_point_counter < 3:               # geofence has been disabled or can not work.
                geofence_active = False
            elif data == 't' and geofence_point_counter >=3 :                                                       # turn on geofence
                geofence_active = True
                
        if geofence_active:
            infence = geofence.check_point((gps.latitude, gps.longitude))
        else:
            infence = True


        if not infence:
            data = 's'
                        
        # ??????? so the remote control only works when the child is pressing the pedal?????? - yup, that's the plan
                        # While the pedal is pressed

            
        if(data == "f" or sig == 1):
            print("THE CAR MOVES FORWARD")
            
            if not forward_collision:
                GPIO.output(16, GPIO.HIGH)
                if (a_mode == "normal"):
                
                    dc0 = 70                            # Intialize Duty Cycle to 100
                    pwm0.start(dc0)                     # Start PWM with 100% Duty Cycle
                else:
                    if car_backward == True:
                        dc0 = 0
                        car_backward = False
                    if dc0 == 0:
                        pwm0.start(dc0)
                    if dc0 < 51:                  # for less than 51
                        print(dc0)
                        pwm0.ChangeDutyCycle(dc0) # start at 0
                        dc0 += 4                  # increment.
                        time.sleep(.1)
                        car_forward = True
            else:
                print("FORWARD COLLISION DETECTED")
                car_forward= False
                car_backward = False
                dc0 = 0
                pwm0.stop() # would it be better to do pwm0.ChangeDutyCycle(0)
                pwm1.stop() 
                 
                    

        elif(data == "b" or sig == 1):
            print("THE CAR MOVES BACKWARD")
            if not backward_collision:
                GPIO.output(16, GPIO.LOW)

                if (a_mode == "normal"):
                    dc0 = 70                            # Intialize Duty Cycle to 100
                    pwm0.start(dc0)                     # Start PWM with 100% Duty Cycle
                else:
                    if car_forward == True:
                        dc0 = 0
                        car_forward = False
                    
                    if dc0 == 0:
                        pwm0.start(dc0)
                    if dc0 < 51:
                        print(dc0)# for less than 51
                        pwm0.ChangeDutyCycle(dc0) # start at 0
                        dc0 += 4                  # increment.
            else:
                print("BACKWARD COLLISION DETECTED")
                car_forward= False
                car_backward = False
                dc0 = 0
                pwm0.stop() # would it be better to do pwm0.ChangeDutyCycle(0)
                pwm1.stop()

                    
                 
        elif(data == "l"):                       # potentionally l/r are backwards, easy software fix
            i = i - 1
            if(i >= i_min):
                print("THE CAR MOVES LEFT")
                pwm1.start(dc1)                      # NOTES: vary dc1 as needed
                GPIO.output(6, GPIO.HIGH)
                time.sleep(0.3)                      #        vary time.sleep() as needed
                pwm1.stop()
            else:
                print("The steering wheel cannot turn left!")
            
        elif(data == "r"):
            i = i + 1
            if(i <= i_max):
                print("THE CAR MOVES RIGHT")
                pwm1.start(dc1)
                GPIO.output(6, GPIO.LOW)
                time.sleep(0.3)
                pwm1.stop()
            else:
                print("The steering wheel cannot turn right!")

                
        elif(data == "s"):
            print("THE CAR STOPS")
            # we do not want pwm0.stop() to happen.
            # we want the pinouts to be set to low so that new commands can happen
            # GPIO.output(16, GPIO.LOW)
            car_forward= False
            car_backward = False
            dc0 = 0
            pwm0.stop() # would it be better to do pwm0.ChangeDutyCycle(0)
            pwm1.stop() # also maybe we should set the 4 outpins to 0

except KeyboardInterrupt:
    print("User shutdown")
    

# close and cleanup modules running
client_socket.close()
server_socket.close()
GPIO.cleanup()
