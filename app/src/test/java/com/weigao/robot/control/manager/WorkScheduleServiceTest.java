package com.weigao.robot.control.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.util.Log;

import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.ServiceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WorkScheduleServiceTest {

    private WorkScheduleService service;
    private IChargerService chargerService;
    private MockedStatic<Log> mockedLogStatic;

    @Before
    public void setUp() throws Exception {
        service = WorkScheduleService.getInstance();
        chargerService = mock(IChargerService.class);
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.w(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);
        TaskExecutionStateManager.getInstance().resetForTest();
        resetDeferredState();
        prepareServiceManager(chargerService);
    }

    @After
    public void tearDown() throws Exception {
        TaskExecutionStateManager.getInstance().resetForTest();
        resetDeferredState();
        resetServiceManager();
        if (mockedLogStatic != null) {
            mockedLogStatic.close();
        }
    }

    @Test
    public void latestDeferredActionOverridesPreviousOne() throws Exception {
        TaskExecutionStateManager.getInstance().startTask(TaskType.ITEM_DELIVERY);

        assertTrue(invokeDeferAction("WORK_START", 0));
        assertTrue(invokeDeferAction("WORK_END", 1));
        assertEquals("WORK_END", getDeferredActionName());
        assertEquals(1, getDeferredScheduleIndex());

        TaskExecutionStateManager.getInstance().finishTask();

        assertTrue(service.executeDeferredActionIfIdle());
        verify(chargerService).startAutoCharge(any());
        verify(chargerService, never()).stopCharge(any());
        assertEquals("NONE", getDeferredActionName());
        assertEquals(-1, getDeferredScheduleIndex());
    }

    @Test
    public void deferredActionExecutesOnlyOnceAfterTaskFinishes() throws Exception {
        TaskExecutionStateManager.getInstance().startTask(TaskType.ITEM_DELIVERY);

        assertTrue(invokeDeferAction("WORK_END", 3));

        TaskExecutionStateManager.getInstance().finishTask();

        assertTrue(service.executeDeferredActionIfIdle());
        assertFalse(service.executeDeferredActionIfIdle());
        verify(chargerService).startAutoCharge(any());
    }

    private boolean invokeDeferAction(String actionName, int scheduleIndex) throws Exception {
        Class<?> actionClass = Class.forName(
                "com.weigao.robot.control.manager.WorkScheduleService$DeferredAction");
        Method method = WorkScheduleService.class.getDeclaredMethod("deferActionIfBusy", actionClass, int.class);
        method.setAccessible(true);
        Object action = Enum.valueOf(actionClass.asSubclass(Enum.class), actionName);
        return (Boolean) method.invoke(service, action, scheduleIndex);
    }

    private String getDeferredActionName() throws Exception {
        Field field = WorkScheduleService.class.getDeclaredField("deferredAction");
        field.setAccessible(true);
        Object value = field.get(service);
        return value instanceof Enum ? ((Enum<?>) value).name() : null;
    }

    private int getDeferredScheduleIndex() throws Exception {
        Field field = WorkScheduleService.class.getDeclaredField("deferredScheduleIndex");
        field.setAccessible(true);
        return field.getInt(service);
    }

    private void resetDeferredState() throws Exception {
        Class<?> actionClass = Class.forName(
                "com.weigao.robot.control.manager.WorkScheduleService$DeferredAction");
        Object noneAction = Enum.valueOf(actionClass.asSubclass(Enum.class), "NONE");
        setWorkScheduleServiceField("deferredAction", noneAction);
        setWorkScheduleServiceField("deferredScheduleIndex", -1);
        setWorkScheduleServiceField("pendingReturnToMainAfterWorkStart", false);
    }

    private void setWorkScheduleServiceField(String fieldName, Object value) throws Exception {
        Field field = WorkScheduleService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private void prepareServiceManager(IChargerService chargerService) throws Exception {
        ServiceManager serviceManager = ServiceManager.getInstance();
        setServiceManagerField(serviceManager, "initialized", true);
        setServiceManagerField(serviceManager, "chargerService", chargerService);
    }

    private void resetServiceManager() throws Exception {
        ServiceManager serviceManager = ServiceManager.getInstance();
        setServiceManagerField(serviceManager, "chargerService", null);
        setServiceManagerField(serviceManager, "initialized", false);
        setServiceManagerField(serviceManager, "context", null);
    }

    private void setServiceManagerField(ServiceManager serviceManager, String fieldName, Object value)
            throws Exception {
        Field field = ServiceManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(serviceManager, value);
    }
}
