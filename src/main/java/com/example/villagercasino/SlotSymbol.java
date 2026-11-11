package com.example.villagercasino;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Symbole der Walzen inkl. Gewichtung (Wahrscheinlichkeit) und Gewinn bei drei gleichen. */
public enum SlotSymbol {
	HEART("\u2665", ChatFormatting.RED, 30, 4),
	CLUB("\u2663", ChatFormatting.GREEN, 25, 6),
	DIAMOND("\u2666", ChatFormatting.AQUA, 20, 10),
	STAR("\u2605", ChatFormatting.GOLD, 15, 20),
	SEVEN("7", ChatFormatting.DARK_RED, 10, 64);

	private static final int TOTAL_WEIGHT;

	static {
		int sum = 0;
		for (SlotSymbol symbol : values()) {
			sum += symbol.weight;
		}
		TOTAL_WEIGHT = sum;
	}

	private final String glyph;
	private final ChatFormatting color;
	private final int weight;
	private final int tripleWin;

	SlotSymbol(String glyph, ChatFormatting color, int weight, int tripleWin) {
		this.glyph = glyph;
		this.color = color;
		this.weight = weight;
		this.tripleWin = tripleWin;
	}

	public static SlotSymbol roll(RandomSource random) {
		int roll = random.nextInt(TOTAL_WEIGHT);
		for (SlotSymbol symbol : values()) {
			roll -= symbol.weight;
			if (roll < 0) {
				return symbol;
			}
		}
		return HEART;
	}

	/** Gewinn in Smaragden (Einsatz = 1 Smaragd): 3 gleiche = Tabelle, 2 gleiche = Einsatz zurueck. */
	public static int payout(SlotSymbol a, SlotSymbol b, SlotSymbol c) {
		if (a == b && b == c) {
			return a.tripleWin;
		}
		if (a == b || b == c || a == c) {
			return 1;
		}
		return 0;
	}

	public Component display() {
		return Component.literal(glyph).withStyle(color, ChatFormatting.BOLD);
	}
}
