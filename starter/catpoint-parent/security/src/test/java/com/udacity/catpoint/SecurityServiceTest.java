package com.udacity.catpoint;

import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService serviceUnderTest;

    @Mock
    private SecurityRepository mockSecurityRepository;

    @Mock
    private FakeImageService mockFakeImageService;

    @BeforeEach
    public void setUp() {
        serviceUnderTest = new SecurityService(mockSecurityRepository, mockFakeImageService);
    }

    private static Stream<Arguments> sensorsDeactivation() {
        return Stream.of(
                arguments(new Sensor("Sensor 1", SensorType.DOOR, true), new Sensor("Sensor 2", SensorType.WINDOW,true)),
                arguments(new Sensor("Sensor 1", SensorType.DOOR, true), new Sensor("Sensor 2", SensorType.WINDOW, true))
        );
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
	@DisplayName("CaseNo.01")
    public void whenAlarmIsArmedAndSensorActivates_thenPendingAlarmIsSet(ArmingStatus armingStatus) {
        Sensor sensor = new Sensor("New sensor", SensorType.WINDOW, false);
        when(mockSecurityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(mockSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        SecurityService spyService = spy(serviceUnderTest);
        spyService.changeSensorActivationStatus(sensor, true);

        verify(mockSecurityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("CaseNo.02")
    public void whenPendingAlarmAndSensorActivates_thenSetAlarm(ArmingStatus armingStatus) {
        Sensor sensor = new Sensor("New sensor", SensorType.DOOR, false);
        when(mockSecurityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(mockSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        SecurityService spyService = spy(serviceUnderTest);
        spyService.changeSensorActivationStatus(sensor, true);

        verify(mockSecurityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("CaseNo.03")
    public void whenPendingAlarmAndAllSensorsInactive_thenSetToNoAlarm() {
        Sensor sensor = new Sensor("New sensor", SensorType.WINDOW, false);

        when(mockSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        SecurityService spyService = spy(serviceUnderTest);
        spyService.changeSensorActivationStatus(sensor, false);

        verify(mockSecurityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @MethodSource("sensorsDeactivation")
    @DisplayName("CaseNo.04")
    public void whenAlarmIsActive_thenSensorStateChangeDoesNotAffectAlarm(Sensor sensor1,Sensor sensor2) {
        when(mockSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        SecurityService spyService = spy(serviceUnderTest);
        spyService.changeSensorActivationStatus(sensor1, true);
        spyService.changeSensorActivationStatus(sensor2, true);

        verify(mockSecurityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("CaseNo.05")
    public void whenSensorActivatedWhilePending_thenSetToAlarm() {
        Sensor sensor = new Sensor("New sensor", SensorType.DOOR, true);

        when(mockSecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        SecurityService spyService = spy(serviceUnderTest);
        spyService.changeSensorActivationStatus(sensor, true);

        verify(mockSecurityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    @DisplayName("CaseNo.06")
    public void whenSensorDeactivatedWhileInactive_thenNoChangeToAlarmState(AlarmStatus status) {
        Sensor sensor = new Sensor("New sensor", SensorType.WINDOW, false);
        when(mockSecurityRepository.getAlarmStatus()).thenReturn(status);

        SecurityService spyService = spy(serviceUnderTest);
        spyService.changeSensorActivationStatus(sensor, false);

        assertEquals(status, spyService.getAlarmStatus());
    }

    @Test
    @DisplayName("CaseNo.07")
    public void whenCatDetectedWhileArmedHome_thenSetAlarm() {
        when(mockSecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(mockFakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        BufferedImage catImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        serviceUnderTest.processImage(catImage);

        verify(mockSecurityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @MethodSource("sensorsDeactivation")
    @DisplayName("CaseNo.08")
    public void whenNoCatDetectedAndAllSensorsInactive_thenSetToNoAlarm(Sensor sensor1 ,Sensor sensor2) {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);

        when(mockSecurityRepository.getSensors()).thenReturn(sensors);
        lenient().when(mockSecurityRepository.getAlarmStatus()). thenReturn(AlarmStatus.ALARM);
        when(mockFakeImageService.imageContainsCat(any(BufferedImage.class), eq(50.0f))).thenReturn(false);
        serviceUnderTest.processImage(mock(BufferedImage.class));

        verify(mockSecurityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("CaseNo.09")
    public void whenSystemDisarmed_thenSetToNoAlarm() {
        serviceUnderTest.setArmingStatus(ArmingStatus.DISARMED);
        verify(mockSecurityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    @DisplayName("CaseNo.10")
    public void whenSystemArmed_thenAllSensorsInactive(ArmingStatus status) {
        Set<Sensor> sensors = new HashSet<>();
        Sensor sensor1 = new Sensor("Sensor 1", SensorType.DOOR, true);
        sensors.add(sensor1);

        Sensor sensor2 = new Sensor("Sensor 2", SensorType.WINDOW, true);
        sensors.add(sensor2);

        when(mockSecurityRepository.getSensors()).thenReturn(sensors);

        serviceUnderTest.setArmingStatus(status);

        for (Sensor sensor : serviceUnderTest.getSensors()) {
            assertEquals(false, sensor.getActive());
        }
    }

    @Test
    @DisplayName("CaseNo.11")
    public void whenSystemArmedAndCatDetected_thenSetAlarm() {
        when(mockSecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(mockFakeImageService.imageContainsCat(any(BufferedImage.class), eq(50.0f))).thenReturn(true);
        serviceUnderTest.processImage(mock(BufferedImage.class));

        serviceUnderTest.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(mockSecurityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}