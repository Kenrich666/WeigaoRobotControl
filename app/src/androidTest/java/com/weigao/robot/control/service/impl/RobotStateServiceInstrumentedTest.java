package com.weigao.robot.control.service.impl;

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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RobotStateServiceImpl 的集成测试。
 */
@RunWith(AndroidJUnit4.class)
public class RobotStateServiceInstrumentedTest {

    private RobotStateServiceImpl mService;

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
    }

    /**
     * 测试：初始化是否注册了监听并启动
     */
    @Test
    public void testInit_RegistersListenerAndStarts() {
        verify(mMockSdk).registerListener(any(PeanutRuntime.Listener.class));
        verify(mMockSdk).start();
    }

    /**
     * 测试：设置工作模式
     */
    @Test
    public void testSetWorkMode() {
        int mode = 1;
        mService.setWorkMode(mode, mock(IResultCallback.class));
        verify(mMockSdk).setWorkMode(mode);
    }

    /**
     * 测试：获取机器人状态
     */
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

    /**
     * 测试：获取电量
     */
    @Test
    public void testGetBatteryLevel() {
        clearInvocations(mMockSdk);

        RuntimeInfo mockInfo = mock(RuntimeInfo.class);
        when(mockInfo.getPower()).thenReturn(75);
        when(mMockSdk.getRuntimeInfo()).thenReturn(mockInfo);

        IResultCallback<Integer> callback = mock(IResultCallback.class);
        mService.getBatteryLevel(callback);

        verify(callback).onSuccess(anyInt());
    }

    /**
     * 测试：查询急停按钮状态
     */
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

    /**
     * 测试：设置急停按钮是否启用
     */
    @Test
    public void testSetEmergencyEnabled() {
        IResultCallback<Void> callback = mock(IResultCallback.class);
        mService.setEmergencyEnabled(true, callback);
        verify(mMockSdk).setEmergencyEnable(true);
        verify(callback).onSuccess(null);
    }

    /**
     * 测试：执行开机定位
     */
    @Test
    public void testPerformLocalization() {
        IResultCallback<Void> callback = mock(IResultCallback.class);
        mService.performLocalization(callback);
        verify(mMockSdk).location();
        verify(callback).onSuccess(null);
    }

    /**
     * 测试：获取总里程
     */
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

    /**
     * 测试：获取机器人IP
     */
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

    /**
     * 测试：回调注册和注销
     */
    @Test
    public void testRegisterAndUnregisterCallback() {
        com.weigao.robot.control.callback.IStateCallback mockStateCallback = mock(
                com.weigao.robot.control.callback.IStateCallback.class);
        mService.registerCallback(mockStateCallback);
        mService.unregisterCallback(mockStateCallback);
    }

    /**
     * 测试：释放资源
     */
    @Test
    public void testRelease() {
        mService.release();
        verify(mMockSdk).removeListener(any(PeanutRuntime.Listener.class));
    }
}
