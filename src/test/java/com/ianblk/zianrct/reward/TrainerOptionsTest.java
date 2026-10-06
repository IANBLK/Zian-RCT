package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.rct.RctTrainerOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainerOptionsTest {
    @TempDir Path temp;
    @org.junit.jupiter.api.AfterEach void cleanup() throws Exception {RctTrainerOptions.clear();RewardTestFiles.cleanup(temp);}
    @Test void formatSelectionSurvivesRestartAndRejectsUnsupportedFormatsBeforeWriting() throws Exception {
        RctTrainerOptions.boot(temp);
        RctTrainerOptions.format("rassvet_leader_novato","GEN_9_DOUBLES");
        Path path=temp.resolve("zianrct-trainer-options.json");
        String saved=Files.readString(path);
        assertThrows(IllegalArgumentException.class,()->RctTrainerOptions.format("rassvet_leader_novato","arbitrary"));
        assertEquals(saved,Files.readString(path));
        RctTrainerOptions.clear();RctTrainerOptions.boot(temp);
        assertEquals("GEN_9_DOUBLES",RctTrainerOptions.formats().get("rassvet_leader_novato"));
    }
    @Test void repeatSetIsLoadedFromPolicyWithoutChangingUniqueDefaults() throws Exception {
        var config=TrainerRewardConfig.open(temp.resolve("zianrct-rewards.json"));
        config.set("repeat",new RewardDefinition("avecoins:coppercoin",5,java.util.List.of(),RewardDefinition.Mode.REPEAT,1));
        config.set("unique",new RewardDefinition("avecoins:coppercoin",5,java.util.List.of()));
        RctTrainerOptions.boot(temp);
        assertEquals(java.util.Set.of("repeat"),RctTrainerOptions.repeat());
    }
}
