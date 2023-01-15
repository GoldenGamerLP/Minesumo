package me.alex.minesumo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListUtils {

    public static <T> String formatList(List<T> list, Function<T, String> fun) {
        return list.stream()
                .map(fun)
                .collect(Collectors.joining(", "));
    }

    /**
     * Splits the given objects in to number of groups. Excess objects get added randomly to the groups.
     *
     * @param objects   the objects to split
     * @param numGroups the number of groups to split to
     * @param <T>       The type of objects
     * @return a list of groups with the list of objects
     * @author Chat-GPT - Alex/GoldenGamer
     */
    public static <T> List<List<T>> distributeNumbers(List<T> objects, int numGroups) {
        // Create a list of lists to store the distributed numbers
        List<List<T>> distributedNumbers = new ArrayList<>();

        // Distribute the numbers evenly into the specified number of groups
        int groupSize = objects.size() / numGroups;
        for (int i = 0; i < numGroups; i++) {
            List<T> group = new ArrayList<>();
            for (int j = 0; j < groupSize; j++) {
                group.add(objects.get(i * groupSize + j));
            }
            distributedNumbers.add(group);
        }

        // Distribute the excess numbers evenly among the groups
        int excess = objects.size() % numGroups;
        for (int i = 0; i < excess; i++) {
            distributedNumbers.get(i).add(objects.get(objects.size() - excess + i));
        }

        return distributedNumbers;
    }
}
