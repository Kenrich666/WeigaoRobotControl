package com.weigao.robot.control.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.weigao.robot.control.callback.INavigationCallback;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.model.ChargingState;
import com.weigao.robot.control.model.NavigationNode;
import com.weigao.robot.control.service.INavigationService;
import com.weigao.robot.control.service.ServiceManager;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;

public class LowBatteryAutoChargeManagerTest {

    @After
    public void tearDown() throws Exception {
        LowBatteryAutoChargeManager.getInstance().release();
        TaskExecutionStateManager.getInstance().resetForTest();
        LowBatteryAutoChargeSettingsManager.resetSingletonForTest();
        setSettingsSingleton(null);
        resetServiceManager();
    }

    @Test
    public void lowBatteryWithoutActiveTaskEntersConfirmationState() throws Exception {
        configureSettings(true, 20);
        prepareServiceManager();
        LowBatteryAutoChargeManager manager = LowBatteryAutoChargeManager.getInstance();

        invokeHandleBatteryLevelChanged(manager, 15);
        invokeHandleChargingStateChanged(manager, chargingState(false));

        assertFalse(manager.hasPendingTaskCompletionAutoCharge());
        assertTrue(getBooleanField(manager, "confirmationPending"));
    }

    @Test
    public void lowBatteryWithActiveTaskWaitsUntilTaskFinishes() throws Exception {
        configureSettings(true, 20);
        prepareServiceManager();
        LowBatteryAutoChargeManager manager = LowBatteryAutoChargeManager.getInstance();
        TaskExecutionStateManager.getInstance().startTask(TaskType.ITEM_DELIVERY);

        invokeHandleBatteryLevelChanged(manager, 15);

        assertTrue(manager.hasPendingTaskCompletionAutoCharge());
        assertFalse(getBooleanField(manager, "confirmationPending"));

        TaskExecutionStateManager.getInstance().finishTask();
        manager.onTaskCompletedAndReadyForPrompt();

        assertFalse(manager.hasPendingTaskCompletionAutoCharge());
        assertTrue(getBooleanField(manager, "confirmationPending"));
    }

    @Test
    public void cancellingCurrentRoundSuppressesUntilBatteryRecovers() throws Exception {
        configureSettings(true, 20);
        prepareServiceManager();
        LowBatteryAutoChargeManager manager = LowBatteryAutoChargeManager.getInstance();

        invokeHandleBatteryLevelChanged(manager, 15);
        invokePrivateNoArg(manager, "skipCurrentAutoCharge");

        assertTrue(getBooleanField(manager, "suppressUntilRecovery"));
        assertFalse(getBooleanField(manager, "confirmationPending"));

        invokeHandleBatteryLevelChanged(manager, 15);
        assertFalse(getBooleanField(manager, "confirmationPending"));

        invokeHandleBatteryLevelChanged(manager, 35);
        assertFalse(getBooleanField(manager, "suppressUntilRecovery"));

        invokeHandleBatteryLevelChanged(manager, 15);
        assertTrue(getBooleanField(manager, "confirmationPending"));
    }

    @Test
    public void chargingStateClearsPendingTaskAutoCharge() throws Exception {
        configureSettings(true, 20);
        prepareServiceManager();
        LowBatteryAutoChargeManager manager = LowBatteryAutoChargeManager.getInstance();
        TaskExecutionStateManager.getInstance().startTask(TaskType.CIRCULAR_DELIVERY);

        invokeHandleBatteryLevelChanged(manager, 15);
        assertTrue(manager.hasPendingTaskCompletionAutoCharge());

        invokeHandleChargingStateChanged(manager, chargingState(true));

        assertFalse(manager.hasPendingTaskCompletionAutoCharge());
        assertFalse(getBooleanField(manager, "confirmationPending"));
        assertTrue(getBooleanField(manager, "autoChargeInProgress"));
    }

    @Test
    public void chargingStateChangeMarksCharging() throws Exception {
        LowBatteryAutoChargeManager manager = LowBatteryAutoChargeManager.getInstance();

        invokeHandleChargingStateChanged(manager, chargingState(true));

        assertTrue(getBooleanField(manager, "currentCharging"));
    }

    private void configureSettings(boolean enabled, int thresholdPercent) throws Exception {
        File root = Files.createTempDirectory("low-battery-auto-charge-manager").toFile();
        LowBatteryAutoChargeSettingsManager settingsManager = new LowBatteryAutoChargeSettingsManager(root);
        settingsManager.update(enabled, thresholdPercent);
        setSettingsSingleton(settingsManager);
    }

    private void setSettingsSingleton(LowBatteryAutoChargeSettingsManager manager) throws Exception {
        Field instanceField = LowBatteryAutoChargeSettingsManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, manager);
    }

    private void invokeHandleChargingStateChanged(LowBatteryAutoChargeManager manager, ChargingState chargingState)
            throws Exception {
        Method method = LowBatteryAutoChargeManager.class.getDeclaredMethod(
                "handleChargingStateChanged", ChargingState.class);
        method.setAccessible(true);
        method.invoke(manager, chargingState);
    }

    private void invokeHandleBatteryLevelChanged(LowBatteryAutoChargeManager manager, int batteryLevel)
            throws Exception {
        Method method = LowBatteryAutoChargeManager.class.getDeclaredMethod("handleBatteryLevelChanged", int.class);
        method.setAccessible(true);
        method.invoke(manager, batteryLevel);
    }

    private void invokePrivateNoArg(LowBatteryAutoChargeManager manager, String methodName) throws Exception {
        Method method = LowBatteryAutoChargeManager.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(manager);
    }

    private boolean getBooleanField(LowBatteryAutoChargeManager manager, String fieldName) throws Exception {
        Field field = LowBatteryAutoChargeManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(manager);
    }

    private ChargingState chargingState(boolean charging) {
        ChargingState state = new ChargingState();
        state.setCharging(charging);
        return state;
    }

    private void prepareServiceManager() throws Exception {
        ServiceManager serviceManager = ServiceManager.getInstance();
        setServiceManagerField(serviceManager, "initialized", true);
        setServiceManagerField(serviceManager, "navigationService", new NoOpNavigationService());
    }

    private void resetServiceManager() throws Exception {
        ServiceManager serviceManager = ServiceManager.getInstance();
        setServiceManagerField(serviceManager, "navigationService", null);
        setServiceManagerField(serviceManager, "initialized", false);
        setServiceManagerField(serviceManager, "context", null);
    }

    private void setServiceManagerField(ServiceManager serviceManager, String fieldName, Object value)
            throws Exception {
        Field field = ServiceManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(serviceManager, value);
    }

    private static class NoOpNavigationService implements INavigationService {
        @Override
        public void setTargets(java.util.List<Integer> targetIds, IResultCallback<Void> callback) {
        }

        @Override
        public void setTargetNodes(java.util.List<NavigationNode> targets, IResultCallback<Void> callback) {
        }

        @Override
        public void prepare(IResultCallback<Void> callback) {
        }

        @Override
        public void start(IResultCallback<Void> callback) {
        }

        @Override
        public void pause(IResultCallback<Void> callback) {
        }

        @Override
        public void stop(IResultCallback<Void> callback) {
        }

        @Override
        public void pilotNext(IResultCallback<Void> callback) {
        }

        @Override
        public void skipTo(int index, IResultCallback<Void> callback) {
        }

        @Override
        public void setSpeed(int speed, IResultCallback<Void> callback) {
        }

        @Override
        public void setRoutePolicy(int policy, IResultCallback<Void> callback) {
        }

        @Override
        public void setBlockingTimeout(int timeout, IResultCallback<Void> callback) {
        }

        @Override
        public void setRepeatCount(int count, IResultCallback<Void> callback) {
        }

        @Override
        public void setAutoRepeat(boolean enabled, IResultCallback<Void> callback) {
        }

        @Override
        public void setArrivalControlEnabled(boolean enabled, IResultCallback<Void> callback) {
        }

        @Override
        public void cancelArrivalControl(IResultCallback<Void> callback) {
        }

        @Override
        public void manual(int direction, IResultCallback<Void> callback) {
        }

        @Override
        public void getRouteNodes(IResultCallback<java.util.List<NavigationNode>> callback) {
        }

        @Override
        public void getCurrentNode(IResultCallback<NavigationNode> callback) {
        }

        @Override
        public void getNextNode(IResultCallback<NavigationNode> callback) {
        }

        @Override
        public void getCurrentPosition(IResultCallback<Integer> callback) {
        }

        @Override
        public void isLastNode(IResultCallback<Boolean> callback) {
        }

        @Override
        public void isLastRepeat(IResultCallback<Boolean> callback) {
        }

        @Override
        public void registerCallback(INavigationCallback callback) {
        }

        @Override
        public void unregisterCallback(INavigationCallback callback) {
        }

        @Override
        public void release() {
        }
    }
}
