package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value @Jacksonized @Builder
public class Root {
    Page page;
}
