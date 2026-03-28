package com.weigao.robot.control.manager;

import static org.junit.Assert.assertEquals;

import com.weigao.robot.control.model.WorkSchedule;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class WorkScheduleValidatorTest {

    @Test
    public void enabledScheduleRequiresAtLeastOneWorkDay() {
        WorkSchedule candidate = schedule(true, "08:00", "17:00",
                false, false, false, false, false, false, false);

        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                Collections.singletonList(candidate), 0, candidate);

        assertEquals(WorkScheduleValidator.ValidationResult.NO_WORK_DAYS, result);
    }

    @Test
    public void equalStartAndEndTimeIsRejected() {
        WorkSchedule candidate = schedule(true, "08:00", "08:00",
                true, false, false, false, false, false, false);

        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                Collections.singletonList(candidate), 0, candidate);

        assertEquals(WorkScheduleValidator.ValidationResult.INVALID_TIME_RANGE, result);
    }

    @Test
    public void crossDayTimeRangeIsRejected() {
        WorkSchedule candidate = schedule(true, "22:00", "06:00",
                true, false, false, false, false, false, false);

        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                Collections.singletonList(candidate), 0, candidate);

        assertEquals(WorkScheduleValidator.ValidationResult.INVALID_TIME_RANGE, result);
    }

    @Test
    public void overlapOnSharedWorkDayIsRejected() {
        WorkSchedule existing = schedule(true, "08:00", "12:00",
                true, false, false, false, false, false, false);
        WorkSchedule candidate = schedule(true, "11:00", "15:00",
                true, false, false, false, false, false, false);

        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                Arrays.asList(existing, candidate), 1, candidate);

        assertEquals(WorkScheduleValidator.ValidationResult.OVERLAP_WITH_ENABLED_SCHEDULE, result);
    }

    @Test
    public void sameTimeOnDifferentWorkDaysIsAllowed() {
        WorkSchedule existing = schedule(true, "08:00", "12:00",
                true, false, false, false, false, false, false);
        WorkSchedule candidate = schedule(true, "08:00", "12:00",
                false, true, false, false, false, false, false);

        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                Arrays.asList(existing, candidate), 1, candidate);

        assertEquals(WorkScheduleValidator.ValidationResult.VALID, result);
    }

    @Test
    public void adjacentTimeRangesAreAllowed() {
        WorkSchedule existing = schedule(true, "08:00", "12:00",
                true, false, false, false, false, false, false);
        WorkSchedule candidate = schedule(true, "12:00", "18:00",
                true, false, false, false, false, false, false);

        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                Arrays.asList(existing, candidate), 1, candidate);

        assertEquals(WorkScheduleValidator.ValidationResult.VALID, result);
    }

    @Test
    public void disabledScheduleDoesNotBlockOverlapValidation() {
        WorkSchedule existingDisabled = schedule(false, "08:00", "12:00",
                true, false, false, false, false, false, false);
        WorkSchedule candidate = schedule(true, "09:00", "11:00",
                true, false, false, false, false, false, false);

        WorkScheduleValidator.ValidationResult result = WorkScheduleValidator.validate(
                Arrays.asList(existingDisabled, candidate), 1, candidate);

        assertEquals(WorkScheduleValidator.ValidationResult.VALID, result);
    }

    private WorkSchedule schedule(boolean enabled, String startTime, String endTime, boolean... workDays) {
        return new WorkSchedule(enabled, startTime, endTime, workDays);
    }
}
