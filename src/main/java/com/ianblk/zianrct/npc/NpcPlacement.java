package com.ianblk.zianrct.npc;

/** The administrator's current block centre, exact feet height and horizontal facing. */
public record NpcPlacement(double x, double y, double z, float yaw) {
    public static NpcPlacement at(double x, double feetY, double z, float yaw) {
        if (!Double.isFinite(x) || !Double.isFinite(feetY) || !Double.isFinite(z) || !Float.isFinite(yaw))
            throw new IllegalArgumentException("Invalid NPC placement");
        return new NpcPlacement(Math.floor(x) + 0.5, feetY, Math.floor(z) + 0.5, yaw);
    }
}
