package net.curxxed.dev.wintercore.tags;

import lombok.Getter;

@Getter
public class Tag {
    private final String id;
    private final String prefix;
    private final String color;
    private final String name;
    private final int weight;

    public Tag(String id, String prefix, String color, String name, int weight) {
        this.id = id;
        this.prefix = prefix;
        this.color = color;
        this.name = name;
        this.weight = weight;
    }
}