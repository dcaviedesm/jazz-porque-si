package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Quality{
    private int identifier;
    private String filePath;
    private String preset;
    private int filesize;
    private String type;
    private int duration;
    private int bitRate;
    private Object bitRateUnit;
    private String language;
    private int numOfChannels;
}