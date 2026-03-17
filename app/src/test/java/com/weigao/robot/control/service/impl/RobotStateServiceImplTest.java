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
import com.weigao.robot.control.model.ChargingState;
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
    private ChargingRuntimeBridge.Listener chargingStateListener;

    @Mock
    private Context mockContext;

    @Mock
    private ChargingRuntimeBridge chargingRuntimeBridge;

    @Mock
    private IResultCallback<Void> voidCallback;

    @Mock
    private IResultCallback<RobotState> stateCallback;

    @Mock
    private IStateCallback observerCallback;

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
        when(chargingRuntimeBridge.getCurrentState()).thenReturn(new ChargingState());

        robotStateService = new RobotStateServiceImpl(mockContext, chargingRuntimeBridge);

        ArgumentCaptor<PeanutRuntime.Listener> runtimeCaptor = ArgumentCaptor.forClass(PeanutRuntime.Listener.class);
        verify(mockedPeanutRuntime).registerListener(runtimeCaptor.capture());
        runtimeListener = runtimeCaptor.getValue();

        ArgumentCaptor<ChargingRuntimeBridge.Listener> chargingCaptor =
                ArgumentCaptor.forClass(ChargingRuntimeBridge.Listener.class);
        verify(chargingRuntimeBridge).addListener(chargingCaptor.capture());
        chargingStateListener = chargingCaptor.getValue();
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
    public void initRegistersListenersAndStartsWithoutImplicitLocalization() {
        verify(mockedPeanutRuntime).registerListener(any(PeanutRuntime.Listener.class));
        verify(mockedPeanutRuntime).start();
        verify(mockedPeanutRuntime, never()).location();
    }

    @Test
    public void registerCallbackImmediatelyPushesBatteryAndChargingState() {
        robotStateService.registerCallback(observerCallback);

        verify(observerCallback).onStateChanged(any(RobotState.class));
        verify(observerCallback).onBatteryLevelChanged(anyInt());
        verify(observerCallback).onChargingStateChanged(any(ChargingState.class));
    }

    @Test
    public void getChargingStateReturnsBridgeSnapshot() {
        ChargingState chargingState = new ChargingState();
        chargingState.setCharging(true);
        when(chargingRuntimeBridge.getCurrentState()).thenReturn(chargingState);

        IResultCallback<ChargingState> callback = mock(IResultCallback.class);
        robotStateService.getChargingState(callback);

        verify(callback).onSuccess(chargingState);
    }

    @Test
    public void chargingStateUpdateNotifiesChargingAndStateCallbacks() {
        robotStateService.registerCallback(observerCallback);
        clearInvocations(observerCallback);

        ChargingState chargingState = new ChargingState();
        chargingState.setCharging(true);
        chargingStateListener.onChargingStateChanged(chargingState);

        verify(observerCallback).onChargingStateChanged(any(ChargingState.class));
        verify(observerCallback).onStateChanged(any(RobotState.class));
    }

    @Test
    public void getRobotStateReadsRuntimeSnapshot() {
        RuntimeInfo runtimeInfo = mock(RuntimeInfo.class);
        when(runtimeInfo.getPower()).thenReturn(99);
        when(runtimeInfo.getWorkMode()).thenReturn(1);
        when(runtimeInfo.isEmergencyOpen()).thenReturn(true);
        when(runtimeInfo.getMotorStatus()).thenReturn(2);
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(runtimeInfo);

        robotStateService.getRobotState(stateCallback);

        verify(stateCallback).onSuccess(any(RobotState.class));
    }

    @Test
    public void performLocalizationWaitsForSuccessEvent() {
        clearInvocations(mockedPeanutRuntime, voidCallback);

        robotStateService.performLocalization(voidCallback);

        verify(mockedPeanutRuntime).location();
        verify(voidCallback, never()).onSuccess(null);
        verify(voidCallback, never()).onError(any(ApiError.class));

        runtimeListener.onEvent(10016, 1);

        verify(voidCallback).onSuccess(null);
    }

    @Test
    public void performLocalizationFailsOnRuntimeErrorEvent() {
        clearInvocations(mockedPeanutRuntime, voidCallback);

        robotStateService.performLocalization(voidCallback);
        runtimeListener.onEvent(10000, null);

        verify(voidCallback).onError(any(ApiError.class));
        verify(voidCallback, never()).onSuccess(null);
    }

    @Test
    public void performLocalizationReusesInFlightRequest() {
        clearInvocations(mockedPeanutRuntime);
        IResultCallback<Void> secondCallback = mock(IResultCallback.class);

        robotStateService.performLocalization(voidCallback);
        robotStateService.performLocalization(secondCallback);

        verify(mockedPeanutRuntime).location();

        runtimeListener.onEvent(10016, 1);

        verify(voidCallback).onSuccess(null);
        verify(secondCallback).onSuccess(null);
    }

    @Test
    public void getBatteryLevelReadsRuntimePower() {
        RuntimeInfo runtimeInfo = mock(RuntimeInfo.class);
        when(runtimeInfo.getPower()).thenReturn(75);
        when(mockedPeanutRuntime.getRuntimeInfo()).thenReturn(runtimeInfo);

        IResultCallback<Integer> callback = mock(IResultCallback.class);
        robotStateService.getBatteryLevel(callback);

        verify(callback).onSuccess(75);
    }

    @Test
    public void unregisterCallbackRemovesObserver() {
        robotStateService.registerCallback(observerCallback);
        robotStateService.unregisterCallback(observerCallback);
    }

    @Test
    public void releaseRemovesRuntimeAndChargingListeners() {
        robotStateService.release();

        verify(mockedPeanutRuntime).removeListener(any(PeanutRuntime.Listener.class));
        verify(chargingRuntimeBridge).removeListener(any(ChargingRuntimeBridge.Listener.class));
    }
}
