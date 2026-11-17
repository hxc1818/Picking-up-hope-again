package com.resilienthope.core.client;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side manager for floating 3D-projected combat damage numbers (Genshin/ARPG style).
 */
public class DamagePopupManager {
    public static class Popup {
        public final double worldX;
        public final double worldY;
        public final double worldZ;
        public final int amount;
        public final boolean isCrit;
        public final boolean isSkill;
        public int ageTicks = 0;
        public final int maxTicks = 26;

        public Popup(double x, double y, double z, int amount, boolean isCrit, boolean isSkill) {
            this.worldX = x;
            this.worldY = y;
            this.worldZ = z;
            this.amount = amount;
            this.isCrit = isCrit;
            this.isSkill = isSkill;
        }
    }

    private static final CopyOnWriteArrayList<Popup> POPUPS = new CopyOnWriteArrayList<>();

    public static void addPopup(double x, double y, double z, int amount, boolean isCrit, boolean isSkill) {
        POPUPS.add(new Popup(x, y, z, amount, isCrit, isSkill));
    }

    public static void clientTick() {
        Iterator<Popup> it = POPUPS.iterator();
        while (it.hasNext()) {
            Popup p = it.next();
            p.ageTicks++;
            if (p.ageTicks >= p.maxTicks) {
                POPUPS.remove(p);
            }
        }
    }

    public static CopyOnWriteArrayList<Popup> getPopups() {
        return POPUPS;
    }
}
