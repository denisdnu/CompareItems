package net.dinikin.clansbattle.plugin;

public enum ClansBattleCommandsEnum {
    BUY("buy"),
    PUT("put"),
    PREVIEW("preview"),
    LIST("list"),
    PURCHASED("purchased"),
    HELP("help");

    private String name;

    ClansBattleCommandsEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
