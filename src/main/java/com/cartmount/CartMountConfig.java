package com.cartmount;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(CartMountConfig.GROUP)
public interface CartMountConfig extends Config
{
    String GROUP = "cartMountMode";

    @Range(min = 60, max = 140)
    @ConfigItem(keyName = "cartScale", name = "Cart size",
        description = "Adjust the minecart's size", position = 0)
    default int cartScale()
    {
        return 100;
    }

    @Range(min = 0, max = 3)
    @ConfigItem(keyName = "facingOffset", name = "Facing correction",
        description = "Apply an extra rotation in quarter turns", position = 1)
    default int facingOffset()
    {
        return 0;
    }

    @Range(min = -48, max = 48)
    @ConfigItem(keyName = "groundingOffset", name = "Cart height",
        description = "Positive values lower the cart; negative values raise it", position = 2)
    default int groundingOffset()
    {
        return 0;
    }

    @ConfigItem(keyName = "idleAnimation", name = "Idle animation",
        description = "Animation used while the minecart is still", position = 3)
    default RiderAnimation idleAnimation()
    {
        return RiderAnimation.MINECART;
    }

    @ConfigItem(keyName = "walkAnimation", name = "Walk animation",
        description = "Animation used while walking in the minecart", position = 4)
    default RiderAnimation walkAnimation()
    {
        return RiderAnimation.MINECART;
    }

    @ConfigItem(keyName = "runAnimation", name = "Run animation",
        description = "Animation used while running in the minecart", position = 5)
    default RiderAnimation runAnimation()
    {
        return RiderAnimation.MINECART;
    }

    @ConfigItem(keyName = "recolourMinecart", name = "Recolour cart",
        description = "Apply custom colours to the minecart", position = 6)
    default boolean recolourMinecart()
    {
        return false;
    }

    @ConfigItem(keyName = "minecartColour", name = "Body colour",
        description = "Colour of the cart body", position = 7)
    default Color minecartColour()
    {
        return new Color(120, 70, 45);
    }

    @ConfigItem(keyName = "wheelColour", name = "Wheel colour",
        description = "Colour of the cart wheels", position = 8)
    default Color wheelColour()
    {
        return new Color(45, 45, 50);
    }

    @ConfigItem(keyName = "hubColour", name = "Wheel hub colour",
        description = "Colour of the middle caps on the wheels", position = 9)
    default Color hubColour()
    {
        return new Color(190, 145, 55);
    }

    @ConfigItem(keyName = "hideCape", name = "Hide cape",
        description = "Hide the local player's cape", position = 10)
    default boolean hideCape()
    {
        return false;
    }

    @ConfigItem(keyName = "hideWeapon", name = "Hide sword",
        description = "Hide the local player's weapon", position = 11)
    default boolean hideWeapon()
    {
        return false;
    }

    @ConfigItem(keyName = "hideShield", name = "Hide shield",
        description = "Hide the local player's shield", position = 12)
    default boolean hideShield()
    {
        return false;
    }

    @Range(min = 24, max = 96)
    @ConfigItem(keyName = "buttonSize", name = "Button size",
        description = "Size of the clickable cart button in pixels", position = 13)
    default int buttonSize()
    {
        return 44;
    }

    @ConfigItem(keyName = "togglePoof", name = "Toggle poof",
        description = "Play a small smoke poof when mounting or dismounting", position = 14)
    default boolean togglePoof()
    {
        return true;
    }

    @ConfigItem(keyName = "resetCart", name = "Reset cart",
        description = "Toggle this whenever the cart or rider pose needs to be rebuilt", position = 15)
    default boolean resetCart()
    {
        return false;
    }
}