package com.weigao.robot.control.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.weigao.robot.control.callback.IResultCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ChargerServiceInstrumentedTest {

    private ChargerServiceImpl service;

    @Mock
    private ChargingRuntimeBridge chargingRuntimeBridge;

    @Mock
    private IResultCallback<Void> callback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ChargerServiceImpl(chargingRuntimeBridge);
    }

    @Test
    public void startAutoChargeDelegatesToBridge() {
        service.startAutoCharge(callback);

        verify(chargingRuntimeBridge).performAction(ChargerServiceImpl.CHARGE_ACTION_AUTO);
        verify(callback).onSuccess(null);
    }

    @Test
    public void setChargePileDelegatesToBridge() {
        service.setChargePile(3, callback);

        verify(chargingRuntimeBridge).setPile(3);
        verify(callback).onSuccess(null);
    }

    @Test
    public void performActionReportsErrorWhenBridgeThrows() {
        doThrow(new IllegalStateException("boom")).when(chargingRuntimeBridge)
                .performAction(ChargerServiceImpl.CHARGE_ACTION_STOP);

        IResultCallback<Void> errorCallback = mock(IResultCallback.class);
        service.performAction(ChargerServiceImpl.CHARGE_ACTION_STOP, errorCallback);

        verify(errorCallback).onError(any());
    }
}
