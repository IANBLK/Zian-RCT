package com.ianblk.zianrct.npc;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
/** Presentation only: translation keys, custom content and click actions remain intact. */
public final class NpcDialogueFormat {
 private NpcDialogueFormat(){}
 public static Component line(String name,String text){return line(Component.literal(name),Component.literal(text));}
 public static Component line(Component name,Component text){return Component.empty()
  .append(Component.literal("✦ ").withStyle(ChatFormatting.GOLD))
  .append(name.copy().withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD))
  .append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY))
  .append(text.copy().withStyle(ChatFormatting.WHITE));}
}
