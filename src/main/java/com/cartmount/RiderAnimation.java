package com.cartmount;

import net.runelite.api.gameval.AnimationID;

public enum RiderAnimation
{
    MINECART("Minecart pose", AnimationID.HUMAN_MINECART_ANIM,
        AnimationID.HUMAN_MINECART_ANIM, AnimationID.HUMAN_MINECART_ANIM),
    SIT_DOWN("Sit Down (seated loop)", AnimationID.EMOTE_SIT_LOOP,
        AnimationID.EMOTE_SIT_LOOP, AnimationID.EMOTE_SIT_LOOP),
    SKIS("Skis", AnimationID.HUMAN_SKI_IDLE,
        AnimationID.HUMAN_SKI_WALK, AnimationID.HUMAN_SKI_RUN);

    final String label;
    final int idleAnimationId;
    final int walkAnimationId;
    final int runAnimationId;

    RiderAnimation(String label, int idleAnimationId, int walkAnimationId, int runAnimationId)
    {
        this.label = label;
        this.idleAnimationId = idleAnimationId;
        this.walkAnimationId = walkAnimationId;
        this.runAnimationId = runAnimationId;
    }

    @Override
    public String toString()
    {
        return label;
    }
}