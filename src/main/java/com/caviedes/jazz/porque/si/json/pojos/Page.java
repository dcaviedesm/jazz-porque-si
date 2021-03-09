package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
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
