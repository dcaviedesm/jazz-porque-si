package com.caviedes.jazz.porque.si.json.pojos;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
@Value
public class Item {
    String uri;
    String htmlUrl;
    String htmlShortUrl;
    String id;
    String language;
    Object anteTitle;
    Object anteTitleUrl;
    String longTitle;
    String shortTitle;
    String mainCategoryRef;
    String popularity;
    String popHistoric;
    String numVisits;
    String publicationDate;
    String modificationDate;
    PubState pubState;
    String mainTopic;
    List<String> topicsName;
    String breadCrumbRef;
    String imageSEO;
    String thumbnail;
    Previews previews;
    String expirationDate;
    String dateOfEmission;
    Object publicationDateTimestamp;
    String contentType;
    String consumption;
    Type type;
    String assetType;
    Statistics statistics;
    String alt;
    String foot;
    String shortDescription;
    String description;
    String otherTopicsRefs;
    List<Quality> qualities;
    String qualitiesRef;
    int duration;
    ProgramInfo programInfo;
    Object sgce;
    CommentOptions commentOptions;
    String cuePointsRef;
    String configPlayerRef;
    String transcriptionRef;
    String temporadasRef;
    String programRef;
    String relacionadosRef;
    String relManualesRef;
    String publicidadRef;
    String comentariosRef;
    String relatedByLangRef;
    Sign sign;
    String estadisticasRef;
    Object ageRangeUid;
    Object ageRange;
    Object contentInitDate;
    Object contentEndDate;
    boolean disabledAlacarta;
    Object promoTitle;
    Object promoDesc;
    String title;
}
