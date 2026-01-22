package com.weigao.robot.control.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.gating.manager.Door;
import com.keenon.sdk.component.gating.manager.PeanutDoor;
import com.keenon.sdk.component.gating.data.GatingType;
import com.keenon.sdk.component.gating.state.GatingState;
import com.weigao.robot.control.callback.IDoorCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.DoorType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * DoorServiceImpl 的单元测试类
 */
public class DoorServiceImplTest {

    private DoorServiceImpl doorService;
    private MockedStatic<PeanutDoor> mockedPeanutDoorStatic;
    private MockedStatic<Log> mockedLogStatic;
    private PeanutDoor mockedPeanutDoorInstance;

    @Mock
    private Context mockContext;

    @Mock
    private IResultCallback<Void> mockVoidCallback;

    @Mock
    private IResultCallback<Boolean> mockBooleanCallback;

    @Mock
    private IResultCallback<Integer> mockIntegerCallback;

    @Mock
    private IResultCallback<DoorType> mockDoorTypeCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 首先模拟静态 Log 方法，因为它在构造函数中被使用
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);
        mockedLogStatic.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.v(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.w(anyString(), anyString())).thenReturn(0);

        // 模拟静态方法 PeanutDoor.getInstance() 以返回模拟实例
        mockedPeanutDoorInstance = mock(PeanutDoor.class);
        mockedPeanutDoorStatic = mockStatic(PeanutDoor.class);
        mockedPeanutDoorStatic.when(PeanutDoor::getInstance).thenReturn(mockedPeanutDoorInstance);

        // 模拟 Context 的 application context
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        // 初始化 Service
        doorService = new DoorServiceImpl(mockContext);
    }

    @After
    public void tearDown() {
        if (doorService != null) {
            doorService.release();
        }
        // 按创建反序或直接关闭静态模拟
        if (mockedPeanutDoorStatic != null) {
            mockedPeanutDoorStatic.close();
        }
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    @Test
    public void testOpenDoor_Success() {
        // 测试成功打开门
        int doorId = 2;
        boolean single = true;

        doorService.openDoor(doorId, single, mockVoidCallback);

        // 验证是否调用了 SDK 对应的开门方法
        verify(mockedPeanutDoorInstance).openDoor(eq(doorId), eq(single));
        // 验证成功回调
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testOpenDoor_InvalidId_Error() {
        // 测试使用无效 ID 开门
        int invalidId = 100;

        doorService.openDoor(invalidId, false, mockVoidCallback);

        // 验证 SDK 方法未被调用
        verify(mockedPeanutDoorInstance, times(0)).openDoor(anyInt(), any(Boolean.class));
        // 验证错误回调
        verify(mockVoidCallback).onError(any());
    }

    @Test
    public void testCloseDoor_Success() {
        // 测试关闭指定门
        int doorId = 2;

        doorService.closeDoor(doorId, mockVoidCallback);

        verify(mockedPeanutDoorInstance).closeDoor(eq(doorId));
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testCloseAllDoors_Success() {
        // 测试关闭所有门
        doorService.closeAllDoors(mockVoidCallback);

        verify(mockedPeanutDoorInstance).closeAllDoor();
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testIsAllDoorsClosed_True() {
        // 测试查询所有门是否已关闭
        when(mockedPeanutDoorInstance.isAllDoorClose()).thenReturn(true);

        doorService.isAllDoorsClosed(mockBooleanCallback);

        verify(mockBooleanCallback).onSuccess(true);
    }

    @Test
    public void testSetDoorType_Four() {
        // 测试设置门类型为四门
        DoorType type = DoorType.FOUR;
        int sdkType = Door.SET_TYPE_FOUR;

        doorService.setDoorType(type, mockVoidCallback);

        verify(mockedPeanutDoorInstance).setDoorType(eq(sdkType), any(), any());
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testGetDoorType_Success() {
        // 测试获取门类型
        int sdkTypeId = 1; // 双门
        when(mockedPeanutDoorInstance.getDoorType()).thenReturn(sdkTypeId);

        doorService.getDoorType(mockDoorTypeCallback);

        verify(mockDoorTypeCallback).onSuccess(DoorType.DOUBLE);
    }

    @Test
    public void testRegisterAndUnregisterCallback() {
        // 测试注册和注销回调
        IDoorCallback callback = mock(IDoorCallback.class);

        doorService.registerCallback(callback);
        doorService.unregisterCallback(callback);
    }

    /**
     * 测试关闭指定门时使用无效 ID。
     */
    @Test
    public void testCloseDoor_InvalidId_Error() {
        int invalidId = 100;
        doorService.closeDoor(invalidId, mockVoidCallback);

        verify(mockedPeanutDoorInstance, times(0)).closeDoor(anyInt());
        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试获取指定门的状态。
     */
    @Test
    public void testGetDoorState_Success() {
        int doorId = 1;
        int expectedState = GatingState.CLOSED;
        when(mockedPeanutDoorInstance.getDoorState(doorId)).thenReturn(expectedState);

        doorService.getDoorState(doorId, mockIntegerCallback);

        verify(mockIntegerCallback).onSuccess(expectedState);
    }

    /**
     * 测试获取所有门的状态。
     */
    @Test
    public void testGetAllDoorStates_Success() {
        int[] expectedStates = { GatingState.CLOSE, GatingState.OPEN, GatingState.CLOSE, GatingState.CLOSE };
        when(mockedPeanutDoorInstance.getAllDoorState()).thenReturn(expectedStates);

        IResultCallback<int[]> mockIntArrayCallback = mock(IResultCallback.class);
        doorService.getAllDoorStates(mockIntArrayCallback);

        verify(mockIntArrayCallback).onSuccess(expectedStates);
    }

    /**
     * 测试是否支持门类型设置。
     */
    @Test
    public void testSupportDoorTypeSetting() {
        when(mockedPeanutDoorInstance.supportDoorTypeSetting()).thenReturn(true);

        doorService.supportDoorTypeSetting(mockBooleanCallback);

        verify(mockBooleanCallback).onSuccess(true);
    }

    /**
     * 测试获取门版本号。
     */
    @Test
    public void testGetDoorVersion() {
        String expectedVersion = "v1.2.3";
        when(mockedPeanutDoorInstance.getDoorVersion()).thenReturn(expectedVersion);

        IResultCallback<String> mockStringCallback = mock(IResultCallback.class);
        doorService.getDoorVersion(mockStringCallback);

        verify(mockStringCallback).onSuccess(expectedVersion);
    }

    /**
     * 测试设置脚踏开关启用状态。
     */
    @Test
    public void testSetFootSwitchEnabled() {
        doorService.setFootSwitchEnabled(true, mockVoidCallback);

        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试设置自动离开启用状态。
     */
    @Test
    public void testSetAutoLeaveEnabled() {
        doorService.setAutoLeaveEnabled(true, mockVoidCallback);

        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试释放资源。
     */
    @Test
    public void testRelease() {
        doorService.release();
        // 验证 release 正确执行，不抛出异常
    }

    /**
     * 测试设置门类型后查询 - 验证双门类型。
     */
    @Test
    public void testSetAndGetDoorType_Double() {
        DoorType type = DoorType.DOUBLE;
        int sdkType = Door.SET_TYPE_DOUBLE;

        doorService.setDoorType(type, mockVoidCallback);
        verify(mockedPeanutDoorInstance).setDoorType(eq(sdkType), any(), any());

        // 测试获取
        when(mockedPeanutDoorInstance.getDoorType()).thenReturn(1);
        doorService.getDoorType(mockDoorTypeCallback);
        verify(mockDoorTypeCallback).onSuccess(DoorType.DOUBLE);
    }
}
