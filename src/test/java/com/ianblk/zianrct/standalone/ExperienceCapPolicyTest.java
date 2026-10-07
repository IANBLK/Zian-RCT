package com.ianblk.zianrct.standalone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExperienceCapPolicyTest {
    @Test void largeCandyAwardStopsExactlyAtInitialCap(){assertEquals(200,ExperienceCapPolicy.allowed(9,10,800,1000,30000));}
    @Test void pokemonAtOrAboveCapReceivesNoMoreXp(){assertEquals(0,ExperienceCapPolicy.allowed(10,10,1000,1000,30000));assertEquals(0,ExperienceCapPolicy.allowed(31,10,40000,1000,30000));}
    @Test void nextUnlockedCapAllowsTrainingAgain(){assertEquals(30000,ExperienceCapPolicy.allowed(10,20,1000,50000,30000));}
    @Test void maximumCapStillBlocksExtraExperience(){assertEquals(0,ExperienceCapPolicy.allowed(100,100,1000000,1000000,30000));}
    @Test void smallAwardsAndFinalCapAreUnchanged(){assertEquals(20,ExperienceCapPolicy.allowed(8,10,500,1000,20));assertEquals(30000,ExperienceCapPolicy.allowed(90,100,1,50000,30000));}
}
