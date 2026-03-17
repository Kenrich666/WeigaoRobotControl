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
import com.weigao.robot.control.model.RobotState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RobotStateServiceInstrumentedTest {

    private RobotStateServiceImpl mService;
    private PeanutRuntime.Listener runtimeListener;

    @Mock
    private PeanutRuntime mMockSdk;

    @Mock
    private IResultCallback<RobotState> mStateCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mService = new RobotStateServiceImpl(appContext) {
            @Override
            protected PeanutRuntime getPeanutRuntimeInstance() {
                return mMockSdk;
            }
        };

        ArgumentCaptor<PeanutRuntime.Listener> captor = ArgumentCaptor.forClass(PeanutRuntime.Listener.class);
        verify(mMockSdk).registerListener(captor.capture());
        runtimeListener = captor.getValue();
    }

    @Test
    public void testInit_RegistersListenerAndStarts() {
        verify(mMockSdk).registerListener(any(PeanutRuntime.Listener.class));
        verify(mMockSdk).start();
        verify(mMockSdk, never()).location();
    }

    @Test
    public void testSetWorkMode() {
        int mode = 1;
        mService.setWorkMode(mode, mock(IResultCallback.class));
        verify(mMockSdk).setWorkMode(mode);
    }

    @Test
    public void testGetRobotState() {
        clearInvocations(mMockSdk);

        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getPower()).thenReturn(80);
        when(mMockSdk.getRuntimeInfo()).thenReturn(mockInfo);

        mService.getRobotState(mStateCallback);

        verify(mMockSdk).getRuntimeInfo();
        verify(mStateCallback).onSuccess(any(RobotState.class));
    }

    @Test
    public void testGetBatteryLevel() {
        clearInvocations(mMockSdk);

        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getPower()).thenReturn(75);
        when(mMockSdk.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Integer> callback = mock(IResultCallback.class);
        mService.getBatteryLevel(callback);

        verify(callback).onSuccess(any(Integer.class));
    }

    @Test
    public void testIsScramButtonPressed() {
        clearInvocations(mMockSdk);

        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.isEmergencyOpen()).thenReturn(true);
        when(mMockSdk.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Boolean> callback = mock(IResultCallback.class);
        mService.isScramButtonPressed(callback);

        verify(callback).onSuccess(true);
    }

    @Test
    public void testSetEmergencyEnabled() {
        IResultCallback<Void> callback = mock(IResultCallback.class);
        mService.setEmergencyEnabled(true, callback);
        verify(mMockSdk).setEmergencyEnable(true);
        verify(callback).onSuccess(null);
    }

    @Test
    public void testPerformLocalizationWaitsForEvent() {
        clearInvocations(mMockSdk);

        IResultCallback<Void> callback = mock(IResultCallback.class);
        mService.performLocalization(callback);

        verify(mMockSdk).location();
        verify(callback, never()).onSuccess(null);

        runtimeListener.onEvent(10016, 1);

        verify(callback).onSuccess(null);
    }

    @Test
    public void testGetTotalOdometer() {
        clearInvocations(mMockSdk);

        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getTotalOdo()).thenReturn(1234.5);
        when(mMockSdk.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Double> callback = mock(IResultCallback.class);
        mService.getTotalOdometer(callback);

        verify(callback).onSuccess(1234.5);
    }

    @Test
    public void testGetRobotIp() {
        clearInvocations(mMockSdk);

        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getRobotIp()).thenReturn("192.168.1.100");
        when(mMockSdk.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<String> callback = mock(IResultCallback.class);
        mService.getRobotIp(callback);

        verify(callback).onSuccess("192.168.1.100");
    }

    @Test
    public void testRegisterAndUnregisterCallback() {
        com.weigao.robot.control.callback.IStateCallback mockStateCallback = mock(
                com.weigao.robot.control.callback.IStateCallback.class);
        mService.registerCallback(mockStateCallback);
        mService.unregisterCallback(mockStateCallback);
    }

    @Test
    public void testRelease() {
        mService.release();
        verify(mMockSdk).removeListener(any(PeanutRuntime.Listener.class));
    }
}
