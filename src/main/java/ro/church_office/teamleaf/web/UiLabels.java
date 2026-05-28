package ro.church_office.teamleaf.web;

import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

@Component("uiLabels")
public class UiLabels {

    private static final Map<String, String> GROUP_TYPE_LABELS = Map.ofEntries(
            Map.entry("SMALL_GROUP", "Grup mic"),
            Map.entry("TEAM", "Echipă"),
            Map.entry("CLASS", "Clasă"),
            Map.entry("MINISTRY", "Slujire"),
            Map.entry("CHOIR", "Cor"),
            Map.entry("YOUTH", "Tineri"),
            Map.entry("CHILDREN", "Copii"),
            Map.entry("FAMILY", "Familie"),
            Map.entry("PRAYER", "Rugăciune"),
            Map.entry("OTHER", "Altele")
    );
    private static final Map<String, String> MEMBER_TYPE_LABELS = Map.ofEntries(
            Map.entry("MEMBER", "Membri"),
            Map.entry("CHILD", "Copii"),
            Map.entry("FREND", "Prieteni")
    );
    private static final Map<String, String> EVENT_STATUS_LABELS = Map.ofEntries(
            Map.entry("PLANNED", "Planificate"),
            Map.entry("IN_PROGRESS", "În desfășurare"),
            Map.entry("DONE", "Finalizate"),
            Map.entry("CANCELED", "Anulat")
    );
    private static final Map<String, String> EVENT_TYPE_LABELS = Map.ofEntries(
            Map.entry("SERVICE", "Serviciu divin"),
            Map.entry("MEETING", "Întâlnire"),
            Map.entry("SMALL_GROUP", "Grup mic"),
            Map.entry("CONFERENCE", "Conferință"),
            Map.entry("SPECIAL", "Special"),
            Map.entry("OTHER", "Alt tip")
    );
    private static final Map<String, String> RECURRENCE_LABELS = Map.ofEntries(
            Map.entry("NONE", "Fără recurență"),
            Map.entry("DAILY", "Zilnic"),
            Map.entry("WEEKLY", "Săptămânal"),
            Map.entry("MONTHLY", "Lunar")
    );
    private static final Map<String, String> ATTENDANCE_SESSION_LABELS = Map.ofEntries(
            Map.entry("MORNING", "Dimineață"),
            Map.entry("EVENING", "Seară"),
            Map.entry("DEFAULT", "Implicit")
    );
    private static final Map<String, String> ATTENDANCE_STATUS_LABELS = Map.ofEntries(
            Map.entry("PRESENT", "Prezent"),
            Map.entry("ABSENT", "Absent")
    );
    private static final Map<String, String> FOLLOW_UP_STATUS_LABELS = Map.ofEntries(
            Map.entry("OPEN", "Deschis"),
            Map.entry("IN_PROGRESS", "În lucru"),
            Map.entry("DONE", "Finalizat")
    );
    private static final Map<String, String> PRIORITY_LABELS = Map.ofEntries(
            Map.entry("HIGH", "Ridicată"),
            Map.entry("HIGH_MEDIUM", "Medie-ridicată"),
            Map.entry("MEDIUM", "Medie"),
            Map.entry("MEDIUM_LOW", "Medie-scăzută"),
            Map.entry("LOW", "Scăzută")
    );
    private static final Map<String, String> TASK_STATUS_LABELS = Map.ofEntries(
            Map.entry("TODO", "De făcut"),
            Map.entry("PLANNED", "Planificate"),
            Map.entry("IN_PROGRESS", "În lucru"),
            Map.entry("DONE", "Finalizate"),
            Map.entry("CANCELED", "Anulat")
    );

    public String groupType(Object rawType) {
        if (rawType == null) {
            return "—";
        }
        String key = rawType.toString();
        if (key == null || key.isBlank()) {
            return "—";
        }
        return GROUP_TYPE_LABELS.getOrDefault(key, humanizeEnumLikeText(key));
    }

    public String memberType(Object rawType) {
        if (rawType == null) {
            return "—";
        }
        String key = rawType.toString();
        if (key == null || key.isBlank()) {
            return "—";
        }
        return MEMBER_TYPE_LABELS.getOrDefault(key, humanizeEnumLikeText(key));
    }

    public String eventStatus(Object rawStatus) {
        return resolveLabel(rawStatus, EVENT_STATUS_LABELS, "—");
    }

    public String eventType(Object rawType) {
        return resolveLabel(rawType, EVENT_TYPE_LABELS, "—");
    }

    public String recurrenceType(Object rawType) {
        return resolveLabel(rawType, RECURRENCE_LABELS, "—");
    }

    public String attendanceSession(Object rawSession) {
        return resolveLabel(rawSession, ATTENDANCE_SESSION_LABELS, "—");
    }

    public String attendanceStatus(Object rawStatus) {
        return resolveLabel(rawStatus, ATTENDANCE_STATUS_LABELS, "—");
    }

    public String followUpStatus(Object rawStatus) {
        return resolveLabel(rawStatus, FOLLOW_UP_STATUS_LABELS, "—");
    }

    public String priority(Object rawPriority) {
        return resolveLabel(rawPriority, PRIORITY_LABELS, "—");
    }

    public String taskStatus(Object rawStatus) {
        return resolveLabel(rawStatus, TASK_STATUS_LABELS, "—");
    }

    public String moneyLei(Object rawAmount) {
        double amount;
        if (rawAmount instanceof Number number) {
            amount = number.doubleValue();
        } else if (rawAmount != null) {
            try {
                amount = Double.parseDouble(rawAmount.toString());
            } catch (NumberFormatException ex) {
                amount = 0.0;
            }
        } else {
            amount = 0.0;
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ro", "RO"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(amount) + " lei";
    }

    private String resolveLabel(Object rawValue, Map<String, String> labels, String fallbackWhenBlank) {
        if (rawValue == null) {
            return fallbackWhenBlank;
        }
        String key = rawValue.toString();
        if (key == null || key.isBlank()) {
            return fallbackWhenBlank;
        }
        return labels.getOrDefault(key, humanizeEnumLikeText(key));
    }

    private String humanizeEnumLikeText(String value) {
        String normalized = value.trim().replace('-', '_');
        String[] parts = normalized.split("_");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String lower = part.toLowerCase(Locale.ROOT);
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                label.append(lower.substring(1));
            }
        }
        return label.length() == 0 ? value : label.toString();
    }
}
