package me.EvilMuffinHa.minecraftManhunt;

public enum Arrows {
	LEFT("⬅"), RIGHT("➡"), UP("⬆"), DOWN("⬇"), UPRIGHT("⬈"), UPLEFT("⬉"), DOWNRIGHT("⬊"), DOWNLEFT("⬋");

	private String arrow;

	Arrows(String s) {
		this.arrow = s;
	}

	public String getArrow() {
		return arrow;
	}
}
