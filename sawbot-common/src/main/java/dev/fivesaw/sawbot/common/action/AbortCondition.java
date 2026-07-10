package dev.fivesaw.sawbot.common.action;

public enum AbortCondition {
    NONE,
    TIMEOUT,
    TARGET_LOST,
    THREAT_INCREASE,
    LOW_HEALTH,
    NO_ROUTE,
    NO_RESOURCE,
    SKILL_FAILURE,
    MANUAL_TAKEOVER
}
