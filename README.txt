Proyecto Java utilizado para renombrar y etiquetar metadatos MP3 de todos los programas de "Jazz porque sí" de RTVE previamente descargados.

Este proyecto Java (utiliza Maven) tiene como clase central ejecutable la clase "App.java". Para que funcione, hay que tener en cuenta varios aspectos:
*Los programas de "Jazz porque sí" tienen que estar organizados por carpetas igual que están en la web de RTVE, donde se organizan por páginas de 20 audios en este caso, por tanto, en un directorio cualquiera, deberá existir una carpeta por cada página de la web del programa "Jazz porque sí" de RTVE
*Dichas carpetas deben nombrarse según el siguiente formato: "XXX YY", donde "XXX" puede ser cualquier cosa (en nuestro caso pusimos la cadena "Pag") e "YY" debe ser el nº de página correspondiente a la web de RTVE (p.ej "01")
*Cada carpeta contendrá los audios correspondientes a su página equivalente en la web de RTVE
*El nombre inicial de los audios será el mismo nombre original existente al descargar cada audio de "Jazz porque sí", el cual es un número seguido de la extensión MP3 (p.ej "1552503521627.mp3")

Los aspectos anteriores son importantes ya que el proyecto Java los presupone.
Dentro de la clase principal del proyecto, existen una serie de constantes definidas de las cuales algunas son necesarias personalizarlas para cada caso. Indicamos el significado de cada una de ellas:
*mainFolderPath: ruta física donde se encuentran las carpetas que representan las páginas de audios (Pag 01, Pag 02, etc)
*audiosExtension: extensión de los ficheros de audio
*jsonUrl: URL base de la API de RTVE para obtener la información de los audios del programa de "Jazz porque sí" (cuyo identificador de RTVE es "1999")
*jsonPath: ruta física por si tuviésemos un único JSON con toda la información de los audios (obsoleto)
*dateInvertedPattern: patrón de fecha que será utilizado para renombrar el principio de los nombres de cada audio
*originalDatePattern: patrón de fecha/hora original procedente de la API de RTVE
*audiosPerPage: número de audios por página de la web de RTVE

Básicamente, la lógica del proyecto es:
*Recorrer cada audio de cada carpeta, y por cada uno:
	-Obtiene su información de la API de RTVE
	-Renombra el audio según el formato: yyMMdd_TituloProcedenteApiRtve.extension
	-Setea en el audio algunos metadatos MP3 (en principio título y descripción)