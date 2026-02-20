package model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemList implements Iterable<ItemList.Entry> {

    public static class Entry {
        private final Material material;
        private double amount;

        public Entry(Material material) {
            this.material = material;
            this.amount = 0.0;
        }

        public Material getMaterial() { return material; }
        public double getAmount() { return amount; }
        public void addAmount(double value) { this.amount += value; }

        /**
         * Уменьшает и возвращает true, если после вычитания amount == 0 (требуется удалить).
         * Не удаляет сам entry — это ответственность ItemList.
         */
        public boolean subtractAmount(int value) {
            amount -= value;
            return amount <= 0;
        }
    }

    // Синхронизированный список + concurrent map для быстрого доступа по материалу
    private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    private final Map<Material, Entry> entryMap = new ConcurrentHashMap<>();

    /**
     * Добавляет amount для материала. Синхронизация нужна, чтобы не ломать entries при параллельных операциях.
     */
    public void addAmount(Material material, double amount) {
        // синхронизируемся на entries, чтобы совместно с сортировкой и итерацией были атомарные изменения списка
        synchronized (entries) {
            Entry entry = entryMap.get(material);
            if (entry == null) {
                entry = new Entry(material);
                entryMap.put(material, entry);
                entries.add(entry);
            }
            entry.addAmount(amount);
        }
    }

    // если нужен, добавь метод, который возвращает точное булево значение успеха:
    public boolean subtractAmount(Material material, int amountToSubtract) {
        synchronized (entries) {
            Entry entry = entryMap.get(material);
            if (entry == null || entry.getAmount() < amountToSubtract) {
                return false;
            }

            boolean becameZero = entry.subtractAmount(amountToSubtract);
            if (becameZero) {
                entryMap.remove(material);
                entries.remove(entry);
            }
            return true;
        }
    }

    /**
     * Пересортировка по убыванию amount.
     * ВАЖНО: синхронизируемся на entries, чтобы никто не менял список в процессе сортировки.
     */
    public void sortDescending() {
        synchronized (entries) {
            entries.sort(Comparator.comparingDouble(Entry::getAmount).reversed());
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Итератор — возвращаем snapshot (копию) чтобы внешний код мог безопасно итерировать без синхронизации.
     */
    @NotNull
    @Override
    public Iterator<Entry> iterator() {
        // делаем копию под синхронизацией
        synchronized (entries) {
            return new ArrayList<>(entries).iterator();
        }
    }

    // Нельзя отдавать сам entries напрямую, отдаём unmodifiable копию
    public List<Entry> getEntries() {
        synchronized (entries) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }
}