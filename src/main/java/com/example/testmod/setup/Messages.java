package com.example.testmod.setup;

import com.example.testmod.TestMod;
import com.example.testmod.gui.inscription_table.network.ServerboundInscribeSpell;
import com.example.testmod.gui.inscription_table.network.ServerboundInscriptionTableSelectSpell;
import com.example.testmod.gui.overlays.network.ServerboundSetSpellBookActiveIndex;
import com.example.testmod.gui.scroll_forge.network.ServerboundScrollForgeSelectSpell;
import com.example.testmod.network.*;
import com.example.testmod.network.spell.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Messages {

    private static SimpleChannel INSTANCE;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(TestMod.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ClientboundUpdateCastingState.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundUpdateCastingState::new)
                .encoder(ClientboundUpdateCastingState::toBytes)
                .consumer(ClientboundUpdateCastingState::handle)
                .add();

        net.messageBuilder(ClientboundAddMotionToPlayer.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundAddMotionToPlayer::new)
                .encoder(ClientboundAddMotionToPlayer::toBytes)
                .consumer(ClientboundAddMotionToPlayer::handle)
                .add();

        net.messageBuilder(ClientboundSyncMana.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundSyncMana::new)
                .encoder(ClientboundSyncMana::toBytes)
                .consumer(ClientboundSyncMana::handle)
                .add();

        net.messageBuilder(ClientboundOnClientCast.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundOnClientCast::new)
                .encoder(ClientboundOnClientCast::toBytes)
                .consumer(ClientboundOnClientCast::handle)
                .add();

        net.messageBuilder(ClientboundSyncPlayerData.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundSyncPlayerData::new)
                .encoder(ClientboundSyncPlayerData::toBytes)
                .consumer(ClientboundSyncPlayerData::handle)
                .add();

        net.messageBuilder(ServerboundInscribeSpell.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundInscribeSpell::new)
                .encoder(ServerboundInscribeSpell::toBytes)
                .consumer(ServerboundInscribeSpell::handle)
                .add();

        net.messageBuilder(ClientboundSyncCooldown.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundSyncCooldown::new)
                .encoder(ClientboundSyncCooldown::toBytes)
                .consumer(ClientboundSyncCooldown::handle)
                .add();

        net.messageBuilder(ClientboundSyncCooldowns.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundSyncCooldowns::new)
                .encoder(ClientboundSyncCooldowns::toBytes)
                .consumer(ClientboundSyncCooldowns::handle)
                .add();

        net.messageBuilder(ClientboundTeleportParticles.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundTeleportParticles::new)
                .encoder(ClientboundTeleportParticles::toBytes)
                .consumer(ClientboundTeleportParticles::handle)
                .add();

        net.messageBuilder(ServerboundSetSpellBookActiveIndex.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundSetSpellBookActiveIndex::new)
                .encoder(ServerboundSetSpellBookActiveIndex::toBytes)
                .consumer(ServerboundSetSpellBookActiveIndex::handle)
                .add();

        net.messageBuilder(ServerboundScrollForgeSelectSpell.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundScrollForgeSelectSpell::new)
                .encoder(ServerboundScrollForgeSelectSpell::toBytes)
                .consumer(ServerboundScrollForgeSelectSpell::handle)
                .add();

        net.messageBuilder(ServerboundInscriptionTableSelectSpell.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundInscriptionTableSelectSpell::new)
                .encoder(ServerboundInscriptionTableSelectSpell::toBytes)
                .consumer(ServerboundInscriptionTableSelectSpell::handle)
                .add();

        net.messageBuilder(ServerboundCancelCast.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundCancelCast::new)
                .encoder(ServerboundCancelCast::toBytes)
                .consumer(ServerboundCancelCast::handle)
                .add();

        net.messageBuilder(ClientboundSuppressRightClicks.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundSuppressRightClicks::new)
                .encoder(ClientboundSuppressRightClicks::toBytes)
                .consumer(ClientboundSuppressRightClicks::handle)
                .add();

        net.messageBuilder(ClientboundHealParticles.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundHealParticles::new)
                .encoder(ClientboundHealParticles::toBytes)
                .consumer(ClientboundHealParticles::handle)
                .add();

        net.messageBuilder(ClientboundBloodSiphonParticles.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundBloodSiphonParticles::new)
                .encoder(ClientboundBloodSiphonParticles::toBytes)
                .consumer(ClientboundBloodSiphonParticles::handle)
                .add();

        net.messageBuilder(ClientboundRegenCloudParticles.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundRegenCloudParticles::new)
                .encoder(ClientboundRegenCloudParticles::toBytes)
                .consumer(ClientboundRegenCloudParticles::handle)
                .add();

        net.messageBuilder(ClientboundOnCastStarted.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClientboundOnCastStarted::new)
                .encoder(ClientboundOnCastStarted::toBytes)
                .consumer(ClientboundOnCastStarted::handle)
                .add();

    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToPlayersTrackingEntity(MSG message, Entity entity) {
        sendToPlayersTrackingEntity(message, entity, false);
    }

    public static <MSG> void sendToPlayersTrackingEntity(MSG message, Entity entity, boolean sendToSource) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
        if (sendToSource && entity instanceof ServerPlayer serverPlayer)
            sendToPlayer(message, serverPlayer);
    }
}