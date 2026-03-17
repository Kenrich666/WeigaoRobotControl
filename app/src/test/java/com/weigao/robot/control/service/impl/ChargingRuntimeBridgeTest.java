package com.weigao.robot.control.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.Log;

import com.keenon.sdk.component.charger.PeanutCharger;
import com.keenon.sdk.component.charger.common.Charger;
import com.weigao.robot.control.callback.SdkErrorCode;
import com.weigao.robot.control.model.ChargingState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class ChargingRuntimeBridgeTest {

    private MockedStatic<Log> mockedLogStatic;
    private MockedConstruction<PeanutCharger.Builder> mockedBuilderConstruction;
    private PeanutCharger mockedPeanutCharger;
    private Charger.Listener chargerListener;
    private ChargingRuntimeBridge chargingRuntimeBridge;

    @Before
    public void setUp() {
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);

        mockedPeanutCharger = mock(PeanutCharger.class);
        mockedBuilderConstruction = mockConstruction(PeanutCharger.Builder.class, (mock, context) -> {
            when(mock.setPile(org.mockito.ArgumentMatchers.anyInt())).thenReturn(mock);
            when(mock.setListener(any())).thenReturn(mock);
            when(mock.build()).thenReturn(mockedPeanutCharger);
        });

        chargingRuntimeBridge = new ChargingRuntimeBridge(mock(Context.class));

        ArgumentCaptor<Charger.Listener> listenerCaptor = ArgumentCaptor.forClass(Charger.Listener.class);
        PeanutCharger.Builder builder = mockedBuilderConstruction.constructed().get(0);
        verify(builder).setListener(listenerCaptor.capture());
        chargerListener = listenerCaptor.getValue();
    }

    @After
    public void tearDown() {
        if (chargingRuntimeBridge != null) {
            chargingRuntimeBridge.release();
        }
        if (mockedBuilderConstruction != null) {
            mockedBuilderConstruction.close();
        }
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    @Test
    public void initExecutesPeanutCharger() {
        verify(mockedPeanutCharger).execute();
    }

    @Test
    public void setPileUpdatesSnapshot() {
        chargingRuntimeBridge.setPile(12);

        ChargingState state = chargingRuntimeBridge.getCurrentState();
        assertEquals(12, state.getPileId());
        verify(mockedPeanutCharger).setPile(12);
    }

    @Test
    public void stopActionMarksChargingExited() {
        chargingRuntimeBridge.performAction(PeanutCharger.CHARGE_ACTION_STOP);

        ChargingState state = chargingRuntimeBridge.getCurrentState();
        assertFalse(state.isCharging());
        assertEquals(ChargingRuntimeBridge.CHARGER_STATUS_IDLE, state.getStatus());
        assertEquals(SdkErrorCode.CHARGER_EVENT_EXIT, state.getEvent());
    }

    @Test
    public void chargerInfoEventUpdatesSnapshot() {
        com.keenon.sdk.component.charger.common.ChargerInfo sdkInfo =
                mock(com.keenon.sdk.component.charger.common.ChargerInfo.class);
        when(sdkInfo.getEvent()).thenReturn(SdkErrorCode.CHARGER_EVENT_CHARGING);

        chargerListener.onChargerInfoChanged(0, sdkInfo);

        assertEquals(SdkErrorCode.CHARGER_EVENT_CHARGING,
                chargingRuntimeBridge.getCurrentState().getEvent());
    }

    @Test
    public void chargerStatusUpdateMarksCharging() {
        chargerListener.onChargerStatusChanged(ChargingRuntimeBridge.CHARGER_STATUS_IDLE);
        chargerListener.onChargerStatusChanged(ChargingRuntimeBridge.CHARGER_STATUS_CHARGING);

        ChargingState state = chargingRuntimeBridge.getCurrentState();
        assertTrue(state.isCharging());
        assertEquals(ChargingRuntimeBridge.CHARGER_STATUS_CHARGING, state.getStatus());
    }
}
