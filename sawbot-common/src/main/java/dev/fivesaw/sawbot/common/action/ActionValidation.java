package dev.fivesaw.sawbot.common.action;

public final class ActionValidation {
    private final boolean valid;
    private final String reason;

    private ActionValidation(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public static ActionValidation valid() { return new ActionValidation(true, "OK"); }
    public static ActionValidation invalid(String reason) { return new ActionValidation(false, reason); }
    public boolean isValid() { return valid; }
    public String reason() { return reason; }
}
