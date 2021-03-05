package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value @Jacksonized @Builder
public class Previews {
    private Object horizontal;
    private Object horizontal2;
    private Object vertical;
    private Object vertical2;
    private Object square;
    private Object square2;
}
