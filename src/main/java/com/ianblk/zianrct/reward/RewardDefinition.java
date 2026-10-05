package com.ianblk.zianrct.reward;

import java.util.List;

public record RewardDefinition(String currency, long coins, List<String> items) {
    public RewardDefinition {
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
