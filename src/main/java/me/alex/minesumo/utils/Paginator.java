package me.alex.minesumo.utils;

import org.jetbrains.annotations.Contract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Paginator<T> {

    private final Double pagSize;
    private T[] objects;
    private Integer currentPage;
    private Integer amountOfPages;

    @Contract(pure = true)
    public Paginator(T[] objects, Integer max) {
        this.objects = objects;
        pagSize = max.doubleValue();
    }

    public Paginator(List<T> objects, Integer max) {
        this(objects.toArray((T[]) new Object[0]), max);
    }

    public void setElements(List<T> objects) {
        this.objects = objects.toArray((T[]) new Object[0]);
    }

    public boolean hasNext() {
        return currentPage < amountOfPages;
    }

    public boolean hasPrev() {
        return currentPage > 1;
    }

    public int getNext() {
        return currentPage + 1;
    }

    public int getPrev() {
        return currentPage - 1;
    }

    public Map<T, Integer> getPage(Integer pageNum) {
        LinkedHashMap<T, Integer> pages = new LinkedHashMap<>();
        double total = objects.length / pagSize;
        amountOfPages = (int) Math.ceil(total);
        currentPage = pageNum;

        if (objects.length == 0)
            return pages;

        double startC = pagSize * (pageNum - 1);
        double finalC = startC + pagSize;

        for (; startC < finalC; startC++)
            if (startC < objects.length)
                pages.put(objects[(int) startC], (int) startC);
        return pages;

    }

}
