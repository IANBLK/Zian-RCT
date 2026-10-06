package com.ianblk.zianrct.reward;

import java.util.List;

public record RewardDefinition(String currency, long coins, List<String> items, Mode mode, long cooldownMinutes) {
    public enum Mode { UNIQUE, REPEAT }
    public RewardDefinition(String currency,long coins,List<String> items){this(currency,coins,items,Mode.UNIQUE,0);}
    public RewardDefinition prize(String currency,long coins,List<String> items){return new RewardDefinition(currency,coins,items,mode,cooldownMinutes);}
    public RewardDefinition {
        if(mode==null || (mode==Mode.UNIQUE && cooldownMinutes!=0) || (mode==Mode.REPEAT && (cooldownMinutes<1 || cooldownMinutes>43200)))
            throw new IllegalArgumentException("Espera inválida: repetible requiere entre 1 minuto y 30 días.");
        currency = currency == null ? "" : currency;
        items = List.copyOf(items == null ? List.of() : items);
        if (currency.length() > 128 || (!currency.isEmpty() && !currency.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))
            throw new IllegalArgumentException("Moneda inválida");
        if (coins < 0 || coins > 1728 || (coins > 0 && !currency.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))
            throw new IllegalArgumentException("Moneda o importe inválido (máximo 1728).");
        if (items.size() > 8 || items.stream().anyMatch(s -> s == null || s.isBlank() || s.length() > 65536))
            throw new IllegalArgumentException("Máximo 8 objetos válidos por entrenador.");
    }
    public boolean empty() { return coins == 0 && items.isEmpty(); }
}
