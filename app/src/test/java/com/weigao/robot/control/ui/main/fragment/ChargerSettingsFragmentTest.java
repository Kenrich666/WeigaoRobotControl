package com.weigao.robot.control.ui.main.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.weigao.robot.control.R;
import com.weigao.robot.control.manager.WorkScheduleService;
import com.weigao.robot.control.manager.WorkScheduleSettingsManager;
import com.weigao.robot.control.model.WorkSchedule;
import com.weigao.robot.control.service.IChargerService;
import com.weigao.robot.control.service.IRobotStateService;
import com.weigao.robot.control.service.ServiceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = ChargerSettingsFragmentTest.TestApplication.class)
public class ChargerSettingsFragmentTest {

    private AppCompatActivity activity;
    private ChargerSettingsFragment fragment;
    private WorkScheduleService originalWorkScheduleService;
    private WorkScheduleService workScheduleServiceSpy;

    @Before
    public void setUp() throws Exception {
        prepareServiceManager();
        prepareWorkScheduleServiceSpy();
        resetWorkScheduleSettingsManager();
    }

    @After
    public void tearDown() throws Exception {
        if (activity != null) {
            activity.finish();
        }
        restoreWorkScheduleService();
        resetWorkScheduleSettingsManager();
        resetServiceManager();
    }

    @Test
    public void enablingOverlappingScheduleRevertsSwitchAndDoesNotPersist() throws Exception {
        launchFragmentWithSchedules(
                schedule(true, "08:00", "12:00", true, false, false, false, false, false, false),
                schedule(false, "09:00", "11:00", true, false, false, false, false, false, false));

        View secondItem = getScheduleItem(1);
        SwitchCompat switchEnabled = secondItem.findViewById(R.id.switch_schedule_enabled);

        switchEnabled.setPressed(true);
        switchEnabled.performClick();
        switchEnabled.setPressed(false);
        idleMainLooper();

        assertFalse(switchEnabled.isChecked());
        assertFalse(WorkScheduleSettingsManager.getInstance().getSchedule(1).isEnabled());
        assertEquals("与已启用时间段冲突", ShadowToast.getTextOfLatestToast());
        verify(workScheduleServiceSpy, never()).rescheduleAll();
    }

    @Test
    public void deselectingLastWorkDayRevertsButtonStateAndDoesNotPersist() throws Exception {
        launchFragmentWithSchedules(
                schedule(true, "08:00", "12:00", true, false, false, false, false, false, false));

        View firstItem = getScheduleItem(0);
        MaterialButton mondayButton = firstItem.findViewById(R.id.btn_day_mon);

        mondayButton.performClick();
        idleMainLooper();

        assertEquals("至少选择一个工作日", ShadowToast.getTextOfLatestToast());
        assertEquals(getColor(android.R.color.white), mondayButton.getCurrentTextColor());
        assertEquals("08:00", WorkScheduleSettingsManager.getInstance().getSchedule(0).getStartTime());
        assertEquals(true, WorkScheduleSettingsManager.getInstance().getSchedule(0).getWorkDays()[0]);
        verify(workScheduleServiceSpy, never()).rescheduleAll();
    }

    @Test
    public void overlappingTimeChangeRevertsDisplayedTimeAndDoesNotPersist() throws Exception {
        launchFragmentWithSchedules(
                schedule(true, "08:00", "12:00", true, false, false, false, false, false, false),
                schedule(true, "12:00", "18:00", true, false, false, false, false, false, false));

        View secondItem = getScheduleItem(1);
        TextView startTimeView = secondItem.findViewById(R.id.tv_start_time);

        startTimeView.performClick();
        idleMainLooper();

        TimePickerDialog dialog = (TimePickerDialog) ShadowAlertDialog.getLatestAlertDialog();
        dialog.updateTime(11, 0);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        idleMainLooper();

        assertEquals("12:00", startTimeView.getText().toString());
        assertEquals("12:00", WorkScheduleSettingsManager.getInstance().getSchedule(1).getStartTime());
        assertEquals("与已启用时间段冲突", ShadowToast.getTextOfLatestToast());
        verify(workScheduleServiceSpy, never()).rescheduleAll();
    }

    @Test
    public void disabledScheduleCanStillEditTimeAndWorkDays() throws Exception {
        launchFragmentWithSchedules(
                schedule(false, "08:00", "17:00", true, false, false, false, false, false, false));

        View firstItem = getScheduleItem(0);
        TextView startTimeView = firstItem.findViewById(R.id.tv_start_time);
        MaterialButton tuesdayButton = firstItem.findViewById(R.id.btn_day_tue);

        startTimeView.performClick();
        idleMainLooper();

        TimePickerDialog dialog = (TimePickerDialog) ShadowAlertDialog.getLatestAlertDialog();
        dialog.updateTime(10, 30);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        idleMainLooper();

        tuesdayButton.performClick();
        idleMainLooper();

        WorkSchedule updated = WorkScheduleSettingsManager.getInstance().getSchedule(0);
        assertEquals("10:30", updated.getStartTime());
        assertEquals("10:30", startTimeView.getText().toString());
        assertEquals(true, updated.getWorkDays()[1]);
        verify(workScheduleServiceSpy, times(2)).rescheduleAll();
    }

    private void launchFragmentWithSchedules(WorkSchedule... schedules) throws Exception {
        configureSchedules(Arrays.asList(schedules));

        activity = Robolectric.buildActivity(TestActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(View.generateViewId());
        activity.setContentView(container);

        fragment = new ChargerSettingsFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(container.getId(), fragment)
                .commitNow();

        idleMainLooper();
        clearInvocations(workScheduleServiceSpy);
    }

    private View getScheduleItem(int index) {
        LinearLayout scheduleContainer = fragment.requireView().findViewById(R.id.schedule_container);
        return scheduleContainer.getChildAt(index);
    }

    private void configureSchedules(List<WorkSchedule> schedules) throws Exception {
        resetWorkScheduleSettingsManager();
        WorkScheduleSettingsManager manager = WorkScheduleSettingsManager.getInstance();
        Field schedulesField = WorkScheduleSettingsManager.class.getDeclaredField("schedules");
        schedulesField.setAccessible(true);
        schedulesField.set(manager, new ArrayList<>(schedules));
    }

    private void prepareServiceManager() throws Exception {
        ServiceManager serviceManager = ServiceManager.getInstance();
        setServiceManagerField(serviceManager, "initialized", true);
        setServiceManagerField(serviceManager, "chargerService", mock(IChargerService.class));
        setServiceManagerField(serviceManager, "robotStateService", mock(IRobotStateService.class));
    }

    private void resetServiceManager() throws Exception {
        ServiceManager serviceManager = ServiceManager.getInstance();
        setServiceManagerField(serviceManager, "chargerService", null);
        setServiceManagerField(serviceManager, "robotStateService", null);
        setServiceManagerField(serviceManager, "initialized", false);
        setServiceManagerField(serviceManager, "context", null);
    }

    private void setServiceManagerField(ServiceManager serviceManager, String fieldName, Object value)
            throws Exception {
        Field field = ServiceManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(serviceManager, value);
    }

    private void prepareWorkScheduleServiceSpy() throws Exception {
        originalWorkScheduleService = WorkScheduleService.getInstance();
        workScheduleServiceSpy = spy(originalWorkScheduleService);
        Field instanceField = WorkScheduleService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, workScheduleServiceSpy);
    }

    private void restoreWorkScheduleService() throws Exception {
        if (originalWorkScheduleService == null) {
            return;
        }

        Field instanceField = WorkScheduleService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, originalWorkScheduleService);
    }

    private void resetWorkScheduleSettingsManager() throws Exception {
        deletePersistedSettingsFile();
        Field instanceField = WorkScheduleSettingsManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void deletePersistedSettingsFile() {
        File settingsFile = new File(Environment.getExternalStorageDirectory(),
                "WeigaoRobot/config/work_schedule_settings.json");
        if (settingsFile.exists()) {
            settingsFile.delete();
        }
    }

    private void idleMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private int getColor(int colorRes) {
        return activity.getResources().getColor(colorRes);
    }

    private WorkSchedule schedule(boolean enabled, String startTime, String endTime, boolean... workDays) {
        return new WorkSchedule(enabled, startTime, endTime, workDays);
    }

    public static class TestActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.Theme_WeigaoRobotControl);
            super.onCreate(savedInstanceState);
        }
    }

    public static class TestApplication extends Application {
    }
}
