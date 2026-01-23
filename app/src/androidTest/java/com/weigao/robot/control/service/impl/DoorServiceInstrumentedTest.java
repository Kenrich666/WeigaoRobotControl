package com.weigao.robot.control.service.impl;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.keenon.sdk.component.gating.manager.PeanutDoor;
import com.keenon.sdk.component.gating.state.GatingState;
import com.weigao.robot.control.callback.IResultCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * DoorServiceImpl 的集成测试。
 */
@RunWith(AndroidJUnit4.class)
public class DoorServiceInstrumentedTest {

    private DoorServiceImpl mService;

    @Mock
    private PeanutDoor mMockSdk;

    @Mock
    private IResultCallback<Void> mMockCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mService = new DoorServiceImpl(appContext) {
            @Override
            protected PeanutDoor getPeanutDoorInstance() {
                return mMockSdk;
            }
        };
    }

    /**
     * 测试：初始化是否调用了 init
     */
    @Test
    public void testInit_CallsInit() {
        verify(mMockSdk).init(any(Context.class));
    }

    /**
     * 测试：开门
     */
    @Test
    public void testOpenDoor() {
        int doorId = 1;
        mService.openDoor(doorId, false, mMockCallback);
        verify(mMockSdk).openDoor(doorId, false);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：关门
     */
    @Test
    public void testCloseDoor() {
        int doorId = 1;
        mService.closeDoor(doorId, mMockCallback);
        verify(mMockSdk).closeDoor(doorId);
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：全部开门
     */
    @Test
    public void testOpenAllDoors() {
        // 假设默认2个舱门
        mService.openAllDoors(false, mMockCallback);
        verify(mMockSdk, times(2)).openDoor(anyInt(), anyBoolean());
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：全部关门
     */
    @Test
    public void testCloseAllDoors() {
        mService.closeAllDoors(mMockCallback);
        verify(mMockSdk).closeAllDoor();
        verify(mMockCallback).onSuccess(null);
    }

    /**
     * 测试：获取单个门状态
     */
    @Test
    public void testGetDoorState() {
        int doorId = 1;
        GatingState mockState = GatingState.CLOSED;
        when(mMockSdk.getDoorState(doorId)).thenReturn(mockState);

        IResultCallback<Integer> callback = mock(IResultCallback.class);
        mService.getDoorState(doorId, callback);

        verify(mMockSdk).getDoorState(doorId);
        verify(callback).onSuccess(anyInt());
    }

    /**
     * 测试：获取所有门状态（从本地遍历获取）
     */
    @Test
    public void testGetAllDoorStates() {
        when(mMockSdk.getDoorState(anyInt())).thenReturn(GatingState.CLOSED);

        IResultCallback<int[]> callback = mock(IResultCallback.class);
        mService.getAllDoorStates(callback);

        // 验证遍历调用了getDoorState
        verify(mMockSdk, atLeastOnce()).getDoorState(anyInt());
        verify(callback).onSuccess(any(int[].class));
    }

    /**
     * 测试：查询所有门是否都已关闭
     */
    @Test
    public void testIsAllDoorsClosed() {
        when(mMockSdk.isAllDoorClose()).thenReturn(true);

        IResultCallback<Boolean> callback = mock(IResultCallback.class);
        mService.isAllDoorsClosed(callback);

        verify(mMockSdk).isAllDoorClose();
        verify(callback).onSuccess(true);
    }

    /**
     * 测试：获取门板固件版本
     */
    @Test
    public void testGetDoorVersion() {
        String expectedVersion = "v1.2.3";
        when(mMockSdk.getDoorVersion()).thenReturn(expectedVersion);

        IResultCallback<String> callback = mock(IResultCallback.class);
        mService.getDoorVersion(callback);

        verify(mMockSdk).getDoorVersion();
        verify(callback).onSuccess(expectedVersion);
    }

    /**
     * 测试：设置脚踩开关门（仅本地状态，无SDK调用）
     */
    @Test
    public void testSetFootSwitchEnabled() {
        mService.setFootSwitchEnabled(true, mMockCallback);
        verify(mMockCallback).onSuccess(null);
        // 注意：实现中只保存本地变量，不调用SDK
    }

    /**
     * 测试：设置自动离开（仅本地状态，无SDK调用）
     */
    @Test
    public void testSetAutoLeaveEnabled() {
        mService.setAutoLeaveEnabled(true, mMockCallback);
        verify(mMockCallback).onSuccess(null);
        // 注意：实现中只保存本地变量，不调用SDK
    }

    /**
     * 测试：回调注册和注销
     */
    @Test
    public void testRegisterAndUnregisterCallback() {
        com.weigao.robot.control.callback.IDoorCallback mockDoorCallback = mock(
                com.weigao.robot.control.callback.IDoorCallback.class);
        mService.registerCallback(mockDoorCallback);
        mService.unregisterCallback(mockDoorCallback);
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
