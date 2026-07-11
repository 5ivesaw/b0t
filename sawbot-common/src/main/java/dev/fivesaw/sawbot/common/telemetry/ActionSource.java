package dev.fivesaw.sawbot.common.telemetry;

/** Identifies who produced the action window associated with a trajectory step. */
public enum ActionSource {
    HUMAN,
    MODEL,
    TEACHER,
    NONE
}
