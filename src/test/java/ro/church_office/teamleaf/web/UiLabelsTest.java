package ro.church_office.teamleaf.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiLabelsTest {

    private final UiLabels uiLabels = new UiLabels();

    @Test
    void returnsKnownGroupTypeLabel() {
        assertEquals("Grup mic", uiLabels.groupType("SMALL_GROUP"));
    }

    @Test
    void fallsBackToDashOnNull() {
        assertEquals("—", uiLabels.groupType(null));
        assertEquals("—", uiLabels.memberType(" "));
    }

    @Test
    void humanizesUnknownEnumStyleValues() {
        assertEquals("Custom Value", uiLabels.eventType("CUSTOM_VALUE"));
        assertEquals("Another Value", uiLabels.eventStatus("ANOTHER-VALUE"));
    }

    @Test
    void returnsKnownFollowUpStatusLabel() {
        assertEquals("În lucru", uiLabels.followUpStatus("IN_PROGRESS"));
    }

    @Test
    void returnsKnownPriorityLabel() {
        assertEquals("Medie-ridicată", uiLabels.priority("HIGH_MEDIUM"));
    }
}
