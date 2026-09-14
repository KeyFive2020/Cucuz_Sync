package br.com.cuscuz.sync.model;

import java.util.List;

public record SyncPlan(List<PlanAction> actions) {
    public SyncPlan {
        actions = List.copyOf(actions);
    }

    public boolean isCompatible() {
        return actions.isEmpty();
    }

    public boolean hasBlockedActions() {
        return actions.stream().anyMatch(action -> action.type() == PlanAction.Type.BLOCKED);
    }

    public long count(PlanAction.Type type) {
        return actions.stream().filter(action -> action.type() == type).count();
    }
}
