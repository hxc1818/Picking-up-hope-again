package com.resilienthope.core.team;

import com.resilienthope.core.PickingHopeCore;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TeamCapability {
    public static final Capability<ITeamData> TEAM_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(PickingHopeCore.MOD_ID, "team_data");

    public interface ITeamData {
        CharacterData getCharacter(int slot);
        int getActiveSlotIndex();
        void setActiveSlotIndex(int index);
        void switchSlot(Player player, int newIndex);
        void syncHealthFromPlayer(Player player);
        void syncHealthToPlayer(Player player);
        CompoundTag serializeNBT();
        void deserializeNBT(CompoundTag tag);
    }

    public static class TeamData implements ITeamData {
        private final CharacterData[] slots = new CharacterData[4];
        private int activeSlot = 0;

        public TeamData() {
            slots[0] = new CharacterData("零号特工·破晓", 1000.0f, 100.0f, true);
            slots[1] = new CharacterData("铁卫·待解锁", 1500.0f, 80.0f, false);
            slots[2] = new CharacterData("矩阵·待解锁", 900.0f, 60.0f, false);
            slots[3] = new CharacterData("星枢·待解锁", 1100.0f, 80.0f, false);
        }

        @Override
        public CharacterData getCharacter(int slot) {
            if (slot >= 0 && slot < 4) return slots[slot];
            return slots[0];
        }

        @Override
        public int getActiveSlotIndex() {
            return activeSlot;
        }

        @Override
        public void setActiveSlotIndex(int index) {
            if (index >= 0 && index < 4 && slots[index].isUnlocked()) {
                this.activeSlot = index;
            }
        }

        @Override
        public void switchSlot(Player player, int newIndex) {
            if (newIndex < 0 || newIndex >= 4 || newIndex == activeSlot) return;
            if (!slots[newIndex].isUnlocked()) return;

            syncHealthFromPlayer(player);
            this.activeSlot = newIndex;
            syncHealthToPlayer(player);
        }

        @Override
        public void syncHealthFromPlayer(Player player) {
            if (player == null) return;
            CharacterData current = slots[activeSlot];
            float ratio = Math.max(0.0f, Math.min(1.0f, player.getHealth() / Math.max(1.0f, player.getMaxHealth())));
            current.setCurrentHp(ratio * current.getTotalMaxHp());
        }

        @Override
        public void syncHealthToPlayer(Player player) {
            if (player == null) return;
            CharacterData target = slots[activeSlot];
            float ratio = Math.max(0.05f, Math.min(1.0f, target.getCurrentHp() / Math.max(1.0f, target.getTotalMaxHp())));
            player.setHealth(ratio * player.getMaxHealth());
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag compound = new CompoundTag();
            compound.putInt("ActiveSlot", activeSlot);
            ListTag list = new ListTag();
            for (CharacterData data : slots) {
                list.add(data.serializeNBT());
            }
            compound.put("Slots", list);
            return compound;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            this.activeSlot = tag.getInt("ActiveSlot");
            ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(4, list.size()); i++) {
                slots[i].deserializeNBT(list.getCompound(i));
            }
            // Guarantee slot 0 identity and unlocked status even on migrated saves
            slots[0].setName("零号特工·破晓");
            slots[0].setUnlocked(true);
            slots[0].setMaxEnergy(100.0f);
            if (!slots[activeSlot].isUnlocked()) {
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
        public CompoundTag serializeNBT() {
            return backend.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            backend.deserializeNBT(nbt);
        }
    }

    public static void register() {
        // Capability token automatically registered in 1.18.2
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(IDENTIFIER, new Provider());
        }
    }
}
