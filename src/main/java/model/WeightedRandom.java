package model;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class WeightedRandom<T> {
    private final NavigableMap<Double, T> map = new TreeMap<>();
    private double total = 0;
    private final Random random = new Random();

    public void add(double weight, T value) {
        if (weight <= 0) return;
        total += weight;
        map.put(total, value);
    }

    public T next() {
        double r = random.nextDouble() * total;
        return map.higherEntry(r).getValue();
    }
}
