package com.ianblk.zianrct.standalone;

import com.ianblk.zianrct.config.ConfigState;
import java.util.*;

public final class PlayerProgress {
    private final ProgressLedger ledger;
    public PlayerProgress(ProgressLedger ledger){this.ledger=ledger;}
    public ProgressLedger ledger(){return ledger;}
    public Set<String> getDefeatedTrainerIds(){return ledger.defeated();}
    public int getLevelCap(){
        var profile=ConfigState.current().activeProfileConfig();int cap=profile.initialCap();
        for(int i=0;i<profile.chain().size();i++){
            if(!ledger.defeated().contains(profile.chain().get(i).trainer()))break;
            cap=profile.unlockCap(i);
        }
        return cap;
    }
    public void replaceChain(Set<String> chain,Set<String> desired){
        try{ledger.replaceChain(chain,desired);}catch(java.io.IOException error){throw new IllegalStateException("No se pudo guardar el progreso",error);}
    }
    public void sync(){} // Progress is already durable; medal snapshots use the existing protocol.
}
