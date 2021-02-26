package com.caviedes.jazz.porque.si.json.pojos;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Page {

	private List<Item> items;
    private int number;
    private int size;
    private int offset;
    private int total;
    private int totalPages;
    private int numElements;
}
