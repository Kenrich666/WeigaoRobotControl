package com.weigao.robot.control.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.weigao.robot.control.callback.IChargerCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.ChargerInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * ChargerServiceImpl 的单元测试类。
 * 
 * 本类旨在验证充电服务逻辑的正确性，并解决以下技术难点：
 * 1. 模拟 Android 静态 Log 类：Log 类在纯单元测试环境下不可用，通过 MockedStatic 拦截。
 * 2. 模拟 Service 内部 new 出来的对象：通过 MockedConstruction 拦截 PeanutCharger.Builder
 * 的实例化过程。
 * 3. 模拟 Builder 模式：对 Builder 的每一个链式调用（如 setPile, setListener）进行模拟，确保最后返回正确的
 * mock 对象。
 */
public class ChargerServiceImplTest {

    // 被测试的服务实例
    private ChargerServiceImpl chargerService;

    // Log 类的静态 Mock 容器，用于处理所有的日志输出调用
    private MockedStatic<Log> mockedLogStatic;

    // PeanutCharger.Builder 的构造函数 Mock 容器
    private MockedConstruction<PeanutCharger.Builder> mockedBuilderConstruction;

    // 模拟的 SDK 核心对象，Service 的操作最终会传导到这个实例上
    private PeanutCharger mockedPeanutCharger;

    // 模拟的 Builder 实例
    private PeanutCharger.Builder mockedBuilder;

    // 模拟 Context 对象，用于满足 Service 的初始化需求
    @Mock
    private Context mockContext;

    // 各种回调接口的 Mock，用于验证业务完成后的通知机制
    @Mock
    private IResultCallback<Void> mockVoidCallback;

    @Mock
    private IResultCallback<Integer> mockIntegerCallback;

    @Mock
    private IResultCallback<ChargerInfo> mockChargerInfoCallback;

    @Mock
    private IResultCallback<Boolean> mockBooleanCallback;

    /**
     * 测试前的初始化工作。
     * 负责配置所有 Mock 容器和模拟行为。
     */
    @Before
    public void setUp() {
        // 初始化标注了 @Mock 的成员变量
        MockitoAnnotations.openMocks(this);

        // [核心配置 1]：拦截 android.util.Log。
        // 因为 Service 构造函数和方法中大量使用了 Log，若不拦截，在 JUnit 环境中会抛出 RuntimeException。
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);

        // [核心配置 2]：配置 SDK Builder 的模拟过程。
        mockedPeanutCharger = mock(PeanutCharger.class);
        mockedBuilder = mock(PeanutCharger.Builder.class);

        // 使用 mockConstruction 拦截 Service 内部的 "new PeanutCharger.Builder()" 行为。
        mockedBuilderConstruction = mockConstruction(PeanutCharger.Builder.class, (mock, context) -> {
            // 当 Service 执行 Builder 的链式方法时，返回 mock 对象自身以支持链式调用
            when(mock.setPile(anyInt())).thenReturn(mock);
            when(mock.setListener(any())).thenReturn(mock);
            // 最后 build() 时返回我们提前定义好的 mockedPeanutCharger
            when(mock.build()).thenReturn(mockedPeanutCharger);
        });

        // 模拟 Context 行为，Service 构造函数会调用 getApplicationContext()
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        // [实例化被测对象]：此时 Service 会在构造函数内执行初始化 SDK 的逻辑，
        // 我们上面配置的 Mock 逻辑将确保整个过程顺利完成。
        chargerService = new ChargerServiceImpl(mockContext);
    }

    /**
     * 测试后的清理工作。
     * 必须关闭静态和构造函数 Mock，否则会影响后续测试类的运行。
     */
    @After
    public void tearDown() {
        if (chargerService != null) {
            chargerService.release();
        }
        if (mockedBuilderConstruction != null) {
            mockedBuilderConstruction.close();
        }
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    /**
     * 测试：开始自动充电。
     * 验证 Service 是否调用了 SDK 对应的 performAction 方法，并使用了 CHARGE_ACTION_AUTO 常量。
     */
    @Test
    public void testStartAutoCharge() {
        chargerService.startAutoCharge(mockVoidCallback);

        // 验证：SDK 是否收到了自动充电动作指令
        verify(mockedPeanutCharger).performAction(PeanutCharger.CHARGE_ACTION_AUTO);
        // 验证：回调是否被成功调用
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试：开始手动充电。
     * 验证 Service 是否正确传递了手动充电动作。
     */
    @Test
    public void testStartManualCharge() {
        chargerService.startManualCharge(mockVoidCallback);
        verify(mockedPeanutCharger).performAction(PeanutCharger.CHARGE_ACTION_MANUAL);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试：开始适配器充电（直接插线充电）。
     */
    @Test
    public void testStartAdapterCharge() {
        chargerService.startAdapterCharge(mockVoidCallback);
        verify(mockedPeanutCharger).performAction(PeanutCharger.CHARGE_ACTION_ADAPTER);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试：停止充电。
     * 同时验证 Service 内部的状态维护逻辑（停止后 isCharging 应该为 false）。
     */
    @Test
    public void testStopCharge() {
        chargerService.stopCharge(mockVoidCallback);

        // 验证 SDK 层动作
        verify(mockedPeanutCharger).performAction(PeanutCharger.CHARGE_ACTION_STOP);
        verify(mockVoidCallback).onSuccess(null);

        // 验证业务状态更新逻辑：停止充电后，再次查询充电状态应返回 false
        chargerService.isCharging(mockBooleanCallback);
        verify(mockBooleanCallback).onSuccess(false);
    }

    /**
     * 测试：设置充电桩 ID。
     * 验证 Service 是否将桩 ID 同步到了底层 SDK。
     */
    @Test
    public void testSetChargePile() {
        int pileId = 101;
        chargerService.setChargePile(pileId, mockVoidCallback);

        // 验证桩 ID 传递
        verify(mockedPeanutCharger).setPile(pileId);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试：获取充电桩 ID。
     * 验证 Service 能否从 SDK 实时读取充电桩信息。
     */
    @Test
    public void testGetChargePile() {
        // 配置模拟返回值
        int expectedPile = 5;
        when(mockedPeanutCharger.getPile()).thenReturn(expectedPile);

        chargerService.getChargePile(mockIntegerCallback);

        // 验证回调获得的值是否与预设一致
        verify(mockIntegerCallback).onSuccess(expectedPile);
    }

    /**
     * 测试：获取电池电量。
     */
    @Test
    public void testGetBatteryLevel() {
        // 验证初始电量获取逻辑
        chargerService.getBatteryLevel(mockIntegerCallback);
        verify(mockIntegerCallback).onSuccess(0);
    }

    /**
     * 测试：回调接口的注册与注销。
     */
    @Test
    public void testRegisterUnregisterCallback() {
        IChargerCallback callback = mock(IChargerCallback.class);
        chargerService.registerCallback(callback);
        chargerService.unregisterCallback(callback);
    }

    /**
     * 测试：资源释放。
     * 验证 Service release 时是否调用了 SDK 的 release。
     */
    @Test
    public void testRelease() {
        chargerService.release();
        verify(mockedPeanutCharger).release();
    }

    /**
     * 测试：获取充电器详细信息。
     */
    @Test
    public void testGetChargerInfo() {
        chargerService.getChargerInfo(mockChargerInfoCallback);

        // 验证回调被调用
        verify(mockChargerInfoCallback).onSuccess(any());
    }

    /**
     * 测试：初始状态下查询充电状态返回 false。
     * <p>
     * 注意：真实的 isCharging 状态是通过 SDK 的 onChargerInfoChanged 回调异步更新的。
     * 在单元测试中，由于 SDK 回调未被触发，状态保持为初始值 false。
     * </p>
     */
    @Test
    public void testIsCharging_InitialState_False() {
        IResultCallback<Boolean> mockBoolCallback = mock(IResultCallback.class);
        chargerService.isCharging(mockBoolCallback);

        // 验证初始充电状态为 false
        verify(mockBoolCallback).onSuccess(false);
    }

    /**
     * 测试：SDK 抛异常时的错误处理。
     */
    @Test
    public void testStartAutoCharge_Exception() {
        // 模拟 SDK 抛出异常
        doThrow(new RuntimeException("SDK Error")).when(mockedPeanutCharger).performAction(anyInt());

        IResultCallback<Void> callback = mock(IResultCallback.class);
        chargerService.startAutoCharge(callback);

        // 验证错误回调被调用
        verify(callback).onError(any());
    }

    /**
     * 测试：多次注册和注销回调。
     */
    @Test
    public void testMultipleRegisterUnregisterCallback() {
        IChargerCallback callback1 = mock(IChargerCallback.class);
        IChargerCallback callback2 = mock(IChargerCallback.class);

        chargerService.registerCallback(callback1);
        chargerService.registerCallback(callback2);
        chargerService.unregisterCallback(callback1);
        chargerService.unregisterCallback(callback2);
        // 验证不抛出异常
    }
}
