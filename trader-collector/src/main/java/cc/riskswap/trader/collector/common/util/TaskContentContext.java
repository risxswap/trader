package cc.riskswap.trader.collector.common.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TaskContentContext {

    private static final ThreadLocal<MutableSnapshot> CONTEXT = new ThreadLocal<>();

    private TaskContentContext() {
    }

    public static void start() {
        CONTEXT.set(new MutableSnapshot());
    }

    public static Snapshot current() {
        MutableSnapshot snapshot = CONTEXT.get();
        if (snapshot == null) {
            return Snapshot.empty();
        }
        return snapshot.toSnapshot();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void addMetric(String name, long delta) {
        MutableSnapshot snapshot = CONTEXT.get();
        if (snapshot == null || name == null || name.isBlank()) {
            return;
        }
        snapshot.metrics.merge(name, delta, Long::sum);
    }

    public static void addAttribute(String name, String value) {
        MutableSnapshot snapshot = CONTEXT.get();
        if (snapshot == null || name == null || name.isBlank() || value == null || value.isBlank()) {
            return;
        }
        snapshot.attributes.put(name, value);
    }

    public static void addDetail(String section, String detail) {
        MutableSnapshot snapshot = CONTEXT.get();
        if (snapshot == null || section == null || section.isBlank() || detail == null || detail.isBlank()) {
            return;
        }
        snapshot.details.computeIfAbsent(section, key -> new ArrayList<>()).add(detail);
    }

    public static void addError(String error) {
        MutableSnapshot snapshot = CONTEXT.get();
        if (snapshot == null || error == null || error.isBlank()) {
            return;
        }
        snapshot.errors.add(error);
    }

    public static final class Snapshot {
        private final Map<String, String> attributes;
        private final Map<String, Long> metrics;
        private final Map<String, List<String>> details;
        private final List<String> errors;

        private Snapshot(Map<String, String> attributes, Map<String, Long> metrics, Map<String, List<String>> details,
                List<String> errors) {
            this.attributes = attributes;
            this.metrics = metrics;
            this.details = details;
            this.errors = errors;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public Map<String, Long> getMetrics() {
            return metrics;
        }

        public Map<String, List<String>> getDetails() {
            return details;
        }

        public List<String> getErrors() {
            return errors;
        }

        private static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of(), Map.of(), List.of());
        }
    }

    private static final class MutableSnapshot {
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final Map<String, Long> metrics = new LinkedHashMap<>();
        private final Map<String, List<String>> details = new LinkedHashMap<>();
        private final List<String> errors = new ArrayList<>();

        private Snapshot toSnapshot() {
            Map<String, String> attributeSnapshot = new LinkedHashMap<>(attributes);
            Map<String, Long> metricSnapshot = new LinkedHashMap<>(metrics);
            Map<String, List<String>> detailSnapshot = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : details.entrySet()) {
                detailSnapshot.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new Snapshot(
                    Map.copyOf(attributeSnapshot),
                    Map.copyOf(metricSnapshot),
                    Map.copyOf(detailSnapshot),
                    List.copyOf(errors));
        }
    }
}
