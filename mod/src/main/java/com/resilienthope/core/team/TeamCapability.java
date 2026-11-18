package com.resilienthope.core.team;

import com.resilienthope.core.PickingHopeCore;
import com.resilienthope.core.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TeamCapability {
    public static final Capability<ITeamData> TEAM_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(PickingHopeCore.MOD_ID, "team_data");

    public static final int ROSTER_SIZE = 6;

    public interface ITeamData {
        CharacterData getCharacter(int partySlot);
        CharacterData getRosterCharacter(int rosterIdx);
        CharacterData getCharacterById(String charId);
        int getPartyRosterIndex(int partySlot);
        void setPartyRosterIndex(int partySlot, int rosterIdx);
        List<CharacterData> getActivePartyMembers();
        int getActiveSlotIndex();
        void setActiveSlotIndex(int index);
        void switchSlot(Player player, int newIndex);
        void syncHealthFromPlayer(Player player);
        void syncHealthToPlayer(Player player);

        int getPlayerLevel();
        void setPlayerLevel(int lv);
        int getPlayerExp();
        void addPlayerExp(int exp);
        int getPlayerExpNeeded();
        int getWorldLevel();
        void setWorldLevel(int wl);
        int getPlayerLevelCapForWorldLevel();
        boolean canBreakthroughWorldLevel();
        int getMaxAscensionAllowed();
        int getStamina();
        void setStamina(int stamina);
        int getPityCounter();
        void setPityCounter(int pity);

        void unlockRosterCharacter(String charId, boolean autoAddToParty);
        void resetToFreshV2();

        CompoundTag serializeNBT();
        void deserializeNBT(CompoundTag tag);
    }

    public static class TeamData implements ITeamData {
        private final CharacterData[] roster = new CharacterData[ROSTER_SIZE];
        private final int[] partyIndices = {0, 1, 2, 3};
        private int activeSlot = 0;

        private int playerLevel = 1;
        private int playerExp = 0;
        private int worldLevel = 0;
        private int stamina = 180;
        private int pityCounter = 0;

        public TeamData() {
            initFreshRoster();
        }

        private void initFreshRoster() {
            roster[0] = new CharacterData("linye", "检修员·林野", CharacterData.ELEM_EARTH, CharacterData.ROLE_BALANCED,
                    5, 780f, 92f, 75f, 104, 100f, true, new ItemStack(ModItems.PLASMA_CUTTER.get()));
            roster[1] = new CharacterData("chenfeng", "巡检员·陈风", CharacterData.ELEM_AIR, CharacterData.ROLE_AOE,
                    4, 680f, 88f, 65f, 112, 100f, false, new ItemStack(ModItems.AERO_BLADE.get()));
            roster[2] = new CharacterData("suqing", "医疗员·苏晴", CharacterData.ELEM_WATER, CharacterData.ROLE_HEALER,
                    4, 840f, 68f, 78f, 102, 100f, false, new ItemStack(ModItems.HYDRO_PULSE_GUN.get()));
            roster[3] = new CharacterData("laozheng", "防护员·老郑", CharacterData.ELEM_FIRE, CharacterData.ROLE_SHIELDER,
                    4, 960f, 72f, 95f, 96, 100f, false, new ItemStack(ModItems.CRYO_SAWBLADE.get()));
            roster[4] = new CharacterData("hanbing", "狙击手·韩冰", CharacterData.ELEM_FIRE, CharacterData.ROLE_SINGLE,
                    5, 700f, 106f, 62f, 108, 100f, false, new ItemStack(ModItems.CRYO_SAWBLADE.get()));
            roster[5] = new CharacterData("zhoumin", "调度员·周敏", CharacterData.ELEM_WATER, CharacterData.ROLE_BUFFER,
                    5, 760f, 78f, 72f, 116, 100f, false, new ItemStack(ModItems.HYDRO_PULSE_GUN.get()));
        }

        @Override
        public void resetToFreshV2() {
            initFreshRoster();
            partyIndices[0] = 0;
            partyIndices[1] = 1;
            partyIndices[2] = 2;
            partyIndices[3] = 3;
            this.activeSlot = 0;
            this.playerLevel = 1;
            this.playerExp = 0;
            this.worldLevel = 0;
            this.stamina = 180;
            this.pityCounter = 0;
        }

        @Override
        public CharacterData getCharacter(int partySlot) {
            int rIdx = (partySlot >= 0 && partySlot < 4) ? partyIndices[partySlot] : 0;
            if (rIdx < 0 || rIdx >= ROSTER_SIZE) rIdx = 0;
            return roster[rIdx];
        }

        @Override
        public CharacterData getRosterCharacter(int rosterIdx) {
            if (rosterIdx >= 0 && rosterIdx < ROSTER_SIZE) return roster[rosterIdx];
            return roster[0];
        }

        @Override
        public CharacterData getCharacterById(String charId) {
            for (CharacterData c : roster) {
                if (c.getCharId().equals(charId)) return c;
            }
            return null;
        }

        @Override
        public int getPartyRosterIndex(int partySlot) {
            return (partySlot >= 0 && partySlot < 4) ? partyIndices[partySlot] : 0;
        }

        @Override
        public void setPartyRosterIndex(int partySlot, int rosterIdx) {
            if (partySlot < 0 || partySlot >= 4 || rosterIdx < 0 || rosterIdx >= ROSTER_SIZE) return;
            if (!roster[rosterIdx].isUnlocked()) return;
            for (int i = 0; i < 4; i++) {
                if (i != partySlot && partyIndices[i] == rosterIdx) {
                    partyIndices[i] = partyIndices[partySlot];
                    break;
                }
            }
            partyIndices[partySlot] = rosterIdx;
        }

        @Override
        public List<CharacterData> getActivePartyMembers() {
            List<CharacterData> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                CharacterData c = getCharacter(i);
                if (c != null && c.isUnlocked() && !list.contains(c)) {
                    list.add(c);
                }
            }
            if (list.isEmpty()) {
                roster[0].setUnlocked(true);
                list.add(roster[0]);
            }
            return list;
        }

        @Override
        public void unlockRosterCharacter(String charId, boolean autoAddToParty) {
            for (int r = 0; r < ROSTER_SIZE; r++) {
                if (roster[r].getCharId().equals(charId)) {
                    if (!roster[r].isUnlocked()) {
                        roster[r].setUnlocked(true);
                        roster[r].setCurrentHp(roster[r].getTotalMaxHp());
                        if (autoAddToParty) {
                            // Ensure placed into first locked/empty party slot
                            boolean alreadyIn = false;
                            for (int p = 0; p < 4; p++) {
                                if (partyIndices[p] == r) alreadyIn = true;
                            }
                            if (!alreadyIn) {
                                for (int p = 0; p < 4; p++) {
                                    if (!roster[partyIndices[p]].isUnlocked()) {
                                        partyIndices[p] = r;
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        roster[r].setEidolon(roster[r].getEidolon() + 1);
                    }
                    break;
                }
            }
        }

        @Override
        public int getActiveSlotIndex() { return activeSlot; }

        @Override
        public void setActiveSlotIndex(int index) {
            if (index >= 0 && index < 4 && getCharacter(index).isUnlocked()) {
                this.activeSlot = index;
            }
        }

        @Override
        public void switchSlot(Player player, int newIndex) {
            if (newIndex < 0 || newIndex >= 4 || newIndex == activeSlot) return;
            if (!getCharacter(newIndex).isUnlocked()) return;
            syncHealthFromPlayer(player);
            this.activeSlot = newIndex;
            syncHealthToPlayer(player);
        }

        @Override
        public void syncHealthFromPlayer(Player player) {
            if (player == null) return;
            CharacterData current = getCharacter(activeSlot);
            float ratio = Math.max(0.0f, Math.min(1.0f, player.getHealth() / Math.max(1.0f, player.getMaxHealth())));
            current.setCurrentHp(ratio * current.getTotalMaxHp());
        }

        @Override
        public void syncHealthToPlayer(Player player) {
            if (player == null) return;
            CharacterData target = getCharacter(activeSlot);
            float ratio = Math.max(0.05f, Math.min(1.0f, target.getCurrentHp() / Math.max(1.0f, target.getTotalMaxHp())));
            player.setHealth(ratio * player.getMaxHealth());
        }

        @Override public int getPlayerLevel() { return playerLevel; }
        @Override public void setPlayerLevel(int lv) { this.playerLevel = Math.max(1, Math.min(65, lv)); }
        @Override public int getPlayerExp() { return playerExp; }
        @Override public int getPlayerExpNeeded() { return 100 + (playerLevel - 1) * 40; }

        @Override
        public void addPlayerExp(int exp) {
            if (exp <= 0) return;
            this.playerExp += exp;
            int cap = getPlayerLevelCapForWorldLevel();
            while (this.playerLevel < cap && this.playerExp >= getPlayerExpNeeded()) {
                this.playerExp -= getPlayerExpNeeded();
                this.playerLevel++;
            }
            if (this.playerLevel >= cap) {
                this.playerExp = Math.min(this.playerExp, getPlayerExpNeeded());
            }
        }

        @Override public int getWorldLevel() { return worldLevel; }
        @Override public void setWorldLevel(int wl) { this.worldLevel = Math.max(0, Math.min(6, wl)); }

        @Override
        public int getPlayerLevelCapForWorldLevel() {
            return switch (worldLevel) {
                case 0 -> 20;
                case 1 -> 30;
                case 2 -> 40;
                case 3 -> 50;
                case 4 -> 60;
                default -> 65;
            };
        }

        @Override
        public boolean canBreakthroughWorldLevel() {
            return worldLevel < 6 && playerLevel >= getPlayerLevelCapForWorldLevel();
        }

        @Override
        public int getMaxAscensionAllowed() {
            return worldLevel;
        }

        @Override public int getStamina() { return stamina; }
        @Override public void setStamina(int stamina) { this.stamina = Math.max(0, Math.min(180, stamina)); }
        @Override public int getPityCounter() { return pityCounter; }
        @Override public void setPityCounter(int pity) { this.pityCounter = Math.max(0, pity); }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag compound = new CompoundTag();
            compound.putInt("ActiveSlot", activeSlot);
            compound.putInt("PlayerLevel", playerLevel);
            compound.putInt("PlayerExp", playerExp);
            compound.putInt("WorldLevel", worldLevel);
            compound.putInt("Stamina", stamina);
            compound.putInt("PityCounter", pityCounter);
            compound.putIntArray("PartyIndices", partyIndices);

            ListTag list = new ListTag();
            for (CharacterData data : roster) {
                list.add(data.serializeNBT());
            }
            compound.put("Roster", list);
            return compound;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            this.activeSlot = tag.getInt("ActiveSlot");
            if (tag.contains("PlayerLevel")) this.playerLevel = Math.max(1, Math.min(65, tag.getInt("PlayerLevel")));
            if (tag.contains("PlayerExp")) this.playerExp = Math.max(0, tag.getInt("PlayerExp"));
            if (tag.contains("WorldLevel")) this.worldLevel = Math.max(0, Math.min(6, tag.getInt("WorldLevel")));
            if (tag.contains("Stamina")) this.stamina = Math.max(0, Math.min(180, tag.getInt("Stamina")));
            if (tag.contains("PityCounter")) this.pityCounter = Math.max(0, tag.getInt("PityCounter"));
            if (tag.contains("PartyIndices")) {
                int[] arr = tag.getIntArray("PartyIndices");
                for (int i = 0; i < Math.min(4, arr.length); i++) {
                    if (arr[i] >= 0 && arr[i] < ROSTER_SIZE) partyIndices[i] = arr[i];
                }
            }
            if (tag.contains("Roster", Tag.TAG_LIST)) {
                ListTag list = tag.getList("Roster", Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(ROSTER_SIZE, list.size()); i++) {
                    roster[i].deserializeNBT(list.getCompound(i));
                }
            }
            roster[0].setName("检修员·林野");
            roster[0].setElement(CharacterData.ELEM_EARTH);
            roster[0].setRole(CharacterData.ROLE_BALANCED);
            roster[0].setUnlocked(true);
            if (!getCharacter(activeSlot).isUnlocked()) {
                this.activeSlot = 0;
            }
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final ITeamData backend = new TeamData();
        private final LazyOptional<ITeamData> optional = LazyOptional.of(() -> backend);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == TEAM_CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() { return backend.serializeNBT(); }

        @Override
        public void deserializeNBT(CompoundTag nbt) { backend.deserializeNBT(nbt); }
    }

    public static void register() {}

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(IDENTIFIER, new Provider());
        }
    }
}
