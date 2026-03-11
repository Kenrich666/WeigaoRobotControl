package com.weigao.robot.control.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LowBatteryAutoChargeSettingsManagerTest {

    @Test
    public void usesDefaultsWhenFileMissing() throws IOException {
        File root = Files.createTempDirectory("low-battery-settings-default").toFile();

        LowBatteryAutoChargeSettingsManager manager = new LowBatteryAutoChargeSettingsManager(root);

        assertFalse(manager.isEnabled());
        assertEquals(20, manager.getThresholdPercent());
    }

    @Test
    public void persistsAndReloadsSettings() throws IOException {
        File root = Files.createTempDirectory("low-battery-settings-persist").toFile();

        LowBatteryAutoChargeSettingsManager manager = new LowBatteryAutoChargeSettingsManager(root);
        manager.update(true, 35);

        LowBatteryAutoChargeSettingsManager reloaded = new LowBatteryAutoChargeSettingsManager(root);

        assertTrue(reloaded.isEnabled());
        assertEquals(35, reloaded.getThresholdPercent());
    }

    @Test
    public void clampsThresholdIntoValidRange() throws IOException {
        File root = Files.createTempDirectory("low-battery-settings-clamp").toFile();

        LowBatteryAutoChargeSettingsManager manager = new LowBatteryAutoChargeSettingsManager(root);
        manager.setThresholdPercent(0);
        assertEquals(1, manager.getThresholdPercent());

        manager.setThresholdPercent(101);
        assertEquals(100, manager.getThresholdPercent());
    }
}
