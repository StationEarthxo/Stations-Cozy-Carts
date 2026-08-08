package com.cartmount;

import net.runelite.api.Player;

final class PlayerPoseSnapshot
{
    private final int pose;
    private final int idle;
    private final int idleLeft;
    private final int idleRight;
    private final int walk;
    private final int walkLeft;
    private final int walkRight;
    private final int walkBack;
    private final int run;

    private PlayerPoseSnapshot(Player player)
    {
        pose = player.getPoseAnimation();
        idle = player.getIdlePoseAnimation();
        idleLeft = player.getIdleRotateLeft();
        idleRight = player.getIdleRotateRight();
        walk = player.getWalkAnimation();
        walkLeft = player.getWalkRotateLeft();
        walkRight = player.getWalkRotateRight();
        walkBack = player.getWalkRotate180();
        run = player.getRunAnimation();
    }

    static PlayerPoseSnapshot capture(Player player)
    {
        return new PlayerPoseSnapshot(player);
    }

    void restore(Player player)
    {
        player.setIdlePoseAnimation(idle);
        player.setIdleRotateLeft(idleLeft);
        player.setIdleRotateRight(idleRight);
        player.setWalkAnimation(walk);
        player.setWalkRotateLeft(walkLeft);
        player.setWalkRotateRight(walkRight);
        player.setWalkRotate180(walkBack);
        player.setRunAnimation(run);
        player.setPoseAnimation(pose);
    }
}
