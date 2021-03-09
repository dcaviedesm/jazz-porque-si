package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Value
public class Previews {
    Object horizontal;
    Object horizontal2;
    Object vertical;
    Object vertical2;
    Object square;
    Object square2;
}
