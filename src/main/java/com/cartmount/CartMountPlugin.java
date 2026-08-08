package com.cartmount;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Station's Cozy Carts",
    description = "Cruise around Gielinor in a customizable cosmetic minecart",
    tags = {"cosmetic", "cart", "minecart", "cozy", "fun"}
)
public class CartMountPlugin extends Plugin
{
    static final int MOUNTED_ANIMATION = AnimationID.HUMAN_MINECART_ANIM;
    private static final int TOGGLE_POOF_SLOT = 0x43415254;
    private static final Set<RuneLiteObject> CART_OBJECTS =
        Collections.newSetFromMap(new IdentityHashMap<>());

    @Inject
    private Client client;
    @Inject
    private CartMountConfig config;
    @Inject
    private CartModelFactory modelFactory;
    @Inject
    private CartToggleOverlay toggleOverlay;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private MouseManager mouseManager;

    private RuneLiteObject mountObject;
    private MountType activeMount;
    private Player mountedPlayer;
    private PlayerPoseSnapshot originalPose;
    private int activePoseSignature = Integer.MIN_VALUE;
    private int activeModelSignature = Integer.MIN_VALUE;
    private boolean lastResetToggle;
    private PlayerComposition hiddenComposition;
    private int[] originalEquipment;
    private int activeVisibilitySignature = Integer.MIN_VALUE;
    private volatile boolean userMounted;

    @Provides
    CartMountConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CartMountConfig.class);
    }

    @Override
    protected void startUp()
    {
        userMounted = true;
        toggleOverlay.bind(this);
        overlayManager.add(toggleOverlay);
        mouseManager.registerMouseListener(toggleOverlay);
        lastResetToggle = config.resetCart();
        resetCartState();
    }

    @Override
    protected void shutDown()
    {
        mouseManager.unregisterMouseListener(toggleOverlay);
        overlayManager.remove(toggleOverlay);
        toggleOverlay.unbind();
        resetCartState();
        userMounted = false;
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        boolean resetToggle = config.resetCart();
        if (resetToggle != lastResetToggle)
        {
            lastResetToggle = resetToggle;
            resetCartState();
        }
        if (!userMounted)
        {
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        Player player = client.getLocalPlayer();
        WorldView worldView = client.getTopLevelWorldView();
        if (player == null || worldView == null)
        {
            return;
        }
        LocalPoint location = player.getLocalLocation();
        if (location == null || location.getWorldView() != worldView.getId())
        {
            return;
        }

        int modelSignature = modelSignature();
        if (activeModelSignature != modelSignature)
        {
            clearMount();
            modelFactory.clear();
            activeModelSignature = modelSignature;
        }

        MountType type = MountType.MINECART;
        mountPlayer(player);
        ensureMount(type, location, worldView.getPlane());
        if (mountObject != null)
        {
            mountObject.setLocation(location, worldView.getPlane());
            mountObject.setZ(mountObject.getZ() + type.grounding + config.groundingOffset());
            mountObject.setOrientation((player.getCurrentOrientation()
                + (type.facingQuarterTurns + config.facingOffset()) * 512) & 2047);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            resetCartState();
        }
    }

    @Subscribe
    public void onWorldViewLoaded(WorldViewLoaded event)
    {
        clearMount();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!CartMountConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }
        if ("resetCart".equals(event.getKey()))
        {
            lastResetToggle = config.resetCart();
            resetCartState();
            return;
        }
        if ("cartScale".equals(event.getKey())
            || "recolourMinecart".equals(event.getKey()) || "minecartColour".equals(event.getKey())
            || "wheelColour".equals(event.getKey()) || "hubColour".equals(event.getKey()))
        {
            clearMount();
            modelFactory.clear();
            activeModelSignature = Integer.MIN_VALUE;
        }
    }

    private void mountPlayer(Player player)
    {
        RiderAnimation idleChoice = choice(config.idleAnimation());
        RiderAnimation walkChoice = choice(config.walkAnimation());
        RiderAnimation runChoice = choice(config.runAnimation());
        int idleAnimation = idleChoice.idleAnimationId;
        int walkAnimation = walkChoice.walkAnimationId;
        int runAnimation = runChoice.runAnimationId;

        if (mountedPlayer != player || originalPose == null)
        {
            restorePlayerPose();
            mountedPlayer = player;
            originalPose = PlayerPoseSnapshot.capture(player);
        }
        applyEquipmentVisibility(player);
        player.setIdlePoseAnimation(idleAnimation);
        player.setIdleRotateLeft(idleAnimation);
        player.setIdleRotateRight(idleAnimation);
        player.setWalkAnimation(walkAnimation);
        player.setWalkRotateLeft(walkAnimation);
        player.setWalkRotateRight(walkAnimation);
        player.setWalkRotate180(walkAnimation);
        player.setRunAnimation(runAnimation);

        int signature = 31 * (31 * idleAnimation + walkAnimation) + runAnimation;
        if (activePoseSignature != signature)
        {
            player.setPoseAnimation(idleAnimation);
            player.setPoseAnimationFrame(0);
            activePoseSignature = signature;
        }
    }

    private static RiderAnimation choice(RiderAnimation animation)
    {
        return animation == null ? RiderAnimation.MINECART : animation;
    }

    private void applyEquipmentVisibility(Player player)
    {
        int signature = (config.hideCape() ? 4 : 0)
            | (config.hideWeapon() ? 2 : 0)
            | (config.hideShield() ? 1 : 0);
        if (signature == 0)
        {
            restoreEquipment();
            activeVisibilitySignature = 0;
            return;
        }

        PlayerComposition composition = player.getPlayerComposition();
        if (composition == null)
        {
            return;
        }
        if (composition == hiddenComposition && activeVisibilitySignature == signature)
        {
            return;
        }

        restoreEquipment();
        composition = player.getPlayerComposition();
        int[] equipment = composition == null ? null : composition.getEquipmentIds();
        if (equipment == null)
        {
            return;
        }
        hiddenComposition = composition;
        originalEquipment = equipment.clone();
        activeVisibilitySignature = signature;

        if (config.hideCape())
        {
            hideSlot(equipment, KitType.CAPE);
        }
        if (config.hideWeapon())
        {
            hideSlot(equipment, KitType.WEAPON);
        }
        if (config.hideShield())
        {
            hideSlot(equipment, KitType.SHIELD);
        }
        composition.setHash();
    }

    private static void hideSlot(int[] equipment, KitType type)
    {
        int index = type.getIndex();
        if (index >= 0 && index < equipment.length)
        {
            equipment[index] = 0;
        }
    }

    private void restoreEquipment()
    {
        if (hiddenComposition != null && originalEquipment != null)
        {
            int[] equipment = hiddenComposition.getEquipmentIds();
            if (equipment != null && equipment.length == originalEquipment.length)
            {
                System.arraycopy(originalEquipment, 0, equipment, 0, equipment.length);
                hiddenComposition.setHash();
            }
        }
        hiddenComposition = null;
        originalEquipment = null;
        activeVisibilitySignature = Integer.MIN_VALUE;
    }

    private int modelSignature()
    {
        int signature = 31 * config.cartScale() + (config.recolourMinecart() ? 1 : 0);
        signature = 31 * signature + colourRgb(config.minecartColour());
        signature = 31 * signature + colourRgb(config.wheelColour());
        return 31 * signature + colourRgb(config.hubColour());
    }

    private static int colourRgb(Color colour)
    {
        return colour == null ? 0 : colour.getRGB();
    }

    private void ensureMount(MountType type, LocalPoint location, int plane)
    {
        if (mountObject != null && mountObject.isActive() && activeMount == type)
        {
            return;
        }
        clearMount();
        Model model = modelFactory.create(type, config.cartScale(), config.recolourMinecart(),
            config.minecartColour(), config.wheelColour(), config.hubColour());
        if (model == null)
        {
            return;
        }

        RuneLiteObject created = client.createRuneLiteObject();
        created.setModel(model);
        created.setLocation(location, plane);
        created.setRadius(120);
        try
        {
            if (type.mountAnimation >= 0)
            {
                Animation animation = client.loadAnimation(type.mountAnimation);
                if (animation != null)
                {
                    created.setAnimation(animation);
                }
            }
            created.setActive(true);
            CART_OBJECTS.add(created);
            mountObject = created;
            activeMount = type;
        }
        catch (RuntimeException | AssertionError ex)
        {
            removeCart(created);
        }
    }

    boolean isUserMounted()
    {
        return userMounted;
    }

    void toggleMounted()
    {
        userMounted = !userMounted;
        playTogglePoof();
        resetCartState();
    }

    private void playTogglePoof()
    {
        Player player = client.getLocalPlayer();
        if (!config.togglePoof() || player == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        try
        {
            player.createSpotAnim(TOGGLE_POOF_SLOT, SpotanimID.SMOKEPUFF, 0, 0);
        }
        catch (RuntimeException | AssertionError ignored)
        {
            // Cosmetic only; never let an unavailable effect block the mount toggle.
        }
    }

    private void resetCartState()
    {
        restorePlayerPose();
        for (RuneLiteObject cart : new ArrayList<>(CART_OBJECTS))
        {
            removeCart(cart);
        }
        CART_OBJECTS.clear();
        mountObject = null;
        activeMount = null;
        modelFactory.clear();
        activeModelSignature = Integer.MIN_VALUE;
    }

    private void restorePlayerPose()
    {
        restoreEquipment();
        if (mountedPlayer != null && originalPose != null)
        {
            originalPose.restore(mountedPlayer);
        }
        mountedPlayer = null;
        originalPose = null;
        activePoseSignature = Integer.MIN_VALUE;
    }

    private void clearMount()
    {
        if (mountObject != null)
        {
            removeCart(mountObject);
            CART_OBJECTS.remove(mountObject);
            mountObject = null;
        }
        activeMount = null;
    }

    private void removeCart(RuneLiteObject cart)
    {
        cart.setActive(false);
        if (client.isRuneLiteObjectRegistered(cart))
        {
            client.removeRuneLiteObject(cart);
        }
    }
}