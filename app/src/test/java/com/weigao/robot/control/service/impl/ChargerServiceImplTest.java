package com.weigao.robot.control.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import android.util.Log;

import com.weigao.robot.control.callback.IResultCallback;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public class ChargerServiceImplTest {

    private ChargerServiceImpl chargerService;
    private MockedStatic<Log> mockedLogStatic;

    @Mock
    private ChargingRuntimeBridge chargingRuntimeBridge;

    @Mock
    private IResultCallback<Void> voidCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);
        chargerService = new ChargerServiceImpl(chargingRuntimeBridge);
    }

    @After
    public void tearDown() {
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    @Test
    public void startAutoChargeDelegatesToBridge() {
        chargerService.startAutoCharge(voidCallback);

        verify(chargingRuntimeBridge).performAction(ChargerServiceImpl.CHARGE_ACTION_AUTO);
        verify(voidCallback).onSuccess(null);
    }

    @Test
    public void startManualChargeDelegatesToBridge() {
        chargerService.startManualCharge(voidCallback);

        verify(chargingRuntimeBridge).performAction(ChargerServiceImpl.CHARGE_ACTION_MANUAL);
        verify(voidCallback).onSuccess(null);
    }

    @Test
    public void startAdapterChargeDelegatesToBridge() {
        chargerService.startAdapterCharge(voidCallback);

        verify(chargingRuntimeBridge).performAction(ChargerServiceImpl.CHARGE_ACTION_ADAPTER);
        verify(voidCallback).onSuccess(null);
    }

    @Test
    public void stopChargeDelegatesToBridge() {
        chargerService.stopCharge(voidCallback);

        verify(chargingRuntimeBridge).performAction(ChargerServiceImpl.CHARGE_ACTION_STOP);
        verify(voidCallback).onSuccess(null);
    }

    @Test
    public void setChargePileDelegatesToBridge() {
        chargerService.setChargePile(7, voidCallback);

        verify(chargingRuntimeBridge).setPile(7);
        verify(voidCallback).onSuccess(null);
    }

    @Test
    public void performActionReportsErrorWhenBridgeThrows() {
        doThrow(new IllegalStateException("boom")).when(chargingRuntimeBridge).performAction(anyInt());

        IResultCallback<Void> callback = mock(IResultCallback.class);
        chargerService.performAction(ChargerServiceImpl.CHARGE_ACTION_AUTO, callback);

        verify(callback).onError(any());
    }

    @Test
    public void releaseDoesNotReleaseSharedBridge() {
        chargerService.release();
    }
}
