package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value @Jacksonized @Builder
public class ProgramInfo {
    String title;
    String htmlUrl;
    String channelPermalink;
    Object ageRangeUid;
    Object ageRange;
}
