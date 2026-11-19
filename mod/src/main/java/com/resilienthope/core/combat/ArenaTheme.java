package com.resilienthope.core.combat;

/**
 * Extensible arena scenery theme registry for native 3D turn-based combat.
 * Currently defaults all encounters to the flagship Starship Tactical Hangar Deck,
 * while preserving clean vertical extension hooks for future planets/biomes.
 */
public enum ArenaTheme {
    STARSHIP_HANGAR("starship_hangar", "星舰战术机库");

    public final String id;
    public final String displayName;

    ArenaTheme(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static ArenaTheme resolveFor(String encounterType, boolean iceTheme) {
        // Vertical extension point: return planet-specific ArenaTheme variants here later.
        return STARSHIP_HANGAR;
    }

    public static ArenaTheme fromId(String id) {
        if (id != null) {
            for (ArenaTheme theme : values()) {
                if (theme.id.equals(id)) return theme;
            }
        }
        return STARSHIP_HANGAR;
    }
}
