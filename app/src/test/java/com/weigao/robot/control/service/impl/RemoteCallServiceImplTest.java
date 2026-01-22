package com.weigao.robot.control.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.IRemoteCallService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * RemoteCallServiceImpl 的单元测试类。
 *
 * 核心测试目标：
 * 1. 验证远程呼叫功能的启用/禁用逻辑。
 * 2. 验证呼叫任务的启动流程（设置目标点 -> 启动导航）。
 * 3. 验证取消呼叫时的资源清理（停止定时器、停止导航）。
 *
 * 技术难点：
 * - Handler 模拟：由于 Handler 是 Android 框架类，在单元测试中直接 new 或调用方法会报错。
 * 我们通过 MockedConstruction 拦截其构造函数，确保 Service 持有的是一个 Mock 对象。
 * - 静态方法模拟：处理 Log 和 Looper 的静态调用。
 */
public class RemoteCallServiceImplTest {

    // 待测试的目标服务
    private RemoteCallServiceImpl remoteCallService;

    // 静态 Mock 容器
    private MockedStatic<Log> mockedLogStatic;
    private MockedStatic<Looper> mockedLooperStatic;

    // 构造函数 Mock 容器，专门用于拦截 "new Handler()"
    private MockedConstruction<Handler> mockedHandlerConstruction;

    @Mock
    private Context mockContext;

    @Mock
    private INavigationService mockNavigationService;

    @Mock
    private IResultCallback<Void> mockVoidCallback;

    @Mock
    private IResultCallback<Boolean> mockBooleanCallback;

    /**
     * 测试前的初始化：配置 Mock 环境
     */
    @Before
    public void setUp() {
        // 初始化标注了 @Mock 的成员
        MockitoAnnotations.openMocks(this);

        // [核心配置 1]：模拟 Log 静态方法，防止日志打印导致崩溃
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);

        // [核心配置 2]：模拟 Looper。
        // 因为 Service 构造时执行了 Looper.getMainLooper()，必须模拟返回一个非空的 Looper 对象。
        mockedLooperStatic = mockStatic(Looper.class);
        Looper mockLooper = mock(Looper.class);
        mockedLooperStatic.when(Looper::getMainLooper).thenReturn(mockLooper);

        // [核心配置 3]：模拟 Handler 构造函数（修复 removeCallbacksAndMessages 报错的关键）。
        // 使用 mockConstruction 拦截 Service 内部的 "new Handler(Looper.getMainLooper())"。
        // 这样 Service 持有的 mainHandler 实际上是一个 Mock 对象，调用其方法时不会执行 Android 源码中的抛错逻辑。
        mockedHandlerConstruction = mockConstruction(Handler.class);

        // 模拟 Context 行为
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        // 实例化 Service
        // 此时，内部的 new Handler(...) 将被我们的 mockedHandlerConstruction 拦截并返回 Mock 对象。
        remoteCallService = new RemoteCallServiceImpl(mockContext, mockNavigationService);
    }

    /**
     * 测试后的资源清理：关闭所有静态和构造函数 Mock 容器
     */
    @After
    public void tearDown() {
        if (remoteCallService != null) {
            // 此时调用 release()，内部的 mainHandler.removeCallbacksAndMessages(null)
            // 会执行在 Mock 对象上，不会抛出 "Method ... not mocked" 异常。
            remoteCallService.release();
        }
        if (mockedHandlerConstruction != null) {
            mockedHandlerConstruction.close();
        }
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
        if (mockedLooperStatic != null) {
            mockedLooperStatic.close();
        }
    }

    /**
     * 测试远程呼叫功能的启用状态。
     */
    @Test
    public void testSetRemoteCallEnabled() {
        // 动作：启用功能
        remoteCallService.setRemoteCallEnabled(true, mockVoidCallback);
        // 验证：是否成功回调
        verify(mockVoidCallback).onSuccess(null);

        // 验证内部状态是否真的变为了 true
        remoteCallService.isRemoteCallEnabled(mockBooleanCallback);
        verify(mockBooleanCallback).onSuccess(true);
    }

    /**
     * 测试处理远程呼叫的成功路径。
     */
    @Test
    public void testHandleRemoteCall_Success() {
        // 前提：功能必须先启用
        remoteCallService.setRemoteCallEnabled(true, null);

        int targetId = 10;
        int callType = 1;

        // 执行呼叫
        remoteCallService.handleRemoteCall(targetId, callType, mockVoidCallback);

        // 验证逻辑：
        // 1. Service 应该调用 INavigationService 的 setTargets 方法
        verify(mockNavigationService).setTargets(anyList(), any());

        // 2. 验证任务是否进入了激活状态 (Active)
        remoteCallService.isRemoteCallActive(mockBooleanCallback);
        verify(mockBooleanCallback).onSuccess(true);
    }

    /**
     * 测试功能未启用时的呼叫请求（异常路径）。
     */
    @Test
    public void testHandleRemoteCall_Disabled_Error() {
        // 确保功能处于禁用状态
        remoteCallService.setRemoteCallEnabled(false, null);

        remoteCallService.handleRemoteCall(1, 1, mockVoidCallback);

        // 验证：不应该调用导航服务
        verify(mockNavigationService, never()).setTargets(any(), any());
        // 验证：应该通过 onError 回调返回错误信息
        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试取消远程呼叫任务。
     */
    @Test
    public void testCancelRemoteCall() {
        // 模拟一个正在进行的呼叫任务
        remoteCallService.setRemoteCallEnabled(true, null);
        remoteCallService.handleRemoteCall(1, 1, null);

        // 执行取消操作
        remoteCallService.cancelRemoteCall(mockVoidCallback);

        // 验证：
        // 1. 应该停止当前的导航
        verify(mockNavigationService).stop(any());
        // 2. 任务激活状态应重置为 false
        remoteCallService.isRemoteCallActive(mockBooleanCallback);
        verify(mockBooleanCallback, atLeastOnce()).onSuccess(false);
    }

    /**
     * 测试到达后停留时长的设置功能。
     */
    @Test
    public void testArrivalStayDurationSettings() {
        int duration = 30; // 30秒
        // 设置时长
        remoteCallService.setArrivalStayDuration(duration, mockVoidCallback);
        verify(mockVoidCallback).onSuccess(null);

        // 读取时长并验证是否一致
        IResultCallback<Integer> mockIntCallback = mock(IResultCallback.class);
        remoteCallService.getArrivalStayDuration(mockIntCallback);
        verify(mockIntCallback).onSuccess(duration);
    }

    /**
     * 测试设置到达停留时长启用状态。
     */
    @Test
    public void testSetArrivalStayDurationEnabled() {
        remoteCallService.setArrivalStayDurationEnabled(true, mockVoidCallback);
        verify(mockVoidCallback).onSuccess(null);

        remoteCallService.isArrivalStayDurationEnabled(mockBooleanCallback);
        verify(mockBooleanCallback).onSuccess(true);
    }

    /**
     * 测试注册和注销远程呼叫回调。
     */
    @Test
    public void testRegisterAndUnregisterCallback() {
        IRemoteCallService.IRemoteCallCallback mockCallback = mock(IRemoteCallService.IRemoteCallCallback.class);

        remoteCallService.registerCallback(mockCallback);
        remoteCallService.unregisterCallback(mockCallback);
        // 验证不抛出异常
    }

    /**
     * 测试释放资源。
     */
    @Test
    public void testRelease() {
        remoteCallService.release();
        // 验证不抛出异常
    }

    /**
     * 测试重复取消远程呼叫。
     */
    @Test
    public void testCancelRemoteCall_NoActive() {
        // 没有活跃的呼叫任务时取消
        remoteCallService.cancelRemoteCall(mockVoidCallback);

        // 应该成功返回（没有任务需要取消）
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试查询任务激活状态 - 无活跃任务。
     */
    @Test
    public void testIsRemoteCallActive_NoActive() {
        remoteCallService.isRemoteCallActive(mockBooleanCallback);

        verify(mockBooleanCallback).onSuccess(false);
    }
}