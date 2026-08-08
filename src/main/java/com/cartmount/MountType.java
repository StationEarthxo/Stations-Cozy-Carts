package com.cartmount;

import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;

public enum MountType
{
    MINECART("Minecart", MountSource.OBJECT, ObjectID.MINECART,
        AnimationID.HUMAN_MINECART_ANIM, -1, 100, 16, 1);

    final String label;
    final MountSource source;
    final int sourceId;
    final int playerAnimation;
    final int mountAnimation;
    final int modelScale;
    final int grounding;
    final int facingQuarterTurns;

    MountType(String label, MountSource source, int sourceId, int playerAnimation,
        int mountAnimation, int modelScale, int grounding, int facingQuarterTurns)
    {
        this.label = label;
        this.source = source;
        this.sourceId = sourceId;
        this.playerAnimation = playerAnimation;
        this.mountAnimation = mountAnimation;
        this.modelScale = modelScale;
        this.grounding = grounding;
        this.facingQuarterTurns = facingQuarterTurns;
    }

    @Override
    public String toString()
    {
        return label;
    }
}