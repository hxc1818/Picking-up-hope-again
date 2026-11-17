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
        CompoundTag serializeNBT();
        void deserializeNBT(CompoundTag tag);
    }

    public static class TeamData implements ITeamData {
        private final CharacterData[] slots = new CharacterData[4];
        private int activeSlot = 0;

        public TeamData() {
            slots[0] = new CharacterData("外勤特工", 1000.0f, 60.0f);
            slots[1] = new CharacterData("工会重装兵", 1600.0f, 80.0f);
            slots[2] = new CharacterData("深空科研员", 850.0f, 40.0f);
            slots[3] = new CharacterData("先驱仿生体", 1100.0f, 70.0f);
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
            if (index >= 0 && index < 4) {
                this.activeSlot = index;
            }
        }

        @Override
        public void switchSlot(Player player, int newIndex) {
            if (newIndex < 0 || newIndex >= 4 || newIndex == activeSlot) return;

            // Sync current player health to current active character
            CharacterData current = slots[activeSlot];
            current.setCurrentHp(player.getHealth() * 50.0f); // 20 hearts -> 1000 hp scale

            // Switch to new character
            this.activeSlot = newIndex;
            CharacterData target = slots[newIndex];
            float mappedPlayerHp = Math.max(1.0f, target.getCurrentHp() / 50.0f);
            player.setHealth(mappedPlayerHp);
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
