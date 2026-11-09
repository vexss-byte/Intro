package dev.introcinema.common.roster;

import java.util.UUID;

/**
 * One frozen roster slot. The UUID is the identity; the name is display only.
 * When {@code silhouette} is true the name must not be sent to clients until the reveal.
 */
public record RosterEntry(UUID uuid, int number, String name, boolean silhouette, String quote, boolean last) {
    public String numberLabel() { return String.format("%02d", number); }
}
