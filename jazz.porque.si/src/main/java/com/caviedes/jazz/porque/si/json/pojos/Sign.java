package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value @Jacksonized @Builder
public class Sign {
    Object ctvId;
    Object name;
    String firma;
    Object photo;
    Object twitter;
    Object facebook;
    Object otras;
    Object publicationDate;
    Object numPublications;
}
