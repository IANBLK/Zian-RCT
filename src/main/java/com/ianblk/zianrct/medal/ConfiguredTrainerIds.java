package com.ianblk.zianrct.medal;
import com.ianblk.zianrct.config.ZianRctConfig;
import java.util.List;
public final class ConfiguredTrainerIds {
    public static String configuredTrainerId(String trainerId, List<ZianRctConfig.ChainEntry> chain) {
        if (trainerId == null || trainerId.isBlank() || chain == null) {
            return null;
        }
        for (ZianRctConfig.ChainEntry entry : chain) {
            if (entry != null && trainerId.equals(entry.trainer())) {
                return trainerId;
            }
        }
        return null;
    }
}
