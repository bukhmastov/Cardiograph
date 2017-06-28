#include <SoftwareSerial.h> 

const int PIN_INPUT_1 = A0;
const byte MESSAGE_PULSE = 0x00;
const byte MESSAGE_1 = 0x01;
int FRAME_RATE = 60;
int BYTES_PER_FRAME = 3;

int AVERAGE_TOLERANCE = 20;
int MAX_TOLERANCE = 10;
int pCalibrationDuration = 10 * 1000;
bool pCalibrating = false;
bool pNeedCalibration = false;
unsigned long pCalibrationTimeStart = 0;
unsigned long pMaxValueTime = 0;
int pMaxValue = 0;
int pAverageValue = 0;
int pCalibrationAverageAmount = 0;
int pCalibrationAverageCount = 0;
boolean pAverageCrossed = false;
int pLastValues[6] = {-1, -1, -1, -1, -1, -1};

void setup(){
    analogReference(DEFAULT);
    Serial.begin(38400);
    while(!Serial){}
}
void loop(){
    while (Serial.available() > 0) {
        int incoming = Serial.read();
        bool sync = false;
        if (byte(incoming) == 0xff) { // we got handshake request. We have to sync with 3 bytes of 0xff and continue broadcast data
            int counter = 0;
            sync = true;
            while (sync) {
                while (Serial.available() > 0) { // now we going to sync
                    int incoming = Serial.read();
                    switch (counter++) {
                        case 0:
                            FRAME_RATE = incoming;
                            break;
                        case 1:
                            BYTES_PER_FRAME = incoming;
                            break;
                        case 2:
                            AVERAGE_TOLERANCE = incoming;
                            break;
                        case 3:
                            MAX_TOLERANCE = incoming;
                            sync = false;
                            break;
                    }
                }
            }
            Serial.flush();
            delay(500);
            for (int i = 0; i < BYTES_PER_FRAME + 1; i++) {
                Serial.write(byte(0xee));
            }
            Serial.flush();
            pNeedCalibration = true;
        } else if (byte(incoming) == 0xee) { // pulse calibrate signal
            pNeedCalibration = true;
        }
    }
    unsigned long timeStart = millis();
    int data1 = int(analogRead(PIN_INPUT_1)) - 511;
    send(MESSAGE_1, data1);
    analysePulse(data1);
    Serial.flush();
    unsigned long delayTime = 1000/FRAME_RATE - (millis() - timeStart);
    if (delayTime > 0) delay(delayTime);
}

void analysePulse(int value){
    if (pNeedCalibration) {
        if (!pCalibrating) {
            pCalibrating = true;
            pCalibrationTimeStart = millis();
            pMaxValue = 0;
            pAverageValue = 0;
            pCalibrationAverageAmount = 0;
            pCalibrationAverageCount = 0;
            send(MESSAGE_PULSE, -2);
        }
        pNeedCalibration = false;
    }
    if (pCalibrating) {
        if (value > pMaxValue) pMaxValue = value;
        pCalibrationAverageAmount += value;
        pCalibrationAverageCount++;
        if (millis() > pCalibrationTimeStart + pCalibrationDuration) { // calibration finished
            pCalibrating = false;
            pAverageValue = int(pCalibrationAverageAmount / pCalibrationAverageCount);
            pMaxValueTime = 0;
            pAverageCrossed = false;
            for (int i = 0; i < 6; i++) {
                pLastValues[i] = -1;
            }
            send(MESSAGE_PULSE, -1);
        }
    } else {
        if (!pAverageCrossed && abs(pAverageValue - value) < AVERAGE_TOLERANCE) {
            pAverageCrossed = true;
        } else if (pAverageCrossed && abs(pMaxValue - value) < MAX_TOLERANCE) {
            pAverageCrossed = false;
            if (pMaxValueTime == 0) {
                pMaxValueTime = millis();
            } else {
                for (int i = 4; i >= 0; i--) {
                    pLastValues[i + 1] = pLastValues[i];
                }
                pLastValues[0] = int(float(60000) / float(millis() - pMaxValueTime));
                int pValue = 0;
                int counter = 0;
                for (int i = 0; i < 6; i++) {
                    if (pLastValues[i] >= 0) {
                        pValue += pLastValues[i];
                        counter++;
                    }
                }
                if (counter > 0) {
                    pValue /= counter;
                } else {
                    pValue = -1;
                }
                send(MESSAGE_PULSE, pValue);
                pMaxValueTime = millis();
            }
        }
    }
}
void send(byte flag, int data){
    Serial.write(flag);
    Serial.write(byte(data));
    Serial.write(byte(data >> 8));
}

