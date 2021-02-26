package com.caviedes.jazz.porque.si.json.pojos;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Item{
	
    private String uri;
    private String htmlUrl;
    private String htmlShortUrl;
    private String id;
    private String language;
    private Object anteTitle;
    private Object anteTitleUrl;
    private String longTitle;
    private String shortTitle;
    private String mainCategoryRef;
    private String popularity;
    private String popHistoric;
    private String numVisits;
    private String publicationDate;
    private String modificationDate;
    private PubState pubState;
    private String mainTopic;
    private List<String> topicsName;
    private String breadCrumbRef;
    private String imageSEO;
    private String thumbnail;
    private Previews previews;
    private String expirationDate;
    private String dateOfEmission;
    private Object publicationDateTimestamp;
    private String contentType;
    private String consumption;
    private Type type;
    private String assetType;
    private Statistics statistics;
    private String alt;
    private String foot;
    private String shortDescription;
    private String description;
    private String otherTopicsRefs;
    private List<Quality> qualities;
    private String qualitiesRef;
    private int duration;
    private ProgramInfo programInfo;
    private Object sgce;
    private CommentOptions commentOptions;
    private String cuePointsRef;
    private String configPlayerRef;
    private String transcriptionRef;
    private String temporadasRef;
    private String programRef;
    private String relacionadosRef;
    private String relManualesRef;
    private String publicidadRef;
    private String comentariosRef;
    private String relatedByLangRef;
    private Sign sign;
    private String estadisticasRef;
    private Object ageRangeUid;
    private Object ageRange;
    private Object contentInitDate;
    private Object contentEndDate;
    private boolean disabledAlacarta;
    private Object promoTitle;
    private Object promoDesc;
    private String title;
}