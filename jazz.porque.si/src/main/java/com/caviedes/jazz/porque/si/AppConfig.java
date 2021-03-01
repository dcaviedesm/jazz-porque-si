package com.caviedes.jazz.porque.si;

import lombok.Getter;

import java.util.Properties;

public class AppConfig {
    public enum Parameter {
        MAIN_FOLDER_PATH("C:\\Temp\\Jazz porque si"),
        AUDIOS_EXTENSION(".mp3"),
        JSON_URL("http://www.rtve.es/api/programas/1999/audios.json"),
        JSON_PATH("E:\\Descargas\\Jazz_porque_si_audios.json"),
        DATE_INVERTED_PATTERN("yyMMdd"),
        ORIGINAL_DATE_PATTERN("dd-MM-yyyy HH:mm:ss"),
        AUDIOS_PER_PAGE(20);

        @Getter
        private final Object defaultValue;

        Parameter(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    private final Properties parameters = new Properties();

    public AppConfig() {
    }

    public void putParameter(Parameter parameter, Object value){
        parameters.put(parameter.name(),value);
    }

    public String getParameter(Parameter parameter) {
        return parameters.getProperty(parameter.name(), (String) parameter.getDefaultValue());
    }

    public Integer getParameterAsInt(Parameter parameter) {
        return (Integer) parameters.getOrDefault(parameter.name(), parameter.getDefaultValue());
    }
}
