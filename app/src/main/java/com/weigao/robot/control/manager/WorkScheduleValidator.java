package com.weigao.robot.control.manager;

import com.weigao.robot.control.model.WorkSchedule;

import java.util.List;

/**
 * Validates work schedule edits before they are persisted and scheduled.
 */
public final class WorkScheduleValidator {

    public enum ValidationResult {
        VALID,
        NO_WORK_DAYS,
        INVALID_TIME_RANGE,
        OVERLAP_WITH_ENABLED_SCHEDULE
    }

    private WorkScheduleValidator() {
    }

    public static ValidationResult validate(List<WorkSchedule> schedules, int currentIndex, WorkSchedule candidate) {
        if (candidate == null) {
            return ValidationResult.INVALID_TIME_RANGE;
        }

        if (!candidate.isEnabled()) {
            return ValidationResult.VALID;
        }

        if (!hasAnyWorkDay(candidate.getWorkDays())) {
            return ValidationResult.NO_WORK_DAYS;
        }

        if (!hasValidTimeRange(candidate)) {
            return ValidationResult.INVALID_TIME_RANGE;
        }

        if (schedules == null) {
            return ValidationResult.VALID;
        }

        for (int i = 0; i < schedules.size(); i++) {
            if (i == currentIndex) {
                continue;
            }

            WorkSchedule other = schedules.get(i);
            if (other == null || !other.isEnabled()) {
                continue;
            }

            if (!hasAnyWorkDay(other.getWorkDays()) || !hasValidTimeRange(other)) {
                continue;
            }

            if (!hasSharedWorkDay(candidate.getWorkDays(), other.getWorkDays())) {
                continue;
            }

            if (hasTimeOverlap(candidate, other)) {
                return ValidationResult.OVERLAP_WITH_ENABLED_SCHEDULE;
            }
        }

        return ValidationResult.VALID;
    }

    private static boolean hasAnyWorkDay(boolean[] workDays) {
        if (workDays == null) {
            return false;
        }

        for (boolean workDay : workDays) {
            if (workDay) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasValidTimeRange(WorkSchedule schedule) {
        return getStartMinutes(schedule) < getEndMinutes(schedule);
    }

    private static boolean hasSharedWorkDay(boolean[] left, boolean[] right) {
        if (left == null || right == null) {
            return false;
        }

        int size = Math.min(left.length, right.length);
        for (int i = 0; i < size; i++) {
            if (left[i] && right[i]) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTimeOverlap(WorkSchedule left, WorkSchedule right) {
        return getStartMinutes(left) < getEndMinutes(right)
                && getStartMinutes(right) < getEndMinutes(left);
    }

    private static int getStartMinutes(WorkSchedule schedule) {
        return schedule.getStartHour() * 60 + schedule.getStartMinute();
    }

    private static int getEndMinutes(WorkSchedule schedule) {
        return schedule.getEndHour() * 60 + schedule.getEndMinute();
    }
}
