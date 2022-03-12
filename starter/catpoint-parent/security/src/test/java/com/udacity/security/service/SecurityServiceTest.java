package com.udacity.security.service;

import com.udacity.image.service.FakeImageService;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.Set;

import static com.udacity.security.data.AlarmStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityServiceTest {
    private SecurityService securityService;
    private Set<Sensor> setOffSensors;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private FakeImageService imageService;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        createSensorSet();
    }

    private void createSensorSet() {
        setOffSensors = Set.of(
                new Sensor("window", SensorType.WINDOW),
                new Sensor("door", SensorType.DOOR),
                new Sensor("motion", SensorType.MOTION)
        );
    }

    // Test 1
    // If alarm is armed
    // and a sensor becomes activated,
    // put the system into pending alarm status.
    @Test
    void armingStatusArmed_sensorActivated_returnPendingAlarm() {
        Sensor sensor = setOffSensors.iterator().next();
        when(securityService.getAlarmStatus()).thenReturn(NO_ALARM);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(PENDING_ALARM);
    }

    // Test 2
    // If alarm is armed
    // and a sensor becomes activated
    // and the system is already pending alarm,
    // set the alarm status to alarm.
    @Test
    void armingNotDisarmed_sensorActivated_alarmPendingAlarm_returnAlarm() {
        Sensor sensor = setOffSensors.iterator().next();
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityService.getAlarmStatus()).thenReturn(PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(ALARM);
    }

    // Test 3
    // If pending alarm
    // and all sensors are inactive,
    // return to no alarm state.
    @Test
    void alarmPendingAlarm_allSensorsInactive_returnNoAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(PENDING_ALARM);

        for (Sensor sensor : setOffSensors) {
            securityService.changeSensorActivationStatus(sensor);
        }

        verify(securityRepository, times(setOffSensors.size())).setAlarmStatus(NO_ALARM);
    }

    // Test 4
    // if alarm is active,
    // change in sensor state
    // should not affect the alarm state.
    @Test
    void alarmAlarm_sensorChange_returnAlarm() {
        Sensor sensor = setOffSensors.iterator().next();
        when(securityService.getAlarmStatus()).thenReturn(ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(PENDING_ALARM);
    }

    // Test 5
    // If a sensor is activated while already active
    // and the system is in pending state,
    // change it to alarm state.
    @Test
    void alarmPendingAlarm_activateActiveSensor_returnAlarm() {
        Sensor sensor = setOffSensors.iterator().next();
        sensor.setActive(true);
        when(securityService.getAlarmStatus()).thenReturn(PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository).setAlarmStatus(ALARM);
    }

    // Test 6
    // If a sensor is deactivated while already inactive,
    // make no changes to the alarm state.
    @Test
    void deactivateInactiveSensor_returnNoChangeAlarmState() {
        Sensor sensor = setOffSensors.iterator().next();
        sensor.setActive(false);
        AlarmStatus systemState = securityService.getAlarmStatus();

        securityService.changeSensorActivationStatus(sensor);

        assertEquals(systemState, securityService.getAlarmStatus());
    }

    // Test 7
    // If the image service identifies an image containing a cat
    // while the system is armed-home,
    // put the system into alarm status.
    @Test
    void systemArmedHome_CatImageDetected_SystemAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(ALARM);
    }

    // Test 8
    // If the image service identifies an image that does not contain a cat,
    // change the status to no alarm
    // as long as the sensors are not active.
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void changeSensorActivationStatus_ImageNotCat_NoAlarmSensorsNotActive(Boolean statusOfSensor) {
        Sensor sensor = setOffSensors.stream().findFirst().get();
        sensor.setActive(statusOfSensor);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        when(securityService.getSensors()).thenReturn(setOffSensors);

        securityService.processImage(mock(BufferedImage.class));

        if (statusOfSensor) {
            verify(securityRepository, never()).setAlarmStatus(NO_ALARM);
        } else {
            verify(securityRepository).setAlarmStatus(NO_ALARM);
        }

    }

    // Test 9
    // If the system is disarmed,
    // set the status to no alarm.
    @Test
    void changeSensorActivationStatus_SystemDisarmed_NoAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(NO_ALARM);
    }

    // Test 10
    // if the system is armed,
    // reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(
            value = ArmingStatus.class,
            names = {"ARMED_HOME", "ARMED_AWAY"}
    )
    void armingArmed_returnAllSensorsInactive(ArmingStatus armingStatus) {
        for (Sensor sensor : setOffSensors) {
            sensor.setActive(true);
        }
        when(securityService.getSensors()).thenReturn(setOffSensors);

        securityService.setArmingStatus(armingStatus);

        for (Sensor sensor : setOffSensors) {
            assertEquals(false, sensor.getActive());
        }
    }

    // Test 11
    // If the system is armed-home
    // while the camera shows a cat,
    // set the alarm status to alarm
    @Test
    void changeSensorActivationStatus_SystemArmedHomeCameraShowCat_Alarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(ALARM);
    }
}