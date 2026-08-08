package com.cartmount;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CartMountPluginTest
{
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(CartMountPlugin.class);
        RuneLite.main(args);
    }

    @Test
    public void pluginUsesCanonicalKeldagrimAssets()
    {
        assertEquals(AnimationID.HUMAN_MINECART_ANIM, CartMountPlugin.MOUNTED_ANIMATION);
        assertEquals(AnimationID.HUMAN_MINECART_ANIM, RiderAnimation.MINECART.idleAnimationId);
        assertEquals(AnimationID.HUMAN_MINECART_ANIM, RiderAnimation.MINECART.walkAnimationId);
        assertEquals(AnimationID.HUMAN_MINECART_ANIM, RiderAnimation.MINECART.runAnimationId);
        assertEquals(AnimationID.EMOTE_SIT_LOOP, RiderAnimation.SIT_DOWN.idleAnimationId);
        assertEquals(AnimationID.HUMAN_SKI_IDLE, RiderAnimation.SKIS.idleAnimationId);
        assertEquals(AnimationID.HUMAN_SKI_WALK, RiderAnimation.SKIS.walkAnimationId);
        assertEquals(AnimationID.HUMAN_SKI_RUN, RiderAnimation.SKIS.runAnimationId);
        assertEquals(ObjectID.MINECART, CartModelFactory.MINECART_OBJECT_ID);
    }

    @Test
    public void catalogueIsMinecartOnlyAndInternallyValid()
    {
        assertEquals(1, MountType.values().length);
        Set<String> labels = new HashSet<>();
        for (MountType mount : MountType.values())
        {
            assertTrue(mount.sourceId >= 0);
            assertTrue(mount.playerAnimation >= 0);
            assertTrue(mount.modelScale > 0);
            assertTrue(labels.add(mount.label));
        }
    }

    @Test
    public void pluginIsLoadable()
    {
        assertTrue(Plugin.class.isAssignableFrom(CartMountPlugin.class));
    }
}