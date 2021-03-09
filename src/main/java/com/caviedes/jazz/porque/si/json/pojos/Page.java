package com.caviedes.jazz.porque.si.json.pojos;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value @Jacksonized @Builder
public class Page {
    List<Item> items;
    int number;
    int size;
    int offset;
    int total;
    int totalPages;
    int numElements;
}
