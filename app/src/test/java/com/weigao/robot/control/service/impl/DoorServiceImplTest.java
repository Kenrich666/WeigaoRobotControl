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

        // Mock Static Log FIRST because it's used in constructor
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);
        mockedLogStatic.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.v(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.w(anyString(), anyString())).thenReturn(0);

        // Mock Static PeanutDoor.getInstance()
        mockedPeanutDoorInstance = mock(PeanutDoor.class);
        mockedPeanutDoorStatic = mockStatic(PeanutDoor.class);
        mockedPeanutDoorStatic.when(PeanutDoor::getInstance).thenReturn(mockedPeanutDoorInstance);

        // Mock Context application context
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        // Initialize Service
        doorService = new DoorServiceImpl(mockContext);
    }

    @After
    public void tearDown() {
        if (doorService != null) {
            doorService.release();
        }
        // Close static mocks in reverse order of creation or just close them
        if (mockedPeanutDoorStatic != null) {
            mockedPeanutDoorStatic.close();
        }
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    @Test
    public void testOpenDoor_Success() {
        // Arrange
        int doorId = 2;
        boolean single = true;

        // Act
        doorService.openDoor(doorId, single, mockVoidCallback);

        // Assert
        verify(mockedPeanutDoorInstance).openDoor(eq(doorId), eq(single));
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testOpenDoor_InvalidId_Error() {
        // Arrange
        int invalidId = 100; // Assuming max is 2 or 4

        // Act
        doorService.openDoor(invalidId, false, mockVoidCallback);

        // Assert
        verify(mockedPeanutDoorInstance, times(0)).openDoor(anyInt(), any(Boolean.class));
        verify(mockVoidCallback).onError(any());
    }

    @Test
    public void testCloseDoor_Success() {
        // Arrange
        int doorId = 2;

        // Act
        doorService.closeDoor(doorId, mockVoidCallback);

        // Assert
        verify(mockedPeanutDoorInstance).closeDoor(eq(doorId));
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testCloseAllDoors_Success() {
        // Act
        doorService.closeAllDoors(mockVoidCallback);

        // Assert
        verify(mockedPeanutDoorInstance).closeAllDoor();
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testIsAllDoorsClosed_True() {
        // Arrange
        when(mockedPeanutDoorInstance.isAllDoorClose()).thenReturn(true);

        // Act
        doorService.isAllDoorsClosed(mockBooleanCallback);

        // Assert
        verify(mockBooleanCallback).onSuccess(true);
    }

    @Test
    public void testSetDoorType_Four() {
        // Arrange
        DoorType type = DoorType.FOUR;
        int sdkType = Door.SET_TYPE_FOUR;

        // Act
        doorService.setDoorType(type, mockVoidCallback);

        // Assert
        verify(mockedPeanutDoorInstance).setDoorType(eq(sdkType), any(), any());
        verify(mockVoidCallback).onSuccess(null);
    }

    @Test
    public void testGetDoorType_Success() {
        // Arrange
        int sdkTypeId = 1; // Double Door
        when(mockedPeanutDoorInstance.getDoorType()).thenReturn(sdkTypeId);

        // Act
        doorService.getDoorType(mockDoorTypeCallback);

        // Assert
        verify(mockDoorTypeCallback).onSuccess(DoorType.DOUBLE);
    }

    @Test
    public void testRegisterAndUnregisterCallback() {
        // Arrange
        IDoorCallback callback = mock(IDoorCallback.class);

        // Act
        doorService.registerCallback(callback);
        doorService.unregisterCallback(callback);

        // Assert
        // This is mainly state validation, hard to verify without triggering callback
        // Could verify internal list logic if exposed, but blackbox is better.
        // We can check logs or assume standard List behavior.
    }
}
