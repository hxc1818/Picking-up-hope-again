package com.resilienthope.core.combat;

import com.resilienthope.core.team.CharacterData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared pure preview calculator predicting target highlights and expected toughness reduction.
 * Strictly mirrors TurnBasedCombatManager toughness formulas; intentionally does NOT predict HP damage.
 */
public final class BattleCombatPreview {

    public enum TargetHighlight {
        NONE,
        PRIMARY_ENEMY,
        SECONDARY_ENEMY,
        ALLY_TARGET
    }

    public static final class PreviewResult {
        public final Map<Integer, Float> expectedToughnessDrop = new HashMap<>();
        public final Map<Integer, Boolean> willBreak = new HashMap<>();
        public final Map<Integer, TargetHighlight> enemyHighlights = new HashMap<>();
        public final Set<Integer> allyHighlights = new HashSet<>();
        public boolean targetsAlliesOnly = false;
    }

    private BattleCombatPreview() {}

    public static PreviewResult computePreview(BattleState st, BattleState.AllyUnit actor,
                                               int actionType, int selectedEnemyIdx) {
        PreviewResult res = new PreviewResult();
        if (st == null || actor == null || !actor.isAlive() || st.finished) {
            return res;
        }

        int mainIdx = resolveLivingEnemyIndex(st, selectedEnemyIdx);
        String charId = actor.charId != null ? actor.charId : "";
        String role = actor.role != null ? actor.role : CharacterData.ROLE_BALANCED;
        String elem = actor.element != null ? actor.element : CharacterData.ELEM_EARTH;

        if (actionType == TurnBasedCombatManager.ACTION_BASIC) {
            if (mainIdx >= 0) {
                BattleState.EnemyUnit target = st.enemies.get(mainIdx);
                res.enemyHighlights.put(mainIdx, TargetHighlight.PRIMARY_ENEMY);

                float toughCost = "suqing".equals(charId) ? 35.0f : 30.0f;
                boolean tutGuaranteeBreak = st.tutorialId == 2 && st.tutorialStep == 2
                        && !target.broken && target.toughness <= 35f;
                if (tutGuaranteeBreak) {
                    toughCost = Math.max(toughCost, target.toughness + 5f);
                    applyToughnessPreviewForceWeakness(res, target, mainIdx, toughCost);
                } else {
                    applyToughnessPreview(res, target, mainIdx, elem, toughCost);
                }
            }
            if ("suqing".equals(charId)) {
                int lowestIdx = findLowestHpLivingAlly(st);
                if (lowestIdx >= 0) {
                    res.allyHighlights.add(lowestIdx);
                }
            }
            return res;
        }

        if (actionType == TurnBasedCombatManager.ACTION_SKILL) {
            switch (role) {
                case CharacterData.ROLE_HEALER, CharacterData.ROLE_SHIELDER, CharacterData.ROLE_BUFFER -> {
                    res.targetsAlliesOnly = true;
                    addAllLivingAllies(st, res);
                }
                case CharacterData.ROLE_SINGLE -> {
                    if (mainIdx >= 0) {
                        res.enemyHighlights.put(mainIdx, TargetHighlight.PRIMARY_ENEMY);
                        applyToughnessPreview(res, st.enemies.get(mainIdx), mainIdx, elem, 75.0f);
                    }
                }
                case CharacterData.ROLE_AOE -> {
                    for (int i = 0; i < st.enemies.size(); i++) {
                        BattleState.EnemyUnit e = st.enemies.get(i);
                        if (!e.isAlive()) continue;
                        res.enemyHighlights.put(i, i == mainIdx ? TargetHighlight.PRIMARY_ENEMY : TargetHighlight.SECONDARY_ENEMY);
                        applyToughnessPreview(res, e, i, elem, 45.0f);
                    }
                }
                default -> { // BALANCED (Lin Ye: 60 main + 30 adjacent blast)
                    if (mainIdx >= 0) {
                        res.enemyHighlights.put(mainIdx, TargetHighlight.PRIMARY_ENEMY);
                        applyToughnessPreview(res, st.enemies.get(mainIdx), mainIdx, elem, 60.0f);
                        if (mainIdx - 1 >= 0 && st.enemies.get(mainIdx - 1).isAlive()) {
                            res.enemyHighlights.put(mainIdx - 1, TargetHighlight.SECONDARY_ENEMY);
                            applyToughnessPreview(res, st.enemies.get(mainIdx - 1), mainIdx - 1, elem, 30.0f);
                        }
                        if (mainIdx + 1 < st.enemies.size() && st.enemies.get(mainIdx + 1).isAlive()) {
                            res.enemyHighlights.put(mainIdx + 1, TargetHighlight.SECONDARY_ENEMY);
                            applyToughnessPreview(res, st.enemies.get(mainIdx + 1), mainIdx + 1, elem, 30.0f);
                        }
                    }
                }
            }
            return res;
        }

        if (actionType == TurnBasedCombatManager.ACTION_ULT) {
            switch (role) {
                case CharacterData.ROLE_HEALER, CharacterData.ROLE_BUFFER -> {
                    res.targetsAlliesOnly = true;
                    addAllLivingAllies(st, res);
                }
                case CharacterData.ROLE_SHIELDER -> {
                    addAllLivingAllies(st, res);
                    for (int i = 0; i < st.enemies.size(); i++) {
                        BattleState.EnemyUnit e = st.enemies.get(i);
                        if (!e.isAlive()) continue;
                        res.enemyHighlights.put(i, i == mainIdx ? TargetHighlight.PRIMARY_ENEMY : TargetHighlight.SECONDARY_ENEMY);
                        applyToughnessPreview(res, e, i, elem, 50.0f);
                    }
                }
                case CharacterData.ROLE_SINGLE -> {
                    if (mainIdx >= 0) {
                        res.enemyHighlights.put(mainIdx, TargetHighlight.PRIMARY_ENEMY);
                        applyToughnessPreview(res, st.enemies.get(mainIdx), mainIdx, elem, 110.0f);
                    }
                }
                case CharacterData.ROLE_AOE -> {
                    for (int i = 0; i < st.enemies.size(); i++) {
                        BattleState.EnemyUnit e = st.enemies.get(i);
                        if (!e.isAlive()) continue;
                        res.enemyHighlights.put(i, i == mainIdx ? TargetHighlight.PRIMARY_ENEMY : TargetHighlight.SECONDARY_ENEMY);
                        applyToughnessPreview(res, e, i, elem, 70.0f);
                    }
                }
                default -> { // BALANCED (Lin Ye: 90 main + 55 adjacent + optional 20 full-field aftershock)
                    if (mainIdx >= 0) {
                        BattleState.EnemyUnit main = st.enemies.get(mainIdx);
                        boolean bonusAftershock = main.broken || main.debuffDefTurns > 0;
                        res.enemyHighlights.put(mainIdx, TargetHighlight.PRIMARY_ENEMY);
                        applyToughnessPreview(res, main, mainIdx, elem, 90.0f);
                        if (mainIdx - 1 >= 0 && st.enemies.get(mainIdx - 1).isAlive()) {
                            res.enemyHighlights.put(mainIdx - 1, TargetHighlight.SECONDARY_ENEMY);
                            applyToughnessPreview(res, st.enemies.get(mainIdx - 1), mainIdx - 1, elem, 55.0f);
                        }
                        if (mainIdx + 1 < st.enemies.size() && st.enemies.get(mainIdx + 1).isAlive()) {
                            res.enemyHighlights.put(mainIdx + 1, TargetHighlight.SECONDARY_ENEMY);
                            applyToughnessPreview(res, st.enemies.get(mainIdx + 1), mainIdx + 1, elem, 55.0f);
                        }
                        if (bonusAftershock) {
                            for (int i = 0; i < st.enemies.size(); i++) {
                                BattleState.EnemyUnit e = st.enemies.get(i);
                                if (!e.isAlive()) continue;
                                res.enemyHighlights.putIfAbsent(i, TargetHighlight.SECONDARY_ENEMY);
                                applyToughnessPreview(res, e, i, elem, 20.0f);
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private static int resolveLivingEnemyIndex(BattleState st, int preferredIdx) {
        if (preferredIdx >= 0 && preferredIdx < st.enemies.size() && st.enemies.get(preferredIdx).isAlive()) {
            return preferredIdx;
        }
        for (int i = 0; i < st.enemies.size(); i++) {
            if (st.enemies.get(i).isAlive()) return i;
        }
        return -1;
    }

    private static int findLowestHpLivingAlly(BattleState st) {
        int best = -1;
        double bestRatio = Double.MAX_VALUE;
        for (int i = 0; i < st.allies.size(); i++) {
            BattleState.AllyUnit a = st.allies.get(i);
            if (!a.isAlive()) continue;
            double r = a.hp / Math.max(1.0f, a.maxHp);
            if (r < bestRatio) {
                bestRatio = r;
                best = i;
            }
        }
        return best;
    }

    private static void addAllLivingAllies(BattleState st, PreviewResult res) {
        for (int i = 0; i < st.allies.size(); i++) {
            if (st.allies.get(i).isAlive()) {
                res.allyHighlights.add(i);
            }
        }
    }

    private static void applyToughnessPreview(PreviewResult res, BattleState.EnemyUnit e, int idx, String elem, float rawCost) {
        if (!e.isAlive() || e.broken || !e.hasWeakness(elem)) return;
        applyToughnessPreviewForceWeakness(res, e, idx, rawCost);
    }

    private static void applyToughnessPreviewForceWeakness(PreviewResult res, BattleState.EnemyUnit e, int idx, float rawCost) {
        if (!e.isAlive() || e.broken || e.toughness <= 0f) return;
        float existing = res.expectedToughnessDrop.getOrDefault(idx, 0f);
        float drop = Math.min(e.toughness, existing + Math.max(0f, rawCost));
        res.expectedToughnessDrop.put(idx, drop);
        res.willBreak.put(idx, drop > 0f && (e.toughness - drop) <= 0.01f);
    }
}
