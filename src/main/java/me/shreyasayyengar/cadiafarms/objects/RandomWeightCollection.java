package me.shreyasayyengar.cadiafarms.objects;

import java.util.*;

public class RandomWeightCollection<E> {

    private final NavigableMap<Double, E> map = new TreeMap<>();
    private final Random random;
    private double total = 0;

    public RandomWeightCollection() {
        this(new Random());
    }

    public RandomWeightCollection(Random random) {
        this.random = random;
    }

    public Collection<E> values() {
        return this.map.values();
    }

    public void remove(E object) {
        for (Map.Entry<Double, E> entry : this.map.entrySet()) {
            if (entry.getValue().equals(object)) {
                this.map.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    public RandomWeightCollection<E> add(double weight, E result) {
        if (weight <= 0) return this;
        total += weight;
        map.put(total, result);
        return this;
    }

    public E next() {
        double value = random.nextDouble() * total;
        Map.Entry<Double, E> entry = map.higherEntry(value);
        return entry == null ? null : entry.getValue();
    }

    public NavigableMap<Double, E> getMap() {
        return map;
    }
}
