package com.weigao.robot.control.service.impl;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.weigao.robot.control.callback.IResultCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * ChargerServiceImpl 的集成测试。
 */
@RunWith(AndroidJUnit4.class)
public class ChargerServiceInstrumentedTest {

    private ChargerServiceImpl mService;

    @Mock
    private PeanutCharger mMockSdk;

    @Mock
    private IResultCallback<Void> mMockCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mService = new ChargerServiceImpl(appContext) {
            @Override
            protected PeanutCharger createPeanutCharger(PeanutCharger.Builder builder) {
                return mMockSdk;
            }
        };
    }

    /**
     * 测试：初始化是否调用了 execute
     */
    @Test
    public void testInit_CallsExecute() {
        verify(mMockSdk).execute();
    }

    /**
     * 测试：自动充电
     */
    @Test
    public void testStartAutoCharge() {
        mService.startAutoCharge(mMockCallback);
        verify(mMockSdk).performAction(PeanutCharger.CHARGE_ACTION_AUTO);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：手动充电
     */
    @Test
    public void testStartManualCharge() {
        mService.startManualCharge(mMockCallback);
        verify(mMockSdk).performAction(PeanutCharger.CHARGE_ACTION_MANUAL);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：适配器充电
     */
    @Test
    public void testStartAdapterCharge() {
        mService.startAdapterCharge(mMockCallback);
        verify(mMockSdk).performAction(PeanutCharger.CHARGE_ACTION_ADAPTER);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：停止充电
     */
    @Test
    public void testStopCharge() {
        mService.stopCharge(mMockCallback);
        verify(mMockSdk).performAction(PeanutCharger.CHARGE_ACTION_STOP);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：执行指定充电动作
     */
    @Test
    public void testPerformAction() {
        int action = PeanutCharger.CHARGE_ACTION_AUTO;
        mService.performAction(action, mMockCallback);
        // performAction内部会映射到SDK常量，验证至少调用了一次
        verify(mMockSdk, atLeastOnce()).performAction(anyInt());
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：设置充电桩ID
     */
    @Test
    public void testSetChargePile() {
        int pileId = 5;
        mService.setChargePile(pileId, mMockCallback);
        verify(mMockSdk).setPile(pileId);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：获取充电桩ID
     */
    @Test
    public void testGetChargePile() {
        int expectedPileId = 3;
        when(mMockSdk.getPile()).thenReturn(expectedPileId);

        IResultCallback<Integer> callback = mock(IResultCallback.class);
        mService.getChargePile(callback);

        verify(mMockSdk).getPile();
        verify(callback).onSuccess(expectedPileId);
    }

    /**
     * 测试：获取电量（从本地缓存）
     */
    @Test
    public void testGetBatteryLevel() {
        // 注意：getBatteryLevel从本地chargerInfo对象获取，不直接调用SDK
        IResultCallback<Integer> callback = mock(IResultCallback.class);
        mService.getBatteryLevel(callback);
        verify(callback).onSuccess(anyInt());
    }

    /**
     * 测试：查询是否正在充电（从本地状态）
     */
    @Test
    public void testIsCharging() {
        // 注意：isCharging从本地变量获取，不直接调用SDK
        IResultCallback<Boolean> callback = mock(IResultCallback.class);
        mService.isCharging(callback);
        verify(callback).onSuccess(any());
    }

    /**
     * 测试：回调注册和注销
     */
    @Test
    public void testRegisterAndUnregisterCallback() {
        com.weigao.robot.control.callback.IChargerCallback mockChargerCallback = mock(
                com.weigao.robot.control.callback.IChargerCallback.class);
        mService.registerCallback(mockChargerCallback);
        mService.unregisterCallback(mockChargerCallback);
    }

    /**
     * 测试：释放资源
     */
    @Test
    public void testRelease() {
        mService.release();
        verify(mMockSdk).release();
    }
}
