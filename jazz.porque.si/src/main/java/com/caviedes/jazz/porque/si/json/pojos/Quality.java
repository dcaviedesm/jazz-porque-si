package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value @Jacksonized @Builder
public class Quality {
    int identifier;
    String filePath;
    String preset;
    int filesize;
    String type;
    int duration;
    int bitRate;
    Object bitRateUnit;
    String language;
    int numOfChannels;
}
