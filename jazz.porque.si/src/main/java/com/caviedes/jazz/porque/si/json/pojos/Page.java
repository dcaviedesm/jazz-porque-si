package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Value;

import java.util.List;

@Value
public class Page {
    List<Item> items;
    int number;
    int size;
    int offset;
    int total;
    int totalPages;
    int numElements;
}
