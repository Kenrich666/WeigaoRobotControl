package com.weigao.robot.control.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.ServiceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

/**
 * SecurityServiceImpl 的单元测试类。
 * 
 * 主要验证：
 * 1. 密码验证逻辑。
 * 2. 安全锁启用/禁用状态持久化。
 * 3. 锁定/解锁状态切换。
 * 4. 协同 IDoorService 执行开门操作。
 * 
 * 技术难点：
 * - 模拟 SharedPreferences 的链式调用。
 * - 模拟静态类 ServiceManager 和 Log。
 */
public class SecurityServiceImplTest {

    private SecurityServiceImpl securityService;
    private MockedStatic<Log> mockedLogStatic;
    private MockedStatic<ServiceManager> mockedServiceManagerStatic;

    @Mock
    private Context mockContext;
    @Mock
    private SharedPreferences mockPrefs;
    @Mock
    private SharedPreferences.Editor mockEditor;
    @Mock
    private ServiceManager mockServiceManager;
    @Mock
    private IDoorService mockDoorService;
    @Mock
    private IResultCallback<Void> mockVoidCallback;
    @Mock
    private IResultCallback<Boolean> mockBooleanCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 模拟 Log
        mockedLogStatic = mockStatic(Log.class);
        mockedLogStatic.when(() -> Log.d(anyString(), anyString())).thenReturn(0);

        // 模拟 SharedPreferences
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);

        // 模拟 ServiceManager 单例
        mockedServiceManagerStatic = mockStatic(ServiceManager.class);
        mockedServiceManagerStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        when(mockServiceManager.getDoorService()).thenReturn(mockDoorService);

        // 初始化 Service
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        securityService = new SecurityServiceImpl(mockContext);
    }

    @After
    public void tearDown() {
        if (mockedLogStatic != null)
            mockedLogStatic.close();
        if (mockedServiceManagerStatic != null)
            mockedServiceManagerStatic.close();
    }

    /**
     * 测试密码验证：正确密码场景
     */
    @Test
    public void testVerifyPassword_Correct() {
        // 预设 SP 中的密码
        when(mockPrefs.getString(eq("security_password"), anyString())).thenReturn("123456");

        securityService.verifyPassword("123456", mockBooleanCallback);

        verify(mockBooleanCallback).onSuccess(true);
    }

    /**
     * 测试密码验证：错误密码场景
     */
    @Test
    public void testVerifyPassword_Incorrect() {
        when(mockPrefs.getString(eq("security_password"), anyString())).thenReturn("123456");

        securityService.verifyPassword("wrong_pwd", mockBooleanCallback);

        verify(mockBooleanCallback).onSuccess(false);
    }

    /**
     * 测试设置新密码（首次设置场景）
     */
    @Test
    public void testSetPassword_Success_FirstTime() {
        // 模拟首次设置密码（KEY_PASSWORD_SET = false）
        when(mockPrefs.getBoolean(eq("security_password_set"), anyBoolean())).thenReturn(false);
        when(mockPrefs.getString(eq("security_password"), anyString())).thenReturn("123456");

        securityService.setPassword("any_old", "654321", mockVoidCallback);

        // 验证是否执行了保存动作
        verify(mockEditor).putString("security_password", "654321");
        verify(mockEditor).putBoolean("security_password_set", true);
        verify(mockEditor).apply();
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试锁定功能
     */
    @Test
    public void testLock() {
        securityService.lock(mockVoidCallback);

        // 验证锁定状态持久化
        verify(mockEditor).putBoolean("security_is_locked", true);
        verify(mockVoidCallback).onSuccess(null);

        securityService.isLocked(mockBooleanCallback);
        verify(mockBooleanCallback).onSuccess(true);
    }

    /**
     * 测试解锁功能
     */
    @Test
    public void testUnlock_Success() {
        when(mockPrefs.getString(anyString(), anyString())).thenReturn("123456");

        securityService.unlock("123456", mockVoidCallback);

        verify(mockEditor).putBoolean("security_is_locked", false);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试验证密码并开门
     */
    @Test
    public void testUnlockDoor_Success() {
        when(mockPrefs.getString(anyString(), anyString())).thenReturn("123456");
        int doorId = 1;

        securityService.unlockDoor(doorId, "123456", mockVoidCallback);

        // 验证是否调用了 DoorService 开门
        verify(mockDoorService).openDoor(eq(doorId), eq(false), any());
        // 验证状态变为解锁
        verify(mockEditor).putBoolean("security_is_locked", false);
    }

    /**
     * 测试启用/禁用安全锁功能
     */
    @Test
    public void testSetSecurityLockEnabled() {
        securityService.setSecurityLockEnabled(false, mockVoidCallback);

        verify(mockEditor).putBoolean("security_enabled", false);
        verify(mockVoidCallback).onSuccess(null);
    }

    /**
     * 测试查询安全锁是否启用。
     */
    @Test
    public void testIsSecurityLockEnabled() {
        when(mockPrefs.getBoolean(eq("security_enabled"), anyBoolean())).thenReturn(true);

        // 需要重新创建 Service 以读取模拟值
        SecurityServiceImpl newService = new SecurityServiceImpl(mockContext);
        newService.isSecurityLockEnabled(mockBooleanCallback);

        verify(mockBooleanCallback).onSuccess(true);
    }

    /**
     * 测试设置密码失败 - 旧密码错误。
     * <p>
     * 模拟已设置过密码的场景（KEY_PASSWORD_SET = true），输入错误的旧密码应失败。
     * </p>
     */
    @Test
    public void testSetPassword_WrongOldPassword() {
        // 模拟已设置过密码
        when(mockPrefs.getBoolean(eq("security_password_set"), anyBoolean())).thenReturn(true);
        when(mockPrefs.getString(eq("security_password"), anyString())).thenReturn("correct_pwd");

        securityService.setPassword("wrong_old", "new_pwd", mockVoidCallback);

        // 验证错误回调被调用
        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试解锁失败 - 密码错误。
     */
    @Test
    public void testUnlock_WrongPassword() {
        when(mockPrefs.getString(anyString(), anyString())).thenReturn("123456");

        securityService.unlock("wrong_pwd", mockVoidCallback);

        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试释放资源。
     */
    @Test
    public void testRelease() {
        securityService.release();
        // 验证正常执行不抛异常
    }

    /**
     * 测试解锁舱门失败 - 密码错误。
     */
    @Test
    public void testUnlockDoor_WrongPassword() {
        when(mockPrefs.getString(anyString(), anyString())).thenReturn("123456");

        securityService.unlockDoor(1, "wrong_pwd", mockVoidCallback);

        // 验证 DoorService 未被调用
        verify(mockDoorService, times(0)).openDoor(anyInt(), anyBoolean(), any());
        // 验证错误回调
        verify(mockVoidCallback).onError(any());
    }

    /**
     * 测试禁用安全锁后自动解锁。
     */
    @Test
    public void testSetSecurityLockDisabled_AutoUnlock() {
        // 先锁定
        securityService.lock(null);

        // 禁用安全锁
        securityService.setSecurityLockEnabled(false, mockVoidCallback);

        // 验证锁定状态变为 false
        verify(mockEditor).putBoolean("security_is_locked", false);
    }
}
