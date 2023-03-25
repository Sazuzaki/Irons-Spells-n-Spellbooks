package io.redspace.ironsspellbooks.spells.fire;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerMagicData;
import io.redspace.ironsspellbooks.player.ClientRenderCache;
import io.redspace.ironsspellbooks.spells.AbstractSpell;
import io.redspace.ironsspellbooks.spells.SpellType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;


public class BurningDashSpell extends AbstractSpell {
    //package net.minecraft.client.renderer.entity.layers;

    public BurningDashSpell() {
        this(1);
    }

    public BurningDashSpell(int level) {
        super(SpellType.BURNING_DASH_SPELL);
        this.level = level;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public void onClientPreCast(Level level, LivingEntity entity, InteractionHand hand, PlayerMagicData playerMagicData) {
        ClientRenderCache.lastSpinAttack = ClientRenderCache.SpinAttackType.FIRE;
        super.onClientPreCast(level, entity, hand, playerMagicData);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onCast(Level world, LivingEntity entity, PlayerMagicData playerMagicData) {
        IronsSpellbooks.LOGGER.debug("BurningDashSpell.onCast: Side <---");
        entity.knockback((double) (5 * 0.5F), (double) Mth.sin(45 * ((float) Math.PI / 180F)), (double) (-Mth.cos(45 * ((float) Math.PI / 180F))));

        float amplifier = 0.5f + (level - 1) * .25f;
        Vec3 vec = entity.getLookAngle().normalize().scale(amplifier);
//        if (entity.isOnGround())
//            entity.move(MoverType.SELF, new Vec3(0.0D, 0.75d, 0.0D));
//
//        if (entity instanceof ServerPlayer player) {
//            player.startAutoSpinAttack(10 + level);
//            //Messages.sendToPlayer(new ClientboundAddMotionToPlayer(vec, true), (ServerPlayer) entity);
//        }

        super.onCast(world, entity, playerMagicData);

        /*
        //in degrees
        float rx = player.getYRot();
        float ry = player.getXRot();

        float vecX = -Mth.sin(rx * ((float) Math.PI / 180F)) * Mth.cos(ry * ((float) Math.PI / 180F));
        float vecY = -Mth.sin(ry * ((float) Math.PI / 180F));
        float vecZ = Mth.cos(rx * ((float) Math.PI / 180F)) * Mth.cos(ry * ((float) Math.PI / 180F));
        float magnitude = Mth.sqrt(vecX * vecX + vecY * vecY + vecZ * vecZ);
        float amplifier = 1.2f * (1 + ((spellLevel - 1) * .25f));

        vecX *= amplifier / magnitude;
        vecY *= amplifier / magnitude;
        vecZ *= amplifier / magnitude;

        player.startAutoSpinAttack(10);
        player.push(vecX, vecY * .5d, vecZ);

        SoundEvent soundevent;
        if (spellLevel >= 3) {
            soundevent = SoundEvents.TRIDENT_RIPTIDE_3;
        } else if (spellLevel == 2) {
            soundevent = SoundEvents.TRIDENT_RIPTIDE_2;
        } else {
            soundevent = SoundEvents.TRIDENT_RIPTIDE_1;
        }
        world.playSound((Player) null, player, soundevent, SoundSource.PLAYERS, 1.0F, 1.0F);*/

    }
}
