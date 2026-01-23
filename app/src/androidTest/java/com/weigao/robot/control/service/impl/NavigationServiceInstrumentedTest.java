package com.weigao.robot.control.service.impl;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.keenon.sdk.component.navigation.PeanutNavigation;
import com.weigao.robot.control.callback.IResultCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NavigationServiceImpl 的集成测试 (Instrumented Test)。
 * 运行在 Android 模拟器/真机上。
 */
@RunWith(AndroidJUnit4.class)
public class NavigationServiceInstrumentedTest {

    private NavigationServiceImpl mService;

    @Mock
    private PeanutNavigation mMockSdk;

    @Mock
    private IResultCallback<Void> mMockCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mService = new NavigationServiceImpl(appContext) {
            @Override
            protected PeanutNavigation createPeanutNavigation(PeanutNavigation.Builder builder) {
                return mMockSdk;
            }
        };
    }

    /**
     * 测试：设置目标点并启动导航
     */
    @Test
    public void testNavigationFlow_Start() throws InterruptedException {
        List<Integer> targets = Arrays.asList(101, 102);
        CountDownLatch latch = new CountDownLatch(1);

        mService.setTargets(targets, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                latch.countDown();
            }

            @Override
            public void onError(com.weigao.robot.control.callback.ApiError error) {
                fail("setTargets 应该成功: " + error.getMessage());
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull("PeanutNavigation 应该已被初始化", mMockSdk);

        mService.start(mMockCallback);
        verify(mMockSdk).setPilotWhenReady(true);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：暂停导航
     */
    @Test
    public void testNavigationFlow_Pause() {
        mService.setTargets(Arrays.asList(99), null);
        mService.pause(mMockCallback);
        verify(mMockSdk).setPilotWhenReady(false);
    }

    /**
     * 测试：停止导航
     */
    @Test
    public void testNavigationFlow_Stop() {
        mService.setTargets(Arrays.asList(100), null);
        mService.stop(mMockCallback);
        verify(mMockSdk).stop();
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：切换到下一个目标点
     */
    @Test
    public void testPilotNext() {
        mService.setTargets(Arrays.asList(101, 102, 103), null);
        mService.pilotNext(mMockCallback);
        verify(mMockSdk).pilotNext();
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：跳到指定索引
     */
    @Test
    public void testSkipTo() {
        mService.setTargets(Arrays.asList(101, 102, 103), null);
        mService.skipTo(2, mMockCallback);
        verify(mMockSdk).skipTo(2);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：设置导航速度
     */
    @Test
    public void testSetSpeed() {
        mService.setTargets(Arrays.asList(100), null);
        int speed = 50;
        mService.setSpeed(speed, mMockCallback);
        verify(mMockSdk).setSpeed(speed);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：设置路线策略（仅本地保存，无SDK调用）
     */
    @Test
    public void testSetRoutePolicy() {
        mService.setRoutePolicy(1, mMockCallback);
        verify(mMockCallback).onSuccess(null);
        // 注意：setRoutePolicy在实现中只保存本地变量，不直接调用SDK
    }

    /**
     * 测试：设置阻挡超时（仅本地保存，无SDK调用）
     */
    @Test
    public void testSetBlockingTimeout() {
        int timeout = 10000;
        mService.setBlockingTimeout(timeout, mMockCallback);
        verify(mMockCallback).onSuccess(null);
        // 注意：setBlockingTimeout在实现中只保存本地变量
    }

    /**
     * 测试：设置循环次数（仅本地保存，无SDK调用）
     */
    @Test
    public void testSetRepeatCount() {
        mService.setRepeatCount(3, mMockCallback);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：设置自动循环（仅本地保存，无SDK调用）
     */
    @Test
    public void testSetAutoRepeat() {
        mService.setAutoRepeat(true, mMockCallback);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：释放资源
     */
    @Test
    public void testRelease() {
        mService.setTargets(Arrays.asList(100), null);
        mService.release();
        verify(mMockSdk).stop();
        verify(mMockSdk).release();
    }

    /**
     * 测试：回调注册（验证不抛异常）
     */
    @Test
    public void testRegisterAndUnregisterCallback() {
        com.weigao.robot.control.callback.INavigationCallback mockNavCallback = mock(
                com.weigao.robot.control.callback.INavigationCallback.class);
        mService.registerCallback(mockNavCallback);
        mService.unregisterCallback(mockNavCallback);
    }
}
