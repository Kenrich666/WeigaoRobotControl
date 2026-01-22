package com.weigao.robot.control.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.util.Log;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.core.state.TimingController;
import com.weigao.robot.control.model.TaskTiming;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * TimingServiceImpl 的单元测试类。
 * 
 * 主要验证：
 * 1. 启动、暂停、恢复、停止计时逻辑。
 * 2. 计时历史记录的存储与清理。
 * 3. 舱门打开时自动停止计时的协同逻辑。
 * 
 * 技术难点：
 * - 模拟 TimingController 的构造和行为（因为它是 new 出来的）。
 * - 模拟静态类 ServiceManager 处理 IDoorService 的注入。
 */
public class TimingServiceImplTest {

    private TimingServiceImpl timingService;
    private MockedStatic<Log> mockedLogStatic;
    private MockedStatic<ServiceManager> mockedServiceManagerStatic;
    private MockedConstruction<TimingController> mockedControllerConstruction;

    @Mock
    private ServiceManager mockServiceManager;
    @Mock
    private IDoorService mockDoorService;
    @Mock
    private IResultCallback<Void> mockVoidCallback;
    @Mock
    private IResultCallback<TaskTiming> mockTimingCallback;
    @Mock
    private IResultCallback<List<TaskTiming>> mockHistoryCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 模拟 Log
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
        mockedLogStatic.when(() -> Log.w(anyString(), anyString())).thenReturn(0);

        // 模拟 ServiceManager 和 DoorService
        mockedServiceManagerStatic = mockStatic(ServiceManager.class);
        mockedServiceManagerStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        when(mockServiceManager.getDoorService()).thenReturn(mockDoorService);

        // 模拟 TimingController 的构造过程
        mockedControllerConstruction = mockConstruction(TimingController.class, (mock, context) -> {
            TaskTiming mockTiming = new TaskTiming();
            mockTiming.setTaskId((String) context.arguments().get(0));
            when(mock.getTiming()).thenReturn(mockTiming);
        });

        // 初始化 Service
        timingService = new TimingServiceImpl();
    }

    @After
    public void tearDown() {
        if (timingService != null)
            timingService.release();
        if (mockedLogStatic != null)
            mockedLogStatic.close();
        if (mockedServiceManagerStatic != null)
            mockedServiceManagerStatic.close();
        if (mockedControllerConstruction != null)
            mockedControllerConstruction.close();
    }

    /**
     * 测试启动计时
     */
    @Test
    public void testStartTiming() {
        String taskId = "task_001";
        timingService.startTiming(taskId, mockVoidCallback);

        // 验证 MockedConstruction 是否拦截到了构造
        assertFalse(mockedControllerConstruction.constructed().isEmpty());
        TimingController mockController = mockedControllerConstruction.constructed().get(0);

        // 验证是否调用了 start
        verify(mockController).start();
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试停止计时
     */
    @Test
    public void testStopTiming() {
        String taskId = "task_001";
        timingService.startTiming(taskId, null);

        TimingController mockController = mockedControllerConstruction.constructed().get(0);

        timingService.stopTiming(taskId, mockTimingCallback);

        // 验证调用了 stop
        verify(mockController).stop();
        // 验证回调返回了任务对象
        verify(mockTimingCallback).onSuccess(any(TaskTiming.class));
    }

    /**
     * 测试暂停与恢复
     */
    @Test
    public void testPauseAndResumeTiming() {
        String taskId = "task_001";
        timingService.startTiming(taskId, null);
        TimingController mockController = mockedControllerConstruction.constructed().get(0);

        // 暂停
        timingService.pauseTiming(taskId, mockVoidCallback);
        verify(mockController).pause();

        // 恢复
        timingService.resumeTiming(taskId, mockVoidCallback);
        verify(mockController).resume();
    }

    /**
     * 测试历史记录管理
     */
    @Test
    public void testHistoryManagement() {
        String taskId = "task_001";
        timingService.startTiming(taskId, null);
        timingService.stopTiming(taskId, null);

        // 获取历史
        timingService.getTimingHistory(null, mockHistoryCallback);
        verify(mockHistoryCallback).onSuccess(argThat(list -> list.size() == 1));

        // 清理历史
        timingService.clearTimingHistory(null, mockVoidCallback);
        timingService.getTimingHistory(null, mockHistoryCallback);
        verify(mockHistoryCallback, atLeastOnce()).onSuccess(argThat(List::isEmpty));
    }

    /**
     * 测试自动停止配置
     */
    @Test
    public void testSetAutoStopOnDoorOpen() {
        timingService.setAutoStopOnDoorOpen(false, mockVoidCallback);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试获取当前计时。
     */
    @Test
    public void testGetCurrentTiming() {
        String taskId = "task_001";
        timingService.startTiming(taskId, null);

        timingService.getCurrentTiming(taskId, mockTimingCallback);

        verify(mockTimingCallback).onSuccess(any(TaskTiming.class));
    }

    /**
     * 测试停止不存在的任务 - 异常路径。
     */
    @Test
    public void testStopTiming_NotExist() {
        String nonExistTaskId = "non_exist_task";

        timingService.stopTiming(nonExistTaskId, mockTimingCallback);

        // 验证错误回调
        verify(mockTimingCallback).onError(any());
    }

    /**
     * 测试暂停不存在的任务 - 异常路径。
     */
    @Test
    public void testPauseTiming_NotExist() {
        String nonExistTaskId = "non_exist_task";

        timingService.pauseTiming(nonExistTaskId, mockVoidCallback);

        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试恢复不存在的任务 - 异常路径。
     */
    @Test
    public void testResumeTiming_NotExist() {
        String nonExistTaskId = "non_exist_task";

        timingService.resumeTiming(nonExistTaskId, mockVoidCallback);

        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试重复启动同一任务。
     */
    @Test
    public void testStartTiming_Duplicate() {
        String taskId = "task_001";
        timingService.startTiming(taskId, null);

        // 再次启动同一任务
        IResultCallback<Void> callback = mock(IResultCallback.class);
        timingService.startTiming(taskId, callback);

        // 验证错误回调（或覆盖原任务）
        verify(callback).onError(any());
    }

    /**
     * 测试释放资源。
     */
    @Test
    public void testRelease() {
        timingService.release();
        // 验证不抛出异常
    }
}
