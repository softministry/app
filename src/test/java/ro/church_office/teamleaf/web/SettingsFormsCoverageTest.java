package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsFormsCoverageTest {

    @Test
    void appearanceFormConstructorAndAccessorsWork() {
        SettingsWebController.AppearanceForm form = new SettingsWebController.AppearanceForm("s", "p", "n");
        assertEquals("s", form.getStatusCustomization());
        assertEquals("p", form.getPriorityCustomization());
        assertEquals("n", form.getNameCustomization());

        form.setStatusCustomization("s2");
        form.setPriorityCustomization("p2");
        form.setNameCustomization("n2");

        assertEquals("s2", form.getStatusCustomization());
        assertEquals("p2", form.getPriorityCustomization());
        assertEquals("n2", form.getNameCustomization());
    }

    @Test
    void churchAndSystemFormsExposeAllFields() {
        SettingsWebController.ChurchSettingsForm church = new SettingsWebController.ChurchSettingsForm(
                1L, 2L, "Betania", "avatar", "addr", "pastor", "0700", "sec", "0701", "trez", "0702");
        assertEquals(1L, church.getDefaultChurchId());
        assertEquals(2L, church.getCurrentChurchId());
        assertEquals("Betania", church.getCurrentChurchName());
        assertEquals("avatar", church.getAvatarUrl());
        assertEquals("addr", church.getAddress());

        church.setPastorName("p2");
        church.setPastorPhone("p3");
        church.setSecretaryName("s2");
        church.setSecretaryPhone("s3");
        church.setTreasurerName("t2");
        church.setTreasurerPhone("t3");
        assertEquals("p2", church.getPastorName());
        assertEquals("p3", church.getPastorPhone());
        assertEquals("s2", church.getSecretaryName());
        assertEquals("s3", church.getSecretaryPhone());
        assertEquals("t2", church.getTreasurerName());
        assertEquals("t3", church.getTreasurerPhone());

        SettingsWebController.SystemSettingsForm system = new SettingsWebController.SystemSettingsForm(10, true, false, true, false, true);
        assertEquals(10, system.getRowsPerPage());
        assertTrue(system.getEventTasksEnabled());
        assertFalse(system.getPrivateMode());
        assertTrue(system.getPasswordRestrictionsEnabled());
        assertFalse(system.getPasswordMinSixEnabled());
        assertTrue(system.getUserCreateRequirePassword());

        system.setRowsPerPage(25);
        system.setEventTasksEnabled(false);
        system.setPrivateMode(true);
        system.setPasswordRestrictionsEnabled(false);
        system.setPasswordMinSixEnabled(true);
        assertEquals(25, system.getRowsPerPage());
        assertFalse(system.getEventTasksEnabled());
        assertTrue(system.getPrivateMode());
        assertFalse(system.getPasswordRestrictionsEnabled());
        assertTrue(system.getPasswordMinSixEnabled());
    }

    @Test
    void runtimeAndPastoralFormsExposeAllFields() {
        SettingsWebController.RuntimeModeForm runtime = new SettingsWebController.RuntimeModeForm("dev", true);
        assertEquals("dev", runtime.getMode());
        assertTrue(runtime.getCloneFromCurrent());
        runtime.setMode("prod");
        runtime.setCloneFromCurrent(false);
        assertEquals("prod", runtime.getMode());
        assertFalse(runtime.getCloneFromCurrent());

        SettingsWebController.PastoralSettingsForm pastoral = new SettingsWebController.PastoralSettingsForm(
                3, 90, 14, 7, 12,
                true, true, true, true, true, true,
                "a", "b", "c", "d", "e", "f");
        assertEquals(3, pastoral.getPastoralAbsenceCount());
        assertEquals(90, pastoral.getPastoralAnalysisDays());
        assertEquals(14, pastoral.getPastoralBirthdayWindowDays());
        assertEquals(7, pastoral.getPastoralSnoozeDays());
        assertEquals(12, pastoral.getPastoralMaxRecommendations());
        assertTrue(pastoral.getPastoralEnableAbsence());
        assertTrue(pastoral.getPastoralEnableChildAbsence());
        assertTrue(pastoral.getPastoralEnableOverdueFollowUp());
        assertTrue(pastoral.getPastoralEnableWithoutGroup());
        assertTrue(pastoral.getPastoralEnableNewPerson());
        assertTrue(pastoral.getPastoralEnableBirthday());
        assertEquals("a", pastoral.getPastoralTemplateAbsence());
        assertEquals("f", pastoral.getPastoralTemplateBirthday());

        pastoral.setPastoralTemplateAbsence("x");
        pastoral.setPastoralTemplateChildAbsence("y");
        pastoral.setPastoralTemplateOverdueFollowUp("z");
        pastoral.setPastoralTemplateWithoutGroup("w");
        pastoral.setPastoralTemplateNewPerson("n");
        pastoral.setPastoralTemplateBirthday("b");
        assertEquals("x", pastoral.getPastoralTemplateAbsence());
        assertEquals("y", pastoral.getPastoralTemplateChildAbsence());
        assertEquals("z", pastoral.getPastoralTemplateOverdueFollowUp());
        assertEquals("w", pastoral.getPastoralTemplateWithoutGroup());
        assertEquals("n", pastoral.getPastoralTemplateNewPerson());
        assertEquals("b", pastoral.getPastoralTemplateBirthday());
    }
}
