package net.dinikin.clansbattle.plugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClansBattleData implements Serializable {
    private static transient final long serialVersionUID = -1681012206529286330L;
    private final Map<String, Region> regionsMap = new HashMap<>();

    public ClansBattleData() {
    }

    public ClansBattleData(ClansBattleData loadedData) {
        regionsMap.putAll(loadedData.getRegionsMap());
    }

    public Map<String, Region> getRegionsMap() {
        return regionsMap;
    }
}
