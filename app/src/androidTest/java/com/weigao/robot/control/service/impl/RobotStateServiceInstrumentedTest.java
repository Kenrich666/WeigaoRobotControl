package com.weigao.robot.control.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.component.runtime.RuntimeInfo;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.RobotState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RobotStateServiceInstrumentedTest {

    private RobotStateServiceImpl service;
    private PeanutRuntime.Listener runtimeListener;

    @Mock
    private PeanutRuntime peanutRuntime;

    @Mock
    private ChargingRuntimeBridge chargingRuntimeBridge;

    @Mock
    private IResultCallback<RobotState> robotStateCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        when(chargingRuntimeBridge.getCurrentState()).thenReturn(new ChargingState());

        service = new RobotStateServiceImpl(appContext, chargingRuntimeBridge) {
            @Override
            protected PeanutRuntime getPeanutRuntimeInstance() {
                return peanutRuntime;
            }
        };

        ArgumentCaptor<PeanutRuntime.Listener> captor = ArgumentCaptor.forClass(PeanutRuntime.Listener.class);
        verify(peanutRuntime).registerListener(captor.capture());
        runtimeListener = captor.getValue();
    }

    @Test
    public void initRegistersListenerAndStarts() {
        verify(peanutRuntime).registerListener(any(PeanutRuntime.Listener.class));
        verify(peanutRuntime).start();
        verify(peanutRuntime, never()).location();
    }

    @Test
    public void getRobotStateReturnsRuntimeSnapshot() {
        clearInvocations(peanutRuntime);

        RuntimeInfo runtimeInfo = mock(RuntimeInfo.class);
        when(runtimeInfo.getPower()).thenReturn(80);
        when(peanutRuntime.getRuntimeInfo()).thenReturn(runtimeInfo);

        service.getRobotState(robotStateCallback);

        verify(peanutRuntime).getRuntimeInfo();
        verify(robotStateCallback).onSuccess(any(RobotState.class));
    }

    @Test
    public void getChargingStateReturnsBridgeSnapshot() {
        ChargingState chargingState = new ChargingState();
        chargingState.setCharging(true);
        when(chargingRuntimeBridge.getCurrentState()).thenReturn(chargingState);

        IResultCallback<ChargingState> callback = mock(IResultCallback.class);
        service.getChargingState(callback);

        verify(callback).onSuccess(chargingState);
    }

    @Test
    public void performLocalizationWaitsForEvent() {
        clearInvocations(peanutRuntime);

        IResultCallback<Void> callback = mock(IResultCallback.class);
        service.performLocalization(callback);

        verify(peanutRuntime).location();
        verify(callback, never()).onSuccess(null);

        runtimeListener.onEvent(10016, 1);

        verify(callback).onSuccess(null);
    }

    @Test
    public void releaseRemovesListeners() {
        service.release();

        verify(peanutRuntime).removeListener(any(PeanutRuntime.Listener.class));
        verify(chargingRuntimeBridge).removeListener(any(ChargingRuntimeBridge.Listener.class));
    }
}
