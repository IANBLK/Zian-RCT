package com.ianblk.zianrct.standalone;

/** Bound a positive award to the XP needed to reach the unlocked level. */
public final class ExperienceCapPolicy {
    private ExperienceCapPolicy(){}
    public static int allowed(int level,int cap,int experience,int capExperience,int requested){
        if(requested<=0)return requested;
        if(level>=cap)return 0;
        return (int)Math.min(requested,Math.max(0L,(long)capExperience-experience));
    }
}
