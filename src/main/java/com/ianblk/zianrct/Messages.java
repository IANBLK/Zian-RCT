package com.ianblk.zianrct;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.nio.file.*;
/** Shared visual vocabulary; configurable server name, without changing component actions. */
public final class Messages {
 private Messages(){}
 private static String prefix="Rassvet";private static long nextRead;private static long modified=Long.MIN_VALUE;
 private static synchronized String prefix(){
  long now=System.nanoTime();if(now<nextRead)return prefix;nextRead=now+5_000_000_000L;
  Path file=net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("zianrct/messages.json");
  try{
   if(!Files.exists(file)){Files.createDirectories(file.getParent());Files.writeString(file,"{\n  \"prefix\": \"Rassvet\"\n}\n",java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}
   if(Files.isSymbolicLink(file)||Files.size(file)>4096)throw new IllegalArgumentException("Archivo de mensajes inválido");
   long stamp=Files.getLastModifiedTime(file).toMillis();if(stamp==modified)return prefix;
   var json=com.google.gson.JsonParser.parseString(Files.readString(file,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
   String next=json.get("prefix").getAsString();if(next.isBlank()||next.length()>32||next.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Prefijo inválido");prefix=next;modified=stamp;
  }catch(Exception error){org.slf4j.LoggerFactory.getLogger("zianrct").warn("No se pudo leer messages.json; se conserva el último prefijo válido: {}",error.toString());}
  return prefix;
 }
 /** Texto de respaldo (español) sin pasar por el idioma del servidor; para datos de paneles de admin y pruebas. */
 public static String plain(Component c){StringBuilder sb=new StringBuilder();plain(c,sb);return sb.toString();}
 private static void plain(Component c,StringBuilder sb){
  if(c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc&&tc.getFallback()!=null){
   Object[] a=tc.getArgs().clone();for(int i=0;i<a.length;i++)if(a[i] instanceof Component x)a[i]=plain(x);
   sb.append(String.format(java.util.Locale.ROOT,tc.getFallback(),a));
  }else c.getContents().visit(s->{sb.append(s);return java.util.Optional.empty();});
  for(Component sibling:c.getSiblings())plain(sibling,sb);
 }
 /** Error para el jugador: traducible si es una LocalizedException, literal en otro caso. */
 public static Component of(Throwable error){return error instanceof LocalizedException.Info info?c(info.msg()):Component.literal(String.valueOf(error.getMessage()));}
 /** Aviso de red como JSON de Component (el cliente lo muestra en su idioma); decode acepta también texto plano antiguo. */
 public static String encode(Component c){return net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,c).getOrThrow().toString();}
 public static Component decode(String s){
  if(s.startsWith("{"))try{return net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(s)).result().orElse(Component.literal(s));}catch(RuntimeException ignored){}
  return Component.literal(s);
 }
 public static Component c(Msg m){Object[] a=m.args().clone();for(int i=0;i<a.length;i++)if(a[i] instanceof Msg n)a[i]=c(n);return Component.translatableWithFallback(m.key(),m.fallback(),a);}
 /** Texto traducible con respaldo en español; para etiquetas de pantalla que reciben String. */
 public static String t(String key,String fallback,Object... args){return Component.translatableWithFallback(key,fallback,args).getString();}
 public static Component format(Component original){
  String text=original.getString();if(text.isBlank()||text.startsWith("✦"))return original;
  var body=original.copy();if(body.getStyle().getColor()==null)body.withStyle(ChatFormatting.GRAY);
  return Component.literal("✦ ").withStyle(ChatFormatting.GOLD).append(Component.literal(prefix()).withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD)).append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY)).append(body);
 }
}
