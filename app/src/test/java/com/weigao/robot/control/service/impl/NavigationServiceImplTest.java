package com.weigao.robot.control.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.navigation.PeanutNavigation;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.route.RouteNode;
import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.NavigationNode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

/**
 * NavigationServiceImpl 的单元测试类。
 * 
 * 本测试类旨在使用 Mockito 框架验证导航服务的逻辑正确性。
 * 主要测试难点在于：
 * 1. 模拟 Android 系统类（如 Log），防止在非 Android 环境运行 JUnit 时崩溃。
 * 2. 模拟 SDK 内部组件（如 PeanutNavigation），该组件通过 Builder 模式在 Service 内部创建。
 * 3. 验证异步回调（ResultCallback）是否被正确执行。
 */
public class NavigationServiceImplTest {

    // 待测试的目标对象
    private NavigationServiceImpl navigationService;

    // 静态 Mock 容器，用于拦截对 android.util.Log 的调用
    private MockedStatic<Log> mockedLogStatic;

    // 构造函数 Mock 容器，用于拦截 "new PeanutNavigation.Builder()" 的操作
    // 这是处理 Service 内部 new 对象的一种高效方式
    private MockedConstruction<PeanutNavigation.Builder> mockedBuilderConstruction;

    // 模拟的 SDK 核心对象，Service 最终会调用这个对象的各种方法
    private PeanutNavigation mockedPeanutNavigation;

    // 模拟的 Builder 对象，用于支持链式调用
    private PeanutNavigation.Builder mockedBuilder;

    // 模拟 Context，Android 开发中几乎所有组件初始化都需要它
    @Mock
    private Context mockContext;

    // 模拟回调接口，用于验证 Service 操作完成后是否通知了调用者
    @Mock
    private IResultCallback<Void> mockVoidCallback;

    // 模拟导航特定的回调，用于测试状态变更通知
    @Mock
    private INavigationCallback mockNavigationCallback;

    /**
     * 在每个测试用例运行前执行。
     * 负责初始化所有 Mock 对象和配置模拟行为。
     */
    @Before
    public void setUp() {
        // 初始化标注了 @Mock 的对象
        MockitoAnnotations.openMocks(this);

        // [核心配置 1]：模拟 Log 静态方法。
        // 在本地单元测试中，android.jar 中的 Log 方法默认没有实现，直接调用会报错。
        // 这里拦截 Log.d/e 等方法并让它们返回 0（正常执行的假象）。
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);

        // [核心配置 2]：配置 SDK 对象的模拟。
        mockedPeanutNavigation = mock(PeanutNavigation.class);
        mockedBuilder = mock(PeanutNavigation.Builder.class);

        // [核心配置 3]：模拟 Builder 模式。
        // 使用 mockConstruction 拦截 Service 中对 PeanutNavigation.Builder 的实例化。
        // 这样当 Service 执行 "new PeanutNavigation.Builder()" 时，会返回我们配置好的 mock。
        mockedBuilderConstruction = mockConstruction(PeanutNavigation.Builder.class, (mock, context) -> {
            // 配置 Builder 的链式调用：每个 setXXX 方法都返回 Builder 实例自己（即这里的 mock）
            when(mock.setListener(any())).thenReturn(mock);
            when(mock.setBlockingTimeOut(anyInt())).thenReturn(mock);
            when(mock.setRoutePolicy(anyInt())).thenReturn(mock);
            when(mock.enableAutoRepeat(anyBoolean())).thenReturn(mock);
            when(mock.setRepeatCount(anyInt())).thenReturn(mock);
            when(mock.setTargets(any())).thenReturn(mock);
            // 最后一步 build() 返回我们提前 mock 好的 SDK 核心对象
            when(mock.build()).thenReturn(mockedPeanutNavigation);
        });

        // 模拟 Context 返回 ApplicationContext，防止 Service 初始化时 NPE
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        // 创建被测实例
        navigationService = new NavigationServiceImpl(mockContext);
    }

    /**
     * 在每个测试用例结束后执行。
     * 必须释放 MockedStatic 和 MockedConstruction，否则会影响其他测试类。
     */
    @After
    public void tearDown() {
        if (mockedBuilderConstruction != null) {
            mockedBuilderConstruction.close();
        }
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    /**
     * 测试设置导航目标点的功能。
     * 验证点：
     * 1. 业务层回调 onSuccess 被触发。
     * 2. 底层 SDK 的 Builder 被正确创建。
     */
    @Test
    public void testSetTargets() {
        // 准备测试数据
        List<Integer> targets = Arrays.asList(1, 2, 3);

        // 执行待测方法
        navigationService.setTargets(targets, mockVoidCallback);

        // 验证：是否通知了调用方操作成功
        verify(mockVoidCallback).onSuccess(null);

        // 验证：PeanutNavigation.Builder 是否在内部被实例化了
        assertFalse("Builder 应该被成功构造", mockedBuilderConstruction.constructed().isEmpty());
    }

    /**
     * 测试启动导航功能。
     * 验证点：
     * 1. Service 是否调用了 SDK 的 setPilotWhenReady(true) 方法。
     */
    @Test
    public void testStart() {
        // 准备工作：在 start 之前必须先初始化 SDK 实例（通过 setTargets 触发）
        navigationService.setTargets(Arrays.asList(1), null);

        // 执行启动
        navigationService.start(mockVoidCallback);

        // 验证：SDK 层的核心方法是否被调用
        verify(mockedPeanutNavigation).setPilotWhenReady(true);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试暂停导航功能。
     * 验证点：调用 pause 后，底层 SDK 是否接收到了 setPilotWhenReady(false) 的指令。
     */
    @Test
    public void testPause() {
        // 初始化 SDK 环境
        navigationService.setTargets(Arrays.asList(1), null);

        // 执行暂停
        navigationService.pause(mockVoidCallback);

        // 验证底层调用
        verify(mockedPeanutNavigation).setPilotWhenReady(false);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试停止导航功能。
     */
    @Test
    public void testStop() {
        navigationService.setTargets(Arrays.asList(1), null);

        navigationService.stop(mockVoidCallback);

        // 验证是否调用了 SDK 的 stop 方法
        verify(mockedPeanutNavigation).stop();
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试设置导航速度。
     */
    @Test
    public void testSetSpeed() {
        navigationService.setTargets(Arrays.asList(1), null);

        int speed = 50;
        navigationService.setSpeed(speed, mockVoidCallback);

        // 验证速度参数是否正确传递给 SDK
        verify(mockedPeanutNavigation).setSpeed(speed);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试前往下一个点。
     */
    @Test
    public void testPilotNext() {
        navigationService.setTargets(Arrays.asList(1, 2), null);

        navigationService.pilotNext(mockVoidCallback);

        // 验证底层 SDK 指令
        verify(mockedPeanutNavigation).pilotNext();
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试跳过点位直接前往指定索引点。
     */
    @Test
    public void testSkipTo() {
        navigationService.setTargets(Arrays.asList(1, 2, 3), null);

        int targetIndex = 2;
        navigationService.skipTo(targetIndex, mockVoidCallback);

        // 验证跳过逻辑是否传达到底层
        verify(mockedPeanutNavigation).skipTo(targetIndex);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试回调监听器的注册。
     */
    @Test
    public void testRegisterCallback() {
        // 仅仅验证注册方法调用不发生异常
        navigationService.registerCallback(mockNavigationCallback);
        // 通常在集成测试或更高阶单元测试中，我们会通过反射检查 Service 内部的 callback list 大小
    }

    /**
     * 测试取消注册回调。
     */
    @Test
    public void testUnregisterCallback() {
        navigationService.registerCallback(mockNavigationCallback);
        navigationService.unregisterCallback(mockNavigationCallback);
        // 验证不抛出异常即可
    }

    /**
     * 测试取消到达控制。
     */
    @Test
    public void testCancelArrivalControl() {
        navigationService.setTargets(Arrays.asList(1), null);

        navigationService.cancelArrivalControl(mockVoidCallback);

        // 验证底层 SDK 调用
        verify(mockedPeanutNavigation).cancelArrivalControl();
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试手动控制导航方向。
     */
    @Test
    public void testManual() {
        navigationService.setTargets(Arrays.asList(1), null);

        int direction = 1; // 前进
        navigationService.manual(direction, mockVoidCallback);

        // 验证 SDK 层的 manual 方法被调用
        verify(mockedPeanutNavigation).manual(direction);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试设置路由策略。
     */
    @Test
    public void testSetRoutePolicy() {
        int policy = Navigation.ROUTE_POLICY_SHORTEST;
        navigationService.setRoutePolicy(policy, mockVoidCallback);

        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试设置阻塞超时时间。
     */
    @Test
    public void testSetBlockingTimeout() {
        int timeout = 30;
        navigationService.setBlockingTimeout(timeout, mockVoidCallback);

        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试设置重复次数。
     */
    @Test
    public void testSetRepeatCount() {
        int count = 3;
        navigationService.setRepeatCount(count, mockVoidCallback);

        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试设置自动重复。
     */
    @Test
    public void testSetAutoRepeat() {
        navigationService.setAutoRepeat(true, mockVoidCallback);

        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试设置到达控制启用状态。
     */
    @Test
    public void testSetArrivalControlEnabled() {
        navigationService.setTargets(Arrays.asList(1), null);

        navigationService.setArrivalControlEnabled(true, mockVoidCallback);

        // 验证底层 SDK 到达控制设置
        verify(mockedPeanutNavigation).setArrivalControl(true);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试获取路由节点列表。
     */
    @Test
    public void testGetRouteNodes() {
        navigationService.setTargets(Arrays.asList(1, 2), null);

        IResultCallback<List<NavigationNode>> mockListCallback = mock(IResultCallback.class);
        navigationService.getRouteNodes(mockListCallback);

        // 验证回调被调用（初始为空列表）
        verify(mockListCallback).onSuccess(any());
    }

    /**
     * 测试获取当前节点。
     */
    @Test
    public void testGetCurrentNode() {
        navigationService.setTargets(Arrays.asList(1), null);

        IResultCallback<NavigationNode> mockNodeCallback = mock(IResultCallback.class);
        navigationService.getCurrentNode(mockNodeCallback);

        // 验证回调被调用
        verify(mockNodeCallback).onSuccess(any());
    }

    /**
     * 测试获取下一个节点。
     */
    @Test
    public void testGetNextNode() {
        navigationService.setTargets(Arrays.asList(1, 2), null);

        IResultCallback<NavigationNode> mockNodeCallback = mock(IResultCallback.class);
        navigationService.getNextNode(mockNodeCallback);

        verify(mockNodeCallback).onSuccess(any());
    }

    /**
     * 测试在未初始化时启动导航（异常路径）。
     */
    @Test
    public void testStart_NotInitialized_Error() {
        // 不调用 setTargets，直接调用 start
        navigationService.start(mockVoidCallback);

        // 验证返回错误
        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试在未初始化时停止导航（异常路径）。
     */
    @Test
    public void testStop_NotInitialized_Error() {
        // 不调用 setTargets，直接调用 stop
        navigationService.stop(mockVoidCallback);

        verify(mockVoidCallback).onError(any());
    }
}
