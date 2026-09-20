package com.scaffold.workflow.domain;

import java.util.Map;

public final class WorkflowStatus {

    public static final String DRAFT = "DRAFT";
    public static final String TODO = "TODO";
    public static final String PASS = "PASS";
    public static final String REJECT = "REJECT";
    public static final String RETURN = "RETURN";
    public static final String REVOKE = "REVOKE";

    private static final Map<String, String> ACTION_LABELS = Map.of(
            "pass", "通过",
            "reject", "驳回",
            "return", "退回",
            "revoke", "撤回",
            "start", "发起"
    );

    private WorkflowStatus() {
    }

    public static String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            return "pass";
        }
        return switch (action) {
            case "reject", "return", "revoke", "start" -> action;
            default -> "pass";
        };
    }

    public static String terminalStatusOfAction(String action) {
        return switch (normalizeAction(action)) {
            case "reject" -> REJECT;
            case "return" -> RETURN;
            case "revoke" -> REVOKE;
            default -> PASS;
        };
    }

    public static String labelOfAction(String action) {
        return ACTION_LABELS.getOrDefault(normalizeAction(action), "通过");
    }

    public static String labelOfStatus(String status) {
        return switch (status == null ? "" : status) {
            case REJECT -> "驳回";
            case RETURN -> "退回";
            case REVOKE -> "撤回";
            case TODO -> "审批中";
            case DRAFT -> "草稿";
            default -> "通过";
        };
    }

    public static String toneOfAction(String action) {
        return switch (normalizeAction(action)) {
            case "reject" -> "danger";
            case "return" -> "warn";
            case "revoke" -> "neutral";
            case "start" -> "info";
            default -> "ok";
        };
    }

    public static String toneOfStatus(String status) {
        return switch (status == null ? "" : status) {
            case REJECT -> "danger";
            case RETURN -> "warn";
            case REVOKE -> "neutral";
            case TODO -> "warn";
            default -> "ok";
        };
    }
}
