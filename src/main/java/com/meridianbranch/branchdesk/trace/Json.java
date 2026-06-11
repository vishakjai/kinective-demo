package com.meridianbranch.branchdesk.trace;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A minimal, deterministic JSON writer — just enough for trace records. We do
 * NOT pull in Jackson/Gson on purpose: byte-stable output requires total
 * control over key ordering and number formatting. Maps serialize in their
 * iteration order (callers use {@link java.util.LinkedHashMap}); BigDecimal
 * serializes via {@code toPlainString()} so money never drifts to exponent
 * notation or a platform-locale decimal.
 */
public final class Json {
    private Json() { }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            writeString(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(((Boolean) v) ? "true" : "false");
        } else if (v instanceof BigDecimal) {
            sb.append(((BigDecimal) v).toPlainString());
        } else if (v instanceof Number) {
            sb.append(v.toString());
        } else if (v instanceof Enum<?>) {
            writeString(sb, ((Enum<?>) v).name());
        } else if (v instanceof Map<?, ?>) {
            writeObject(sb, (Map<?, ?>) v);
        } else if (v instanceof List<?>) {
            writeArray(sb, (List<?>) v);
        } else {
            writeString(sb, v.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
