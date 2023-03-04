// Define pin numbers
const int trigPinLF = 12;
const int echoPinLF = 13;

const int trigPinRF = 9;
const int echoPinRF = 10;

const int trigPinLB = 4;
const int echoPinLB = 5;

const int trigPinRB = 6;
const int echoPinRB = 7;

const int LED = 8;
const int LED2 = 3;


// Define variables
long frontDuration;
int frontDistanceCm;
int distanceLF;
int distanceRF;
int dangerDistance = 120;
int counter = 0;
int stopLF = 0;
int stopRF = 0;
int clearLFstate = 0;
int clearRFstate = 0;

long backDuration;
int backDistanceCm;
int distanceLB;
int distanceRB;
int stopLB = 0;
int stopRB = 0;
int clearLBstate = 0;
int clearRBstate = 0;

// Setup 
void setup() {
  Serial.begin(9600); // Sets serial communication
  pinMode(trigPinLF, OUTPUT); // Sets the trigPinLF as an Output
  pinMode(echoPinLF, INPUT); // Sets the echoPinLF as an Input

  pinMode(trigPinRF, OUTPUT); 
  pinMode(echoPinRF, INPUT);

  pinMode(trigPinLB, OUTPUT);
  pinMode(echoPinLB, INPUT);

  pinMode(trigPinRB, OUTPUT);
  pinMode(echoPinRB, INPUT);

  pinMode(LED, OUTPUT); // Set LED Alert pin
  pinMode(LED2, OUTPUT);
  
}

void loop() {

sensorLF();
sensorRF();

sensorLB();
sensorRB();



}



/////////////////////FRONT SYSTEMS///////////////////////////////////


int checkDistanceLF(){

  // Generate the pulse
  digitalWrite(trigPinLF, LOW); // Low for 4 us
  delayMicroseconds(4);
  digitalWrite(trigPinLF, HIGH); // Generate trigger signal for 10 us
  delayMicroseconds(10);
  digitalWrite(trigPinLF, LOW);

  // Measure the pulse
  frontDuration = pulseIn(echoPinLF, HIGH); // Mesure time between pulses in microseconds
  frontDistanceCm = frontDuration * 0.034/2; // Convert to cm

  // Output the data
  Serial.print("SENSOR LF - ");
  Serial.print("Distance: ");
  Serial.print(frontDistanceCm);
  Serial.println(" cm");



  // Return distance in cm
  return frontDistanceCm;
}


int checkDistanceRF(){
  // Generate the pulse
  digitalWrite(trigPinRF, LOW); // Low for 4 us
  delayMicroseconds(4);
  digitalWrite(trigPinRF, HIGH); // Generate trigger signal for 10 us
  delayMicroseconds(10);
  digitalWrite(trigPinRF, LOW);

  // Measure the pulse
  frontDuration = pulseIn(echoPinRF, HIGH); // Mesure time between pulses in microseconds
  frontDistanceCm = frontDuration * 0.034/2; // Convert to cm

  // Output the data
  Serial.print("SENSOR RF - ");
  Serial.print("Distance: ");
  Serial.print(frontDistanceCm);
  Serial.println(" cm");



  // Return distance in cm
  return frontDistanceCm;
}



// Function that checks to see if the LF sensor is clear
int sensorLF(){
  
  // Check the distance of the front left sensor
  distanceLF = checkDistanceLF();

  // M of N Detection Loop to reduce falses
  if (distanceLF < dangerDistance){
    counter = 0;

    // Check for 3 out of 6 detections
    for (int i = 0; i <= 6; i++){
      distanceLF = checkDistanceLF();
      if (distanceLF < dangerDistance){
        counter = counter + 1;
      }
    }
    
    // If three of the detections are within range, send stop signal
    if (counter > 2){
      stopFront();
    }
  }

}

// Function that checks to see if the RF sensor is clear
int sensorRF(){
  
  // Check the distance of the front right sensor
  distanceRF = checkDistanceRF();

  // M of N Detection Loop to reduce falses
  if (distanceRF < dangerDistance){
    counter = 0;

    // Check for 3 out of 6 detections
    for (int i = 0; i <= 6; i++){
      distanceRF = checkDistanceRF();
      if (distanceRF < dangerDistance){
        counter = counter + 1;
      }
    }
    
    // If three of the detections are within range, send stop signal
    if (counter > 2){
      stopFront();
    }
  }

}

// Function serves as a more thourough M of N check
int clearLF(){
  
  // Initial check of distance
  distanceLF = checkDistanceLF();

  // M of N loop to check if front LF is clear
  while(stopLF == 1){
    counter = 0;

    // Loop to check 20 times if obstacle is no longer present
    for (int i = 0; i <= 20; i++){
      distanceLF = checkDistanceLF();
      if (distanceLF > dangerDistance){
        counter = counter + 1;
      }

      // If it gets 15 out of 20 detections are clear, sends a clear signal
      if (counter == 15){
        stopLF = 0;
        return 1;
      }
    }
  }
}

// Function serves as a more thourough M of N check
int clearRF(){
  
  // Initial check of distance
  distanceRF = checkDistanceRF();

  // M of N loop to check if front LF is clear
  while(stopRF == 1){
    counter = 0;

    // Loop to check 20 times if obstacle is no longer present
    for (int i = 0; i <= 20; i++){
      distanceRF = checkDistanceRF();
      if (distanceRF > dangerDistance){
        counter = counter + 1;
      }

      // If it gets 15 out of 20 detections are clear, sends a clear signal
      if (counter == 15){
        stopRF = 0;
        return 1;
      }
    }
  }
}

void stopFront(){

  // Temporary pin for alert
  digitalWrite(LED, HIGH);

  // Set stop bit to 1
  stopLF = 1;
  stopRF = 1;

  // Check if front sensors are clear
  clearLFstate = clearLF();
  clearRFstate = clearRF();
  if (clearLFstate == 1 && clearRFstate == 1){
    digitalWrite(LED, LOW);
    clearLFstate = 0;
    clearRFstate = 0;
  }

}


/////////////////////BACK SYSTEMS///////////////////////////////////


int checkDistanceLB(){

  // Generate the pulse
  digitalWrite(trigPinLB, LOW); // Low for 4 us
  delayMicroseconds(4);
  digitalWrite(trigPinLB, HIGH); // Generate trigger signal for 10 us
  delayMicroseconds(10);
  digitalWrite(trigPinLB, LOW);

  // Measure the pulse
  backDuration = pulseIn(echoPinLB, HIGH); // Mesure time between pulses in microseconds
  backDistanceCm = backDuration * 0.034/2; // Convert to cm

  // Output the data
  Serial.print("SENSOR LB - ");
  Serial.print("Distance: ");
  Serial.print(backDistanceCm);
  Serial.println(" cm");



  // Return distance in cm
  return backDistanceCm;
}


int checkDistanceRB(){
  // Generate the pulse
  digitalWrite(trigPinRB, LOW); // Low for 4 us
  delayMicroseconds(4);
  digitalWrite(trigPinRB, HIGH); // Generate trigger signal for 10 us
  delayMicroseconds(10);
  digitalWrite(trigPinRB, LOW);

  // Measure the pulse
  backDuration = pulseIn(echoPinRB, HIGH); // Mesure time between pulses in microseconds
  backDistanceCm = backDuration * 0.034/2; // Convert to cm

  // Output the data
  Serial.print("SENSOR RB - ");
  Serial.print("Distance: ");
  Serial.print(backDistanceCm);
  Serial.println(" cm");



  // Return distance in cm
  return backDistanceCm;
}



// Function that checks to see if the LF sensor is clear
int sensorLB(){
  
  // Check the distance of the front left sensor
  distanceLB = checkDistanceLB();

  // M of N Detection Loop to reduce falses
  if (distanceLB < dangerDistance){
    counter = 0;

    // Check for 3 out of 6 detections
    for (int i = 0; i <= 6; i++){
      distanceLB = checkDistanceLB();
      if (distanceLB < dangerDistance){
        counter = counter + 1;
      }
    }
    
    // If three of the detections are within range, send stop signal
    if (counter > 2){
      stopBack();
    }
  }

}

// Function that checks to see if the RF sensor is clear
int sensorRB(){
  
  // Check the distance of the front right sensor
  distanceRB = checkDistanceRB();

  // M of N Detection Loop to reduce falses
  if (distanceRB < dangerDistance){
    counter = 0;

    // Check for 3 out of 6 detections
    for (int i = 0; i <= 6; i++){
      distanceRB = checkDistanceRB();
      if (distanceRB < dangerDistance){
        counter = counter + 1;
      }
    }
    
    // If three of the detections are within range, send stop signal
    if (counter > 2){
      stopBack();
    }
  }

}

// Function serves as a more thourough M of N check
int clearLB(){
  
  // Initial check of distance
  distanceLB = checkDistanceLB();

  // M of N loop to check if front LF is clear
  while(stopLB == 1){
    counter = 0;

    // Loop to check 20 times if obstacle is no longer present
    for (int i = 0; i <= 6; i++){
      distanceLB = checkDistanceLB();
      if (distanceLB > dangerDistance){
        counter = counter + 1;
      }

      // If it gets 15 out of 20 detections are clear, sends a clear signal
      if (counter == 3){
        stopLB = 0;
        return 1;
      }
    }
  }
}

// Function serves as a more thourough M of N check
int clearRB(){
  
  // Initial check of distance
  distanceRB = checkDistanceRB();

  // M of N loop to check if front LF is clear
  while(stopRB == 1){
    counter = 0;

    // Loop to check 20 times if obstacle is no longer present
    for (int i = 0; i <= 6; i++){
      distanceRB = checkDistanceRB();
      if (distanceRB > dangerDistance){
        counter = counter + 1;
      }

      // If it gets 15 out of 20 detections are clear, sends a clear signal
      if (counter == 3){
        stopRB = 0;
        return 1;
      }
    }
  }
}

void stopBack(){

  // Temporary pin for alert
  digitalWrite(LED2, HIGH);

  // Set stop bit to 1
  stopLB = 1;
  stopRB = 1;

  // Check if front sensors are clear
  clearLBstate = clearLB();
  clearRBstate = clearRB();
  if (clearLBstate == 1 && clearRBstate == 1){
    digitalWrite(LED2, LOW);
    clearLBstate = 0;
    clearRBstate = 0;
  }


}
