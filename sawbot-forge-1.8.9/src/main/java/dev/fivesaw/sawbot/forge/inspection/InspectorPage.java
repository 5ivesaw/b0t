package dev.fivesaw.sawbot.forge.inspection;

public enum InspectorPage {
    SUMMARY,
    BODY,
    TERRAIN,
    ENTITIES,
    INVENTORY,
    EVENTS,
    DIFFERENCE,
    SYSTEM;

    public InspectorPage next() {
        InspectorPage[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
