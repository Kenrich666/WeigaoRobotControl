package com.weigao.robot.control.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class TaskExecutionStateManagerTest {

    @After
    public void tearDown() {
        TaskExecutionStateManager.getInstance().resetForTest();
    }

    @Test
    public void startTaskMarksTaskAsActive() {
        TaskExecutionStateManager manager = TaskExecutionStateManager.getInstance();

        manager.startTask(TaskType.ITEM_DELIVERY);

        assertTrue(manager.hasActiveTask());
        assertEquals(TaskType.ITEM_DELIVERY, manager.getActiveTaskType());
    }

    @Test
    public void finishTaskClearsActiveState() {
        TaskExecutionStateManager manager = TaskExecutionStateManager.getInstance();
        manager.startTask(TaskType.CIRCULAR_DELIVERY);

        manager.finishTask();

        assertFalse(manager.hasActiveTask());
        assertNull(manager.getActiveTaskType());
    }

    @Test
    public void cancelTaskClearsActiveState() {
        TaskExecutionStateManager manager = TaskExecutionStateManager.getInstance();
        manager.startTask(TaskType.ITEM_DELIVERY);

        manager.cancelTask();

        assertFalse(manager.hasActiveTask());
        assertNull(manager.getActiveTaskType());
    }
}
