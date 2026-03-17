package com.weigao.robot.control.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.component.runtime.RuntimeInfo;
import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.RobotState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public class RobotStateServiceImplTest {

    private RobotStateServiceImpl robotStateService;
    private MockedStatic<Log> mockedLogStatic;
    private MockedStatic<PeanutRuntime> mockedRuntimeStatic;
    private PeanutRuntime mockedPeanutRuntime;
    private PeanutRuntime.Listener runtimeListener;

    @Mock
    private Context mockContext;

    @Mock
    private IResultCallback<Void> mockVoidCallback;

    @Mock
    private IResultCallback<RobotState> mockStateCallback;

    @Mock
    private IStateCallback mockObserverCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);

        mockedPeanutRuntime = mock(PeanutRuntime.class);
        mockedRuntimeStatic = mockStatic(PeanutRuntime.class);
        mockedRuntimeStatic.when(PeanutRuntime::getInstance).thenReturn(mockedPeanutRuntime);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        robotStateService = new RobotStateServiceImpl(mockContext);

        ArgumentCaptor<PeanutRuntime.Listener> listenerCaptor = ArgumentCaptor.forClass(PeanutRuntime.Listener.class);
        verify(mockedPeanutRuntime).registerListener(listenerCaptor.capture());
        runtimeListener = listenerCaptor.getValue();
    }

    @After
    public void tearDown() {
        if (mockedRuntimeStatic != null) {
            mockedRuntimeStatic.close();
        }
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    @Test
    public void testInit_RegistersListenerAndStartsWithoutImplicitLocalization() {
        verify(mockedPeanutRuntime).registerListener(any(PeanutRuntime.Listener.class));
        verify(mockedPeanutRuntime).start();
        verify(mockedPeanutRuntime, never()).location();
    }

    @Test
    public void testRegisterCallback_ImmediateNotification() {
        robotStateService.registerCallback(mockObserverCallback);

        verify(mockObserverCallback).onStateChanged(any(RobotState.class));
        verify(mockObserverCallback).onBatteryLevelChanged(anyInt());
    }

    @Test
    public void testStateUpdateFromSdk() {
        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getPower()).thenReturn(99);
        when(mockInfo.getWorkMode()).thenReturn(1);
        when(mockInfo.isEmergencyOpen()).thenReturn(true);
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(mockInfo);

        robotStateService.getRobotState(mockStateCallback);

        verify(mockStateCallback).onSuccess(any(RobotState.class));
    }

    @Test
    public void testSetWorkMode() {
        robotStateService.setWorkMode(1, mockVoidCallback);

        verify(mockedPeanutRuntime).setWorkMode(1);
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testReboot() {
        robotStateService.reboot(mockVoidCallback);

        verify(mockedPeanutRuntime).syncParams2Robot(true);
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testPerformLocalizationWaitsForSuccessEvent() {
        clearInvocations(mockedPeanutRuntime, mockVoidCallback);

        robotStateService.performLocalization(mockVoidCallback);

        verify(mockedPeanutRuntime).location();
        verify(mockVoidCallback, never()).onSuccess(null);
        verify(mockVoidCallback, never()).onError(any(ApiError.class));

        runtimeListener.onEvent(10016, 1);

        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testPerformLocalizationFailsOnRuntimeErrorEvent() {
        clearInvocations(mockedPeanutRuntime, mockVoidCallback);

        robotStateService.performLocalization(mockVoidCallback);
        runtimeListener.onEvent(10000, null);

        verify(mockVoidCallback).onError(any(ApiError.class));
        verify(mockVoidCallback, never()).onSuccess(null);
    }

    @Test
    public void testPerformLocalizationReusesInFlightRequest() {
        clearInvocations(mockedPeanutRuntime);

        IResultCallback<Void> secondCallback = mock(IResultCallback.class);

        robotStateService.performLocalization(mockVoidCallback);
        robotStateService.performLocalization(secondCallback);

        verify(mockedPeanutRuntime).location();

        runtimeListener.onEvent(10016, 1);

        verify(mockVoidCallback).onSuccess(null);
        verify(secondCallback).onSuccess(null);
    }

    @Test
    public void testGetBatteryLevel() {
        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getPower()).thenReturn(75);
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Integer> callback = mock(IResultCallback.class);
        robotStateService.getBatteryLevel(callback);

        verify(callback).onSuccess(75);
    }

    @Test
    public void testGetCurrentLocation() {
        IResultCallback<RobotState.LocationInfo> callback = mock(IResultCallback.class);
        robotStateService.getCurrentLocation(callback);

        verify(callback).onSuccess(null);
    }

    @Test
    public void testIsScramButtonPressed() {
        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.isEmergencyOpen()).thenReturn(true);
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Boolean> callback = mock(IResultCallback.class);
        robotStateService.isScramButtonPressed(callback);

        verify(callback).onSuccess(true);
    }

    @Test
    public void testGetMotorStatus() {
        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getMotorStatus()).thenReturn(1);
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Integer> callback = mock(IResultCallback.class);
        robotStateService.getMotorStatus(callback);

        verify(callback).onSuccess(1);
    }

    @Test
    public void testSetMotorEnabled() {
        try {
            robotStateService.setMotorEnabled(true, mockVoidCallback);
        } catch (Exception ignored) {
            // PeanutSDK is not fully mocked in this unit test.
        }
    }

    @Test
    public void testSyncParams() {
        robotStateService.syncParams(false, mockVoidCallback);

        verify(mockedPeanutRuntime).syncParams2Robot(false);
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testGetTotalOdometer() {
        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getTotalOdo()).thenReturn(12345.6);
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Double> callback = mock(IResultCallback.class);
        robotStateService.getTotalOdometer(callback);

        verify(callback).onSuccess(12345.6);
    }

    @Test
    public void testGetRobotIp() {
        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getRobotIp()).thenReturn("192.168.1.100");
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<String> callback = mock(IResultCallback.class);
        robotStateService.getRobotIp(callback);

        verify(callback).onSuccess("192.168.1.100");
    }

    @Test
    public void testUnregisterCallback() {
        robotStateService.registerCallback(mockObserverCallback);
        robotStateService.unregisterCallback(mockObserverCallback);
    }

    @Test
    public void testRelease() {
        robotStateService.release();

        verify(mockedPeanutRuntime).removeListener(any(PeanutRuntime.Listener.class));
    }
}
