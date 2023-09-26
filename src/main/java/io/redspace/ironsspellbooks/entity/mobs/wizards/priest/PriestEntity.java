package io.redspace.ironsspellbooks.entity.mobs.wizards.priest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.mobs.SupportMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class PriestEntity extends NeutralWizard implements VillagerDataHolder, SupportMob, HomeOwner, Merchant {
    private static final EntityDataAccessor<VillagerData> DATA_VILLAGER_DATA = SynchedEntityData.defineId(PriestEntity.class, EntityDataSerializers.VILLAGER_DATA);
    private static final EntityDataAccessor<Boolean> DATA_VILLAGER_UNHAPPY = SynchedEntityData.defineId(PriestEntity.class, EntityDataSerializers.BOOLEAN);
    public GoalSelector supportTargetSelector;
    private int unhappyTimer;

    public PriestEntity(EntityType<? extends AbstractSpellCastingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        xpReward = 15;

    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(1, new GustDefenseGoal(this));
        this.goalSelector.addGoal(2, new WizardSupportGoal<>(this, 1.5f, 100, 180)
                .setSpells(
                        List.of(SpellRegistry.BLESSING_OF_LIFE_SPELL.get(), SpellRegistry.BLESSING_OF_LIFE_SPELL.get(), SpellRegistry.HEALING_CIRCLE_SPELL.get()),
                        List.of(SpellRegistry.FORTIFY_SPELL.get())
                ));
        this.goalSelector.addGoal(3, new WizardAttackGoal(this, 1.5f, 35, 70)
                .setSpells(
                        List.of(SpellRegistry.WISP_SPELL.get(), SpellRegistry.GUIDING_BOLT_SPELL.get(), SpellRegistry.GUIDING_BOLT_SPELL.get()),
                        List.of(SpellRegistry.GUST_SPELL.get()),
                        List.of(),
                        List.of(SpellRegistry.HEAL_SPELL.get()))
                .setSpellQuality(0.3f, 0.5f)
                .setDrinksPotions());
        this.goalSelector.addGoal(5, new RoamVillageGoal(this, 30, 1f));
        this.goalSelector.addGoal(6, new ReturnToHomeAtNightGoal<>(this, 1f));
        this.goalSelector.addGoal(7, new PatrolNearLocationGoal(this, 30, 1f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new GenericDefendVillageTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, (mob) -> mob instanceof Enemy && !(mob instanceof Creeper)));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));

        this.supportTargetSelector = new GoalSelector(this.level.getProfilerSupplier());
        this.supportTargetSelector.addGoal(0, new FindSupportableTargetGoal<>(this, LivingEntity.class, true,
                (mob) -> !isAngryAt(mob) && mob.getHealth() * 1.25f < mob.getMaxHealth() && (mob.getType().is(ModTags.VILLAGE_ALLIES) || mob instanceof Player))
        );
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        RandomSource randomsource = pLevel.getRandom();
        this.populateDefaultEquipmentSlots(randomsource, pDifficulty);
        this.setHome(this.blockPosition());
        IronsSpellbooks.LOGGER.debug("Priest new home: {}", this.getHome());
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.PRIEST_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.PRIEST_CHESTPLATE.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(AttributeRegistry.CAST_TIME_REDUCTION.get(), 1.5)
                .add(Attributes.MOVEMENT_SPEED, .23);
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new GroundPathNavigation(this, pLevel) {
            @Override
            protected PathFinder createPathFinder(int pMaxVisitedNodes) {
                this.nodeEvaluator = new WalkNodeEvaluator();
                this.nodeEvaluator.setCanPassDoors(true);
                this.nodeEvaluator.setCanOpenDoors(true);
                return new PathFinder(this.nodeEvaluator, pMaxVisitedNodes);
            }
//
//            @Override
//            public NodeEvaluator getNodeEvaluator() {
//                var nodeEvalutator = super.getNodeEvaluator();
//                nodeEvalutator.setCanPassDoors(true);
//                nodeEvalutator.setCanOpenDoors(true);
//                return nodeEvalutator;
//            }
        };
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
        this.entityData.define(DATA_VILLAGER_UNHAPPY, false);
    }

    public void setVillagerData(VillagerData villagerdata) {
        //VillagerData villagerdata = this.getVillagerData();
        villagerdata.setProfession(VillagerProfession.NONE);
        this.entityData.set(DATA_VILLAGER_DATA, villagerdata);
    }

    public boolean isUnhappy() {
        return this.entityData.get(DATA_VILLAGER_UNHAPPY);
    }

    public @NotNull VillagerData getVillagerData() {
        return this.entityData.get(DATA_VILLAGER_DATA);
    }

    LivingEntity supportTarget;

    @org.jetbrains.annotations.Nullable
    @Override
    public LivingEntity getSupportTarget() {
        return supportTarget;
    }

    @Override
    public void setSupportTarget(LivingEntity target) {
        this.supportTarget = target;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        //Vanilla does this seemingly-excessive tick count solution. maybe there's a method to the madness
        if (this.tickCount % 4 == 0 && this.tickCount > 1) {
            this.supportTargetSelector.tick();
        }
        if (this.tickCount % 60 == 0) {
            this.level.getEntities(this, this.getBoundingBox().inflate(this.getAttributeValue(Attributes.FOLLOW_RANGE)), (entity) -> entity instanceof Enemy && !(entity instanceof Creeper)).forEach((enemy) -> {
                if (enemy instanceof Mob mob)
                    if (mob.getTarget() == null)
                        if (TargetingConditions.forCombat().test(mob, this))
                            mob.setTarget(this);
            });
        }
        if (unhappyTimer > 0)
            if (--unhappyTimer == 0)
                this.entityData.set(DATA_VILLAGER_UNHAPPY, false);
//        this.level.getProfiler().push("priestBrain");
//        this.getBrain().tick((ServerLevel) this.level, this);
//        this.level.getProfiler().pop();
//        this.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.CORE));

    }

    @Override
    protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        //TODO: player needs to meet some reputation condition?
        //setUnhappy();
        boolean preventTrade = this.getOffers().isEmpty() || this.getTarget() != null || isAngryAt(pPlayer);
        if (pHand == InteractionHand.MAIN_HAND) {
            if (preventTrade && !this.level.isClientSide) {
                this.setUnhappy();
            }
        }
        if (preventTrade) {
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            if (!this.level.isClientSide && !this.offers.isEmpty()) {
                this.startTrading(pPlayer);
            }

            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        //return super.mobInteract(pPlayer, pHand);
    }

    public void setUnhappy() {
        if (!level.isClientSide) {
            this.playSound(SoundEvents.VILLAGER_NO, this.getSoundVolume(), this.getVoicePitch());
            unhappyTimer = 20;
            this.entityData.set(DATA_VILLAGER_UNHAPPY, true);
        }
    }

    /*
     * Homeowner implementation
     */
    BlockPos homePos;

    @org.jetbrains.annotations.Nullable
    @Override
    public BlockPos getHome() {
        return homePos;
    }

    @Override
    public void setHome(BlockPos homePos) {
        this.homePos = homePos;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        serializeHome(this, pCompound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        deserializeHome(this, pCompound);
    }

    /*
     * Merchant implementation
     */
    @Nullable
    private Player tradingPlayer;
    @Nullable
    protected MerchantOffers offers;

    @Override
    public void setTradingPlayer(@org.jetbrains.annotations.Nullable Player pTradingPlayer) {
        this.tradingPlayer = pTradingPlayer;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    Int2ObjectMap<VillagerTrades.ItemListing[]> trades = new Int2ObjectOpenHashMap<>(ImmutableMap.of(
            1, new VillagerTrades.ItemListing[]{
                    new VillagerTrades.EmeraldForItems(Items.ROTTEN_FLESH, 32, 16, 2),
                    new VillagerTrades.ItemsForEmeralds(Items.REDSTONE, 1, 2, 1)
            },
            2, new VillagerTrades.ItemListing[]{
                    new VillagerTrades.EmeraldForItems(Items.GOLD_INGOT, 3, 12, 10),
                    new VillagerTrades.ItemsForEmeralds(Items.LAPIS_LAZULI, 1, 1, 5)
            },
            3, new VillagerTrades.ItemListing[]{
                    new VillagerTrades.EmeraldForItems(Items.RABBIT_FOOT, 2, 12, 20),
                    new VillagerTrades.ItemsForEmeralds(Blocks.GLOWSTONE, 4, 1, 12, 10)
            },
            4, new VillagerTrades.ItemListing[]{
                    new VillagerTrades.EmeraldForItems(Items.SCUTE, 4, 12, 30),
                    new VillagerTrades.EmeraldForItems(Items.GLASS_BOTTLE, 9, 12, 30),
                    new VillagerTrades.ItemsForEmeralds(Items.ENDER_PEARL, 5, 1, 15)
            },
            5, new VillagerTrades.ItemListing[]{
                    new VillagerTrades.EmeraldForItems(Items.NETHER_WART, 22, 12, 30),
                    new VillagerTrades.ItemsForEmeralds(Items.EXPERIENCE_BOTTLE, 3, 1, 30)
            }
    ));

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.offers.add(new FastTreasureMapTrade(
                    22,
                    ModTags.PRIEST_WAR_MAP,
                    "item.irons_spellbooks.priest_war_map",
                    MapDecoration.Type.RED_X,
                    1,
                    0
            ).getOffer(this, this.random));
            this.offers.add(new MerchantOffer(
                    new ItemStack(ItemRegistry.GREATER_HEALING_POTION.get()),
                    new ItemStack(Items.EMERALD, 18),
                    3,
                    0,
                    0.2F
            ));
            this.offers.add(new MerchantOffer(
                    new ItemStack(Items.EMERALD, 6),
                    PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING),
                    2,
                    0,
                    0.2F
            ));

            //this.addOffersFromItemListings(offers, trades.get(1), 5);
            //this.updateTrades();
        }

        return this.offers;
    }

    protected void addOffersFromItemListings(MerchantOffers pGivenMerchantOffers, VillagerTrades.ItemListing[] pNewTrades, int pMaxNumbers) {
        Set<Integer> set = Sets.newHashSet();
        if (pNewTrades.length > pMaxNumbers) {
            while (set.size() < pMaxNumbers) {
                set.add(this.random.nextInt(pNewTrades.length));
            }
        } else {
            for (int i = 0; i < pNewTrades.length; ++i) {
                set.add(i);
            }
        }

        for (Integer integer : set) {
            VillagerTrades.ItemListing villagertrades$itemlisting = pNewTrades[integer];
            MerchantOffer merchantoffer = villagertrades$itemlisting.getOffer(this, this.random);
            if (merchantoffer != null) {
                pGivenMerchantOffers.add(merchantoffer);
            }
        }

    }

    @Override
    public void overrideOffers(MerchantOffers pOffers) {

    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isTrading();
    }

    @Override
    public void notifyTrade(MerchantOffer pOffer) {
        pOffer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        //this.rewardTradeXp(pOffer);
    }

    @Override
    public void notifyTradeUpdated(ItemStack pStack) {
        if (!this.level.isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            //this.playSound(this.getTradeUpdatedSound(!pStack.isEmpty()), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    private void startTrading(Player pPlayer) {
        this.setTradingPlayer(pPlayer);
        this.lookAt(pPlayer, 360, 360);
        this.openTradingScreen(pPlayer, this.getDisplayName(), this.getVillagerData().getLevel());
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int pXp) {

    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return null;
    }

    @Override
    public boolean isClientSide() {
        return this.level.isClientSide;
    }

    /*
    Brain Testing
     */
/*
    public Brain<PriestEntity> getBrain() {
        return (Brain<PriestEntity>) super.getBrain();
    }

    protected Brain.Provider<PriestEntity> brainProvider() {
        return Brain.provider(
                //Memories
                ImmutableList.of(
                        MemoryModuleType.WALK_TARGET,
                        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                        MemoryModuleType.PATH,
                        //MemoryModuleType.MEETING_POINT,
                        MemoryModuleType.LOOK_TARGET,
                        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES
                ),
                //Sensors
                ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY));
    }
    //ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_BED, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES, SensorType.VILLAGER_BABIES, SensorType.SECONDARY_POIS, SensorType.GOLEM_DETECTED);

    protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
        Brain<PriestEntity> brain = this.brainProvider().makeBrain(pDynamic);
        this.registerBrainGoals(brain);
        return brain;
    }
//
//    public void refreshBrain(ServerLevel pServerLevel) {
//        Brain<PriestEntity> brain = this.getBrain();
//        brain.stopAll(pServerLevel, this);
//        this.brain = brain.copyWithoutBehaviors();
//        this.registerBrainGoals(this.getBrain());
//    }

    private void registerBrainGoals(Brain<PriestEntity> brain) {

        ImmutableList<Pair<Integer, ? extends Behavior<? super PriestEntity>>> core = ImmutableList.of(
                Pair.of(0, new Swim(0.8F)),
                Pair.of(0, new InteractWithDoor()),

                Pair.of(0, new LookAtTargetSink(45, 90)),
                Pair.of(1, new MoveToTargetSink())

//                Pair.of(0, new RunSometimes<>(new SetEntityLookTarget(EntityType.PLAYER, 6.0F), UniformInt.of(30, 60))),
//
//                //Pair.of(0, new SetRaidStatus()),
//                Pair.of(3, new RunOne<>(ImmutableList.of(
//                        Pair.of(new RandomStroll(1.0F), 2),
//                        Pair.of(new SetWalkTargetFromLookTarget(1.0F, 3), 2),
//                        Pair.of(new DoNothing(30, 60), 1)
//                )))
//                Pair.of(10, new AcquirePoi((p_217499_) -> {
//                    return p_217499_.is(PoiTypes.HOME);
//                }, MemoryModuleType.HOME, false, Optional.of((byte) 14))),
//                Pair.of(10, new AcquirePoi((p_217497_) -> {
//                    return p_217497_.is(PoiTypes.MEETING);
//                }, MemoryModuleType.MEETING_POINT, true, Optional.of((byte) 14)))
        );
        ImmutableList<Pair<Integer, ? extends Behavior<? super PriestEntity>>> idle = ImmutableList.of(
                Pair.of(0, new RunSometimes<>(new SetEntityLookTarget(EntityType.PLAYER, 6.0F), UniformInt.of(30, 60))),

                Pair.of(3, new RunOne<>(ImmutableList.of(
                        Pair.of(new RandomStroll(1.0F), 2),
                        Pair.of(new SetWalkTargetFromLookTarget(1.0F, 3), 2),
                        Pair.of(new DoNothing(30, 60), 1)
                )))

        );
        brain.addActivity(Activity.CORE, core);
        brain.addActivity(Activity.IDLE, idle);
//        brain.addActivityWithConditions(Activity.MEET, VillagerGoalPackages.getMeetPackage(villagerprofession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
//        brain.addActivity(Activity.REST, VillagerGoalPackages.getRestPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.IDLE, VillagerGoalPackages.getIdlePackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.PANIC, VillagerGoalPackages.getPanicPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.RAID, VillagerGoalPackages.getRaidPackage(villagerprofession, 0.5F));
//        brain.addActivity(Activity.HIDE, VillagerGoalPackages.getHidePackage(villagerprofession, 0.5F));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
//
//        pBrain.addActivity(Activity.CORE, 0,
//                ImmutableList.of(
//                        new Swim(0.8F),
//                        new AnimalPanic(2.0F),
//                        new LookAtTargetSink(45, 90),
//                        new MoveToTargetSink(),
//                        new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
//                        new CountDownCooldownTicks(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS),
//                        new CountDownCooldownTicks(MemoryModuleType.RAM_COOLDOWN_TICKS)
//                ));
//
//        pBrain.addActivityWithConditions(Activity.IDLE,
//                ImmutableList.of(
//                        Pair.of(0, new RunSometimes<>(new SetEntityLookTarget(EntityType.PLAYER, 6.0F), UniformInt.of(30, 60))),
//                        Pair.of(0, new AnimalMakeLove(EntityType.GOAT, 1.0F)),
//                        Pair.of(1, new FollowTemptation((p_149446_) -> {
//                            return 1.25F;
//                        })),
//                        Pair.of(2, new BabyFollowAdult<>(ADULT_FOLLOW_RANGE, 1.25F)),
//                        Pair.of(3, new RunOne<>(ImmutableList.of(
//                                Pair.of(new RandomStroll(1.0F), 2),
//                                Pair.of(new SetWalkTargetFromLookTarget(1.0F, 3), 2),
//                                Pair.of(new DoNothing(30, 60), 1)
//                        )))
//                ),
//                ImmutableSet.of(
//                        Pair.of(MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_ABSENT),
//                        Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT)));
    }
 */
}
