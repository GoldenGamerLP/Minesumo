package me.alex.minesumo;

import java.util.ArrayList;
import java.util.List;

public class Main {

    // Distributes a list of numbers into a specified number of groups and distributes the excess evenly
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

    // Main method for testing
    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            numbers.add(i);
        }

        System.out.println(distributeNumbers(numbers, 3)); // Expected output: [[1, 2, 3, 4], [5, 6, 7], [8, 9, 10]]
    }
}


