/*
 * Copyright (c) 2012 Gerrit Grunwald
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.hansolo.fxgtools.main

import eu.hansolo.fxgtools.fxg.FxgElement
import eu.hansolo.fxgtools.fxg.FxgVariable
import eu.hansolo.fxgtools.fxg.Language
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.effect.InnerShadow

import java.util.regex.Matcher
import java.util.regex.Pattern
import eu.hansolo.fxgtools.fxg.FxgRichText

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 20:36
 * To change this template use File | Settings | File Templates.
 */
class FxgTranslator {
    private static final Pattern CANVAS_PATTERN = Pattern.compile("_?(canvas)", Pattern.CASE_INSENSITIVE)
    private StringBuilder     allLayers         = new StringBuilder()
    private StringBuilder     allElements       = new StringBuilder()
    private int               splitCounter      = 0
    private int               nextSplit         = 70000
    private int               splitNumber       = 0
    private String            packageInfo       = "eu.hansolo.fx"
    private List<String>      layerSelection    = []
    private HashSet<String>   nameSet           = []
    private HashSet<String>   groupNameSet      = []
    private HashSet<String>   cssNameSet        = []


    // ******************** Translate given elements to given language ********
    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE) {
        return translate(FILE_NAME, layerMap, LANGUAGE, WIDTH, HEIGHT, EXPORT_TO_FILE, COMPONENT_TYPE.JCOMPONENT, "", new HashMap<String, FxgVariable>())
    }

    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE, final HashMap<String, FxgVariable> PROPERTIES) {
        return translate(FILE_NAME, layerMap, LANGUAGE, WIDTH, HEIGHT, EXPORT_TO_FILE, COMPONENT_TYPE.JCOMPONENT, "", PROPERTIES)
    }

    String translate(final String FILE_NAME, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final String WIDTH, final String HEIGHT, final boolean EXPORT_TO_FILE, final COMPONENT_TYPE TYPE, final String NAME_PREFIX, final HashMap<String, FxgVariable> PROPERTIES) {
        final String CLASS_NAME = (FILE_NAME.contains(".") ? (FILE_NAME.substring(0, FILE_NAME.lastIndexOf('.')) + NAME_PREFIX) : (FILE_NAME + NAME_PREFIX)).capitalize()
        final String USER_HOME = System.properties.getProperty('user.home')
        StringBuilder desktopPath = new StringBuilder(USER_HOME).append(File.separator).append('Desktop').append(File.separator)
        StringBuilder exportFileName = new StringBuilder(desktopPath).append(CLASS_NAME)
        if (layerSelection.isEmpty()) {
            layerSelection.addAll(layerMap.keySet())
        }

        splitCounter = 0
        nextSplit = 70000

        StringBuilder codeToExport = new StringBuilder()

        // Export the header of the language specific template
        switch(LANGUAGE) {
            case Language.JAVAFX:
                if (EXPORT_TO_FILE) {
                    String path = new StringBuilder(USER_HOME).append(File.separator).append('Desktop').append(File.separator).toString()
                    writeToFile(desktopPath.append('Demo.java').toString(), javaFxDemoTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                    writeToFile(path + ("${CLASS_NAME}.java").toString(), javaFxControlTemplate(CLASS_NAME, Double.parseDouble(WIDTH), Double.parseDouble(HEIGHT), PROPERTIES))
                    writeToFile(path + ("${CLASS_NAME}Behavior.java").toString(), javaFxBehaviorTemplate(CLASS_NAME))
                    writeToFile(path + ("${CLASS_NAME.toLowerCase()}.css").toString(), makeCssNicer(javaFxCssTemplate(CLASS_NAME, layerMap, PROPERTIES)))
                    writeToFile(path + ("${CLASS_NAME}Builder.java").toString(), javaFxBuilderTemplate(CLASS_NAME, PROPERTIES))
                }
                codeToExport.append(javaFxSkinTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE, PROPERTIES))
                exportFileName.append('Skin.java')
                break
            case Language.CANVAS:
                if (EXPORT_TO_FILE) {
                    writeToFile(exportFileName + '.html', htmlTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", "")))
                }
                codeToExport.append(canvasTemplate(CLASS_NAME, WIDTH.replace(".0", ""), HEIGHT.replace(".0", ""), layerMap, LANGUAGE))
                exportFileName.append(".js")
                break
            default:
                throw Exception
        }
        if (EXPORT_TO_FILE) {
            writeToFile(exportFileName.toString(), makeNicer(codeToExport, LANGUAGE))
        }
        return codeToExport.toString()
    }

    String getDrawingCode(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        String result = createCode(layerMap, LANGUAGE)
        return result
    }

    void setPackageInfo(final String PACKAGE_INFO) {
        packageInfo = PACKAGE_INFO.isEmpty() ? "eu.hansolo.fx" : PACKAGE_INFO;
    }

    void setLayerSelection(List<String> selectedLayers) {
        layerSelection.clear()
        layerSelection.addAll(selectedLayers)
    }

    private String createCssName(final String LAYER_NAME, final String ELEMENT_NAME) {
        StringBuilder cssName = new StringBuilder()
        cssName.append(createName(LAYER_NAME.toLowerCase())).append("-").append(createName(ELEMENT_NAME.toLowerCase().replaceAll("_", '-')))
        return cssName.toString()
    }

    private String createVarName(final String LAYER_NAME, final String ELEMENT_NAME) {
        StringBuilder varName = new StringBuilder()
        varName.append(createName(LAYER_NAME)).append(createName(ELEMENT_NAME).capitalize())
        return varName.toString()
    }

    private String createName(final String NAME) {
        String varName
        String tmpName = NAME.replaceFirst("E_", '')
        tmpName.replaceFirst("_?RR[0-9]+_([0-9]+_)?", '')
        if (tmpName.contains("_")) {
            String[] varNameParts = tmpName.toLowerCase().split("_")
            StringBuilder output = new StringBuilder()
            varNameParts.each {output.append(it.capitalize())}
            varName = output.substring(0,1).toLowerCase() + output.substring(1)
        } else if (tmpName == tmpName.toUpperCase()) {
            varName = NAME.toLowerCase()
        } else if (tmpName == tmpName.capitalize()) {
            varName = "${tmpName.charAt(0).toLowerCase()}${tmpName.substring(1)}"
        } else {
            varName = tmpName
        }
        return varName
    }


    // ******************** JAVA FX *******************************************
    private String javaFxSkinTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_skin.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        int maxLength = 11
        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                maxLength = Math.max(createName(layerName).length(), maxLength)
            }
        }

        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$varDeclaration", javaFxVariableDeclaration(layerMap).toString())
        replaceAll(codeToExport, "\$varInitialization", javaFxVariableInitialization(layerMap).toString())
        replaceAll(codeToExport, "\$registerListeners", javaFxRegisterListeners(PROPERTIES))
        replaceAll(codeToExport, "\$handlePropertyChanges", javaFxHandlePropertyChanges(PROPERTIES))
        replaceAll(codeToExport, "\$canvasMethods", createCode(layerMap, LANGUAGE))
        replaceAll(codeToExport, "\$resizeRegions", createResizingCode(layerMap, LANGUAGE))
        replaceAll(allLayers, "_Canvas", "")
        if (allLayers.length() > 31) {
            allLayers.replace(allLayers.length() - 31, allLayers.length(), "")
        }
        replaceAll(codeToExport, "\$layerList", allLayers.toString())

        return codeToExport.toString()
    }

    private String javaFxControlTemplate(final String CLASS_NAME, final double WIDTH, final double HEIGHT, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_control.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$propertyDeclaration", javaFxPropertyDeclaration(PROPERTIES))
        replaceAll(codeToExport, "\$propertyInitialization", javaFxPropertyInitialization(PROPERTIES))
        replaceAll(codeToExport, "\$propertyGetterSetter", javaFxPropertyGetterSetter(PROPERTIES))
        replaceAll(codeToExport, "\$prefSizeCalculation", javaFxPrefSizeCalculation(WIDTH, HEIGHT))
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$styleClass", CLASS_NAME.toLowerCase())
        return codeToExport.toString()
    }

    private String javaFxBuilderTemplate(final String CLASS_NAME, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_builder.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$propertySetter", javaFxPropertySetter(CLASS_NAME, PROPERTIES))
        replaceAll(codeToExport, "\$buildMethod", javaFxBuildMethod(CLASS_NAME, PROPERTIES))
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        return codeToExport.toString()
    }

    private StringBuilder javaFxCssTemplate(final String CLASS_NAME, Map<String, List<FxgElement>> layerMap, final HashMap<String, FxgVariable> PROPERTIES) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_css.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", packageInfo)
        replaceAll(codeToExport, "\$styleClass", CLASS_NAME.toLowerCase())
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$fillAndStrokeDefinitions", cssCode(CLASS_NAME, layerMap))
        replaceAll(codeToExport, "\$colorDefinitions", cssColors(PROPERTIES))
        return codeToExport
    }

    private String javaFxBehaviorTemplate(final String CLASS_NAME) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_behavior.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        return codeToExport.toString()
    }

    private String javaFxDemoTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/javafx_demo.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)
        replaceAll(codeToExport, "\$packageInfo", "package " + packageInfo + ";")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)
        return codeToExport.toString()
    }

    private String javaFxCanvasLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        String layerName = LAYER_NAME.replaceAll(CANVAS_PATTERN, "")
        layerCode.append("\n")
        layerCode.append("    public final Canvas draw${layerName.capitalize()}(final double WIDTH, final double HEIGHT) {\n")
        layerCode.append("        final Canvas          CANVAS = new Canvas(WIDTH, HEIGHT);\n")
        layerCode.append("        final GraphicsContext CTX    = CANVAS.getGraphicsContext2D();\n");
        return layerCode.toString()
    }

    private String javaFxCanvasLayerMethodStop() {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("\n")
        layerCode.append("        return CANVAS;\n")
        layerCode.append("    }\n")
        return layerCode.toString()
    }

    private StringBuilder javaFxVariableDeclaration(final HashMap<String, List<FxgElement>> layerMap) {
        StringBuilder regionDeclaration    = new StringBuilder()
        int effectCounter = 0
        String lastEffectName
        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties") && !layerName.toLowerCase().endsWith("canvas")) {
                layerMap[layerName].each {FxgElement element ->
                    String varName = createVarName(layerName, element.shape.shapeName)
                    if(element.getShape().getClass().equals(FxgRichText.class)) {
                        regionDeclaration.append("\n    private Text         ${varName};")
                    } else {
                        regionDeclaration.append("\n    private Region       ${varName};")
                    }
                    if (!element.shape.effects.isEmpty() && element.shape.effects.size() > 1) {
                        element.shape.effects.each { Effect effect ->
                            if (effect.class.equals(InnerShadow.class)) {
                                regionDeclaration.append("\n    private InnerShadow  ${varName}InnerShadow${effectCounter}").append(";")
                                lastEffectName = "${varName}InnerShadow${effectCounter}"
                                effectCounter++
                            } else if (effect.class.equals(DropShadow.class)) {
                                regionDeclaration.append("\n    private DropShadow   ${varName}DropShadow${effectCounter}").append(";")
                                lastEffectName = "${varName}DropShadow${effectCounter}"
                                effectCounter++
                            }
                        }
                    }
                }
            }
        }
        return regionDeclaration
    }

    private StringBuilder javaFxVariableInitialization(final HashMap<String, List<FxgElement>> layerMap) {
        StringBuilder regionInitialization = new StringBuilder()
        List<String>  regionsToAdd         = new ArrayList<>()
        layerMap.keySet().each {String layerName ->
            int effectCounter = 0
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties") && !layerName.toLowerCase().endsWith("canvas")) {
                int shapeIndex = 0
                layerMap[layerName].each {FxgElement element ->
                    shapeIndex += 1
                    String varName = createVarName(layerName, element.shape.shapeName)
                    String cssName = createCssName(layerName, element.shape.shapeName)
                    if(element.getShape().getClass().equals(FxgRichText.class)) {
                        regionInitialization.append("\n        ${varName}").append(" = new Text(\"${element.shape.text}\");")
                    } else {
                        regionInitialization.append("\n        ${varName}").append(" = new Region();")
                    }
                    regionInitialization.append("\n        ${varName}.getStyleClass().setAll(\"${cssName}\");\n\n")
                    // Add chained effects if the FxgShape contains more than one effect
                    if (!element.shape.effects.isEmpty() && element.shape.effects.size() > 1) {
                        String lastEffectName
                        element.shape.effects.each { Effect effect ->
                            if (effect.class.equals(InnerShadow.class)) {
                                regionInitialization.append("\n        ${varName}InnerShadow${effectCounter}").append(" = InnerShadowBuilder.create()")
                                regionInitialization.append("\n            .offsetX(${((InnerShadow) effect).offsetX})")
                                regionInitialization.append("\n            .offsetY(${((InnerShadow) effect).offsetY})")
                                regionInitialization.append("\n            .radius(${((InnerShadow) effect).radius} / ${element.shape.referenceWidth} * DEFAULT_WIDTH)")
                                regionInitialization.append("\n            .color(Color.web(\"${((InnerShadow) effect).color}\"))")
                                regionInitialization.append("\n            .blurType(BlurType.${((InnerShadow) effect).blurType})")
                                if (!effect.equals(element.shape.effects.first())) {
                                    regionInitialization.append("\n            .input(${lastEffectName})")
                                }
                                regionInitialization.append("\n            .build();")
                                lastEffectName = "${varName}InnerShadow${effectCounter}"
                                effectCounter++
                            } else if (effect.class.equals(DropShadow.class)) {
                                regionInitialization.append("\n        ${varName}DropShadow${effectCounter}").append(" = DropShadowBuilder.create()")
                                regionInitialization.append("\n            .offsetX(${((DropShadow) effect).offsetX})")
                                regionInitialization.append("\n            .offsetY(${((DropShadow) effect).offsetY})")
                                regionInitialization.append("\n            .radius(${((DropShadow) effect).radius} / ${element.shape.referenceWidth} * DEFAULT_WIDTH)")
                                regionInitialization.append("\n            .color(Color.web(\"${((DropShadow) effect).color}\"))")
                                regionInitialization.append("\n            .blurType(BlurType.${((DropShadow) effect).blurType})")
                                if (!effect.equals(element.shape.effects.first())) {
                                    regionInitialization.append("\n            .input(${lastEffectName})")
                                }
                                regionInitialization.append("\n            .build();")
                                lastEffectName = "${varName}DropShadow${effectCounter}"
                                effectCounter++
                            }
                        }
                        regionInitialization.append("\n        ${varName}.setEffect(${lastEffectName});\n")
                    }
                    regionsToAdd.add(varName)
                }
            }
        }
        regionInitialization.append("        pane.getChildren().setAll(")
        for (int i = 0 ; i < regionsToAdd.size() ; i++) {
            if (i > 0) {
                regionInitialization.append("                                  ")
            }
            regionInitialization.append(regionsToAdd[i])
            if (i < regionsToAdd.size() - 1) {
                regionInitialization.append(",\n")
            } else {
                regionInitialization.append(");\n")
            }
        }
        return regionInitialization
    }

    private String javaFxPropertyDeclaration(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        int maxLength = -1
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (!TYPE.equals("double") && !TYPE.equals("boolean") && !TYPE.equals("int") &&
                !TYPE.equals("long") && !TYPE.equals("string") && !TYPE.equals("object")) {
                maxLength = Math.max(TYPE.length(), maxLength)
            }
        }

        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("    private DoubleProperty ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("    private BooleanProperty")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("    private IntegerProperty")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("    private LongProperty   ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("    private StringProperty ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("    private ObjectProperty ")
                appendBlanks(PROPERTY_CODE, (maxLength + 2))
                PROPERTY_CODE.append(PROPERTY_NAME).append(";\n")
            } else {
                PROPERTY_CODE.append("    private ObjectProperty<${PROPERTIES.get(PROPERTY_NAME).type}> ").append(PROPERTY_NAME).append(";\n")
            }
        }
        PROPERTY_CODE.append("    private boolean        ")
        appendBlanks(PROPERTY_CODE, (maxLength + 2))
        PROPERTY_CODE.append("square;\n")
        PROPERTY_CODE.append("    private long           ")
        appendBlanks(PROPERTY_CODE, (maxLength + 2))
        PROPERTY_CODE.append("interval;\n")
        PROPERTY_CODE.append("    private long           ")
        appendBlanks(PROPERTY_CODE, (maxLength + 2))
        PROPERTY_CODE.append("lastTimerCall;\n")
        PROPERTY_CODE.append("    private AnimationTimer ")
        appendBlanks(PROPERTY_CODE, (maxLength + 2))
        PROPERTY_CODE.append("timer;\n")

        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertyInitialization(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        int maxLength = 13
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            maxLength = Math.max(PROPERTY_NAME.length(), maxLength)
        }

        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleDoubleProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleBooleanProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleIntegerProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleLongProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleStringProperty(\"${PROPERTIES.get(PROPERTY_NAME).defaultValue}\");\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                PROPERTY_CODE.append(" = new SimpleObjectProperty(${PROPERTIES.get(PROPERTY_NAME).defaultValue});\n")
            } else {
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME)
                appendBlanks(PROPERTY_CODE, (maxLength - PROPERTY_NAME.length()))
                String defaultValue
                if (TYPE.equals("color")) {
                    defaultValue = "Color.web(\"#${PROPERTIES.get(PROPERTY_NAME).defaultValue}\")"
                } else {
                    defaultValue = PROPERTIES.get(PROPERTY_NAME).defaultValue
                }
                PROPERTY_CODE.append(" = new SimpleObjectProperty<${PROPERTIES.get(PROPERTY_NAME).type}>(${defaultValue});\n")
            }
        }
        PROPERTY_CODE.append("        square")
        int spacer = maxLength == 0 ? 0 : 6;
        appendBlanks(PROPERTY_CODE, (maxLength - spacer))
        PROPERTY_CODE.append(" = false;\n")
        PROPERTY_CODE.append("        interval")
        spacer = maxLength == 0 ? 0 : 8;
        appendBlanks(PROPERTY_CODE, (maxLength - spacer))
        PROPERTY_CODE.append(" = 1_000_000_000;\n")
        PROPERTY_CODE.append("        lastTimerCall")
        spacer = maxLength == 0 ? 0 : 13;
        appendBlanks(PROPERTY_CODE, (maxLength - spacer))
        PROPERTY_CODE.append(" = System.nanoTime();\n")
        PROPERTY_CODE.append("        timer")
        spacer = maxLength == 0 ? 0 : 5;
        appendBlanks(PROPERTY_CODE, (maxLength - spacer))
        PROPERTY_CODE.append(" = new AnimationTimer() {\n" +
                "            @Override public void handle(final long NOW) {\n" +
                "                if (NOW > lastTimerCall + interval) {\n" +
                "                    lastTimerCall = NOW;\n" +
                "                }\n" +
                "            }\n" +
                "        };\n")

        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertyGetterSetter(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("\n    ").append("public final double get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final double ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final DoubleProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("\n    ").append("public final boolean is").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final boolean ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final BooleanProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("\n    ").append("public final int get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final int ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final IntegerProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("\n    ").append("public final long get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final long ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final LongProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("\n    ").append("public final String get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final String ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final StringProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("\n    ").append("public final Object get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final Object ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final ObjectProperty ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n")
            } else {
                final String ORIGINAL_TYPE = PROPERTIES.get(PROPERTY_NAME).type
                PROPERTY_CODE.append("\n    ").append("public final ${ORIGINAL_TYPE} get").append(PROPERTY_NAME.capitalize()).append("() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(".get();\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final void set").append(PROPERTY_NAME.capitalize()).append("(final ${ORIGINAL_TYPE} ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append(PROPERTY_NAME).append(".set(").append(PROPERTY_NAME.toUpperCase()).append(");\n")
                PROPERTY_CODE.append("    }\n\n")
                PROPERTY_CODE.append("    ").append("public final ObjectProperty<${ORIGINAL_TYPE}> ").append(PROPERTY_NAME).append("Property() {\n")
                PROPERTY_CODE.append("        return ").append(PROPERTY_NAME).append(";\n")
                PROPERTY_CODE.append("    }\n")
            }
        }

        return PROPERTY_CODE.toString()
    }

    private String javaFxPropertySetter(final String CLASS_NAME, final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
            if (TYPE.equals("double")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final double ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleDoubleProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("boolean")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final boolean ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleBooleanProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("int")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final int ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleIntegerProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("long")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final long ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleLongProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("string")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final String ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleStringProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else if (TYPE.equals("object")) {
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final Object ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleObjectProperty(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            } else {
                final String ORIGINAL_TYPE = PROPERTIES.get(PROPERTY_NAME).type
                PROPERTY_CODE.append("    ").append("public final ").append(CLASS_NAME).append("Builder ").append(PROPERTY_NAME).append("(final ${ORIGINAL_TYPE} ").append(PROPERTY_NAME.toUpperCase()).append(") {\n")
                PROPERTY_CODE.append("        ").append("properties.put(\"").append(PROPERTY_NAME).append("\", new SimpleObjectProperty<${ORIGINAL_TYPE}>(").append(PROPERTY_NAME.toUpperCase()).append("));\n")
                PROPERTY_CODE.append("        return this;\n")
                PROPERTY_CODE.append("    }\n\n")
            }
        }
        return PROPERTY_CODE.toString()
    }

    private String javaFxBuildMethod(final String CLASS_NAME, final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder BUILD_CODE = new StringBuilder()
        BUILD_CODE.append("        for (String key : properties.keySet()) {\n")
        boolean first = true
        if (PROPERTIES.keySet().isEmpty()) {
            BUILD_CODE.append("            if (\"\".equals(key)) {\n\n")
        } else {
            PROPERTIES.keySet().each{String PROPERTY_NAME->
                final String TYPE = PROPERTIES.get(PROPERTY_NAME).type.toLowerCase()
                if (first) {
                    BUILD_CODE.append("            if (")
                } else {
                    BUILD_CODE.append("            } else if(")
                }
                BUILD_CODE.append("\"").append(PROPERTY_NAME).append("\".equals(key)) {\n")
                if (TYPE.equals("double")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((DoubleProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("boolean")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((BooleanProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("int")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((IntegerProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("long")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((LongProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("string")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((StringProperty) properties.get(key)).get());\n")
                } else if (TYPE.equals("object")) {
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((ObjectProperty) properties.get(key)).get());\n")
                } else {
                    final String ORIGINAL_TYPE = PROPERTIES.get(PROPERTY_NAME).type
                    BUILD_CODE.append("                CONTROL.set").append(PROPERTY_NAME.capitalize()).append("(((ObjectProperty<${ORIGINAL_TYPE}>) properties.get(key)).get());\n")
                }
                first = false
            }
        }
        BUILD_CODE.append("            } else if(\"prefWidth\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setPrefWidth(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            } else if(\"prefHeight\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setPrefHeight(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            } else if (\"layoutX\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setLayoutX(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            } else if (\"layoutY\".equals(key)) {\n")
        BUILD_CODE.append("                CONTROL.setLayoutY(((DoubleProperty) properties.get(key)).get());\n")
        BUILD_CODE.append("            }\n")
        BUILD_CODE.append("        }\n")
        return BUILD_CODE.toString()
    }

    private String javaFxPrefSizeCalculation(final double WIDTH, final double HEIGHT) {
        StringBuilder PREF_SIZE_CODE = new StringBuilder()
        PREF_SIZE_CODE.append("        double prefHeight = WIDTH < (HEIGHT * ${WIDTH / HEIGHT}) ? (WIDTH * ${HEIGHT / WIDTH}) : HEIGHT;\n")
        PREF_SIZE_CODE.append("        double prefWidth = prefHeight * ${WIDTH / HEIGHT};\n")
        return PREF_SIZE_CODE.toString()
    }

    private String javaFxRegisterListeners(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            PROPERTY_CODE.append("        ").append("registerChangeListener(control.")
            PROPERTY_CODE.append("${PROPERTY_NAME}Property(), \"${PROPERTY_NAME.toUpperCase()}\");\n")
        }
        return PROPERTY_CODE.toString()
    }

    private String javaFxHandlePropertyChanges(final HashMap<String, FxgVariable> PROPERTIES) {
        StringBuilder PROPERTY_CODE = new StringBuilder()
        PROPERTIES.keySet().eachWithIndex() {String PROPERTY_NAME, int index->
            PROPERTY_CODE.append(" else if (").append("\"${PROPERTY_NAME.toUpperCase()}\".equals(PROPERTY)) {\n")
            PROPERTY_CODE.append("            // React to ${PROPERTY_NAME} property change here\n")
            PROPERTY_CODE.append("        }")
            if (index == PROPERTIES.size()) {
                PROPERTY_CODE.append(";")
            }
        }
        return PROPERTY_CODE.toString()
    }


    // ******************** CANVAS ********************************************
    private String canvasTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT, Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/canvas.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        StringBuilder createBuffers = new StringBuilder()
        StringBuilder drawImagesToBuffer = new StringBuilder()
        StringBuilder drawImagesToCanvas = new StringBuilder()
        StringBuilder clearBuffers = new StringBuilder()
        layerMap.keySet().each {String layerName ->
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                createBuffers.append("    var ${layerName}Buffer = document.createElement('canvas');\n")
                createBuffers.append("    ${layerName}Buffer.width = imageWidth;\n")
                createBuffers.append("    ${layerName}Buffer.height = imageHeight;\n")
                createBuffers.append("    var ${layerName}Ctx = ${layerName}Buffer.getContext('2d');\n")
                drawImagesToBuffer.append("        draw${layerName}Image(${layerName}Ctx);\n")
                drawImagesToCanvas.append("        mainCtx.drawImage(${layerName}Buffer, 0, 0);\n")
                clearBuffers.append("        ${layerName}Ctx.clearRect(0, 0, this.width, this.height);\n")
            }
        }

        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$createBuffers", createBuffers.toString())
        replaceAll(codeToExport, "\$drawImagesToBuffer", drawImagesToBuffer.toString())
        replaceAll(codeToExport, "\$drawImagesToCanvas", drawImagesToCanvas.toString())
        replaceAll(codeToExport, "\$creationMethods", createCode(layerMap, LANGUAGE))
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)
        replaceAll(codeToExport, "\$clearBuffers", clearBuffers.toString())

        return codeToExport.toString()
    }

    private String canvasLayerMethodStart(final String LAYER_NAME) {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("    var draw${LAYER_NAME}Image = function(ctx) {\n")
        layerCode.append("        ctx.save();\n\n")
        return layerCode.toString()
    }

    private String canvasLayerMethodStop() {
        StringBuilder layerCode = new StringBuilder()
        layerCode.append("        ctx.restore();\n")
        layerCode.append("    }\n\n")
        return layerCode.toString()
    }

    private String htmlTemplate(final String CLASS_NAME, final String WIDTH, final String HEIGHT) {
        def template = getClass().getResourceAsStream('/eu/hansolo/fxgtools/resources/html.txt')
        StringBuilder codeToExport = new StringBuilder(template.text)

        replaceAll(codeToExport, "\$jsFileName", CLASS_NAME + ".js")
        replaceAll(codeToExport, "\$className", CLASS_NAME)
        replaceAll(codeToExport, "\$width", WIDTH)
        replaceAll(codeToExport, "\$height", HEIGHT)

        return codeToExport.toString()
    }


    // ******************** CREATE CODE ***************************************
    private String createCode(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        allLayers.length = 0
        allElements.length = 0
        layerMap.keySet().each { String layerName->
            groupNameSet.clear()
            nameSet.clear();
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                splitNumber = 0
                int shapeIndex = 0

                switch(LANGUAGE) {
                    case Language.JAVAFX       :
                        if (layerName.toLowerCase().endsWith("canvas")) {
                            code.append(javaFxCanvasLayerMethodStart(layerName))
                        }
                        break
                    case Language.CANVAS       :
                        code.append(canvasLayerMethodStart(layerName))
                        break
                }

                // main translation routine
                layerMap[layerName].each { FxgElement element ->
                    shapeIndex += 1
                    if (layerName.toLowerCase().endsWith("canvas")) {
                        code.append(element.shape.translateTo(Language.JAVAFX_CANVAS, shapeIndex, nameSet))
                    }
                }

                // add language dependend method end
                switch(LANGUAGE) {
                    case Language.JAVAFX:
                        if (layerName.toLowerCase().endsWith("canvas")) {
                            code.append(javaFxCanvasLayerMethodStop())
                            allLayers.append("draw").append(layerName.capitalize()).append("(control.getPrefWidth(), control.getPrefHeight())").append(",\n                             ")
                        }
                        break
                    case Language.CANVAS:
                        code.append(canvasLayerMethodStop())
                        break
                }
            }
        }
        return code.toString()
    }

    private String createResizingCode(Map<String, List<FxgElement>> layerMap, final Language LANGUAGE) {
        StringBuilder code = new StringBuilder()
        allLayers.length   = 0
        allElements.length = 0
        String varName
        layerMap.keySet().each { String layerName->
            groupNameSet.clear()
            nameSet.clear();
            if (layerSelection.contains(layerName) && !layerName.toLowerCase().startsWith("properties")) {
                // Resizing routine
                layerMap[layerName].each { FxgElement element ->
                    int    effectCounter = 0
                    String lastEffectName
                    varName = createVarName(layerName, element.shape.shapeName)
                    if (!layerName.toLowerCase().endsWith("canvas")) {
                        if (LANGUAGE == Language.JAVAFX){
                            code.append("\n        ").append(varName).append(".setPrefSize(${element.getShape().elementWidth / element.getShape().referenceWidth} * width, ${element.getShape().elementHeight / element.getShape().referenceHeight} * height);")
                            code.append("\n        ").append(varName).append(".setTranslateX(${element.getShape().elementX / element.getShape().referenceWidth} * width);")
                            code.append("\n        ").append(varName).append(".setTranslateY(${element.getShape().elementY / element.getShape().referenceHeight} * height);")
                            if (!element.shape.effects.isEmpty() && element.shape.effects.size() > 1) {
                                element.shape.effects.each { Effect effect ->
                                    if (effect.class.equals(InnerShadow.class)) {
                                        code.append("\n        ${varName}InnerShadow${effectCounter}").append(".setRadius(${((InnerShadow) effect).radius} / ${element.shape.referenceWidth} * size);")
                                        effectCounter++
                                    } else if (effect.class.equals(DropShadow.class)) {
                                        code.append("\n        ${varName}DropShadow${effectCounter}").append(".setRadius(${((DropShadow) effect).radius} / ${element.shape.referenceWidth} * size);")
                                        effectCounter++
                                    }
                                }
                            }
                        }
                    }
                    code.append("\n")
                }
            }
        }
        return code.toString()
    }

    private String cssCode(String CLASS_NAME, Map<String, List<FxgElement>> layerMap) {
        StringBuilder cssCode = new StringBuilder()
        layerMap.keySet().each {String layerName->
            cssNameSet.clear();
            if (layerSelection.contains(layerName) && !layerName.equalsIgnoreCase("properties")) {
                int shapeIndex = 0
                layerMap[layerName].each {FxgElement element ->
                    shapeIndex += 1

                    String cssName = "${element.shape.shapeName}"
                    cssName = cssName.replaceAll("E_", '')
                    cssName = cssName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '')
                    cssName = "${CLASS_NAME} .${layerName.toLowerCase()}-${cssName.toLowerCase()}"

                    if (cssNameSet.contains(cssName)) {
                        cssName = "${CLASS_NAME} .${layerName.toLowerCase()}-${element.shape.shapeName.toLowerCase()}-${shapeIndex}"
                    } else {
                        cssNameSet.add(cssName)
                    }
                    cssCode.append(element.shape.createCssFillAndStroke(cssName, element.shape.filled, element.shape.stroked))
                }
            }
        }
        return cssCode.toString()
    }

    private String cssColors(final HashMap<String, FxgVariable> PROPERTIES) {
        final StringBuilder CSS_COLORS = new StringBuilder()
        PROPERTIES.keySet().each{String PROPERTY_NAME->
            if (PROPERTIES.get(PROPERTY_NAME).type.toLowerCase().equals("color")) {
                CSS_COLORS.append("    -fx-")
                          .append(PROPERTY_NAME.toLowerCase())
                          .append(": #")
                          .append(PROPERTIES.get(PROPERTY_NAME).defaultValue)
                          .append(";\n")
            }
        }
        return CSS_COLORS.toString()
    }


    // ******************** OUTPUT ********************************************
    private void writeToFile(final String FILE_NAME, String codeToExport) {
        new File("$FILE_NAME").withWriter { out ->
            out.println codeToExport
    }
}


    // ******************** OPTIMIZE OUTPUT ***********************************
    private String makeNicer(final StringBuilder CODE, final Language LANGUAGE) {
        /* TEMPLATE ENGINE
        String template = '${width}_${height}'
        def width = 200
        def height = 1000

        def engine = new SimpleTemplateEngine()
        def binding = [width:"${width}", height:"${height}"]
        def result = engine.createTemplate(template).make(binding) // in the template, replace values of one and two from the binding

        println result.toString()

        */

        switch (LANGUAGE) {
            case Language.JAVAFX:
                replaceAll(CODE, "0.0 * WIDTH", "0.0")
                replaceAll(CODE, "0.0 * HEIGHT", "0.0")
                replaceAll(CODE, "0.0 * SIZE", "0.0")
                replaceAll(CODE, "1.0 * WIDTH", "WIDTH")
                replaceAll(CODE, "1.0 * HEIGHT", "HEIGHT")
                replaceAll(CODE, "1.0 * SIZE", "SIZE")
                replaceAll(CODE, "Color.color(0, 0, 0, 1)", "Color.BLACK")
                replaceAll(CODE, "Color.color(0.0, 0.0, 0.0, 1)", "Color.BLACK")
                replaceAll(CODE, "Color.color(0.0, 0.0, 0.0, 1.0)", "Color.BLACK")
                replaceAll(CODE, "Color.color(1, 1, 1, 1)", "Color.WHITE")
                replaceAll(CODE, "Color.color(1.0, 1.0, 1.0, 1)", "Color.WHITE")
                replaceAll(CODE, "Color.color(1.0, 1.0, 1.0, 1.0)", "Color.WHITE")
                replaceAll(CODE, "Color.color(1, 0, 0, 1)", "Color.RED")
                replaceAll(CODE, "Color.color(1.0, 0.0, 0.0, 1)", "Color.RED")
                replaceAll(CODE, "Color.color(1.0, 0.0, 0.0, 1.0)", "Color.RED")
                replaceAll(CODE, "Color.color(0, 1, 0, 1)", "Color.LIME")
                replaceAll(CODE, "Color.color(0.0, 1.0, 0.0, 1)", "Color.LIME")
                replaceAll(CODE, "Color.color(0.0, 1.0, 0.0, 1.0)", "Color.LIME")
                replaceAll(CODE, "Color.color(0, 0, 1, 1)", "Color.BLUE")
                replaceAll(CODE, "Color.color(0.0, 0.0, 1.0, 1)", "Color.BLUE")
                replaceAll(CODE, "Color.color(0.0, 0.0, 1.0, 1.0)", "Color.BLUE")
                replaceAll(CODE, "Color.color(1, 1, 0, 1)", "Color.YELLOW")
                replaceAll(CODE, "Color.color(1.0, 1.0, 0.0, 1)", "Color.YELLOW")
                replaceAll(CODE, "Color.color(1.0, 1.0, 0.0, 1.0)", "Color.YELLOW")
                replaceAll(CODE, "Color.color(0, 1, 1, 1)", "Color.CYAN")
                replaceAll(CODE, "Color.color(0.0, 1.0, 1.0, 1)", "Color.CYAN")
                replaceAll(CODE, "Color.color(0.0, 1.0, 1.0, 1.0)", "Color.CYAN")
                replaceAll(CODE, "Color.color(1, 0, 1, 1)", "Color.MAGENTA")
                replaceAll(CODE, "Color.color(1.0, 0.0, 1.0, 1)", "Color.MAGENTA")
                replaceAll(CODE, "Color.color(1.0, 0.0, 1.0, 1.0)", "Color.MAGENTA")
                break
        }
        final Pattern ZERO_PATTERN = Pattern.compile(/(0{8}[0-9]*)/)
        replaceAll(CODE, ZERO_PATTERN, "0")
        // replace shape name prefixes like E_ and RRn_m_
        replaceAll(CODE, "_E_", "")
        final Pattern PATTERN = Pattern.compile(/_?RR[0-9]+_([0-9]+_)?/)
        replaceAll(CODE, PATTERN, "")

        return CODE.toString()
    }

    private String makeCssNicer(final StringBuilder CSS) {
        replaceAll(CSS, "rgba(255, 255, 255, 0)", "transparent")
        replaceAll(CSS, "rgba(255, 255, 255, 0.0)", "transparent")
        replaceAll(CSS, "rgba(0, 0, 0, 0)", "transparent")
        replaceAll(CSS, "rgba(0, 0, 0, 0.0)", "transparent")
        replaceAll(CSS, "rgba(0, 0, 0, 1)", "black")
        replaceAll(CSS, "rgba(0, 0, 0, 1.0)", "black")
        replaceAll(CSS, "rgb(0, 0, 0)", "black")
        replaceAll(CSS, "rgba(255, 255, 255, 1)", "white")
        replaceAll(CSS, "rgba(255, 255, 255, 1.0)", "white")
        replaceAll(CSS, "rgb(255, 255, 255)", "white")
        replaceAll(CSS, "rgba(255, 0, 0, 1)", "red")
        replaceAll(CSS, "rgba(255, 0, 0, 1.0)", "red")
        replaceAll(CSS, "rgb(255, 0, 0)", "red")
        replaceAll(CSS, "rgba(0, 255, 0, 1)", "lime")
        replaceAll(CSS, "rgba(0, 255, 0, 1.0)", "lime")
        replaceAll(CSS, "rgb(0, 255, 0)", "lime")
        replaceAll(CSS, "rgba(0, 0, 255, 1)", "blue")
        replaceAll(CSS, "rgba(0, 0, 255, 1.0)", "blue")
        replaceAll(CSS, "rgb(0, 0, 255)", "blue")
        replaceAll(CSS, "rgba(255, 255, 0, 1)", "yellow")
        replaceAll(CSS, "rgba(255, 255, 0, 1.0)", "yellow")
        replaceAll(CSS, "rgb(255, 255, 0)", "yellow")
        replaceAll(CSS, "rgba(0, 255, 255, 1)", "cyan")
        replaceAll(CSS, "rgba(0, 255, 255, 1.0)", "cyan")
        replaceAll(CSS, "rgb(0, 255, 255)", "cyan")
        replaceAll(CSS, "rgba(255, 0, 255, 1)", "magenta")
        replaceAll(CSS, "rgba(255, 0, 255, 1.0)", "magenta")
        replaceAll(CSS, "rgb(255, 0, 255)", "magenta")

        return CSS.toString()
    }


    // ******************** REPLACEMENT METHODS *******************************
    private static void replaceAll(final StringBuilder TEXT, final String SEARCH, final String REPLACE) {
        int index = TEXT.indexOf(SEARCH)
        while (index != -1) {
            TEXT.replace(index, index + SEARCH.length(), REPLACE)
            index += REPLACE.length()
            index = TEXT.indexOf(SEARCH, index)
        }
    }

    private static void replaceAll(final StringBuilder TEXT, final Pattern PATTERN, final String REPLACE) {
        final Matcher MATCHER = PATTERN.matcher(TEXT)
        while (MATCHER.find()) {
            TEXT.replace(MATCHER.start(), MATCHER.end(), REPLACE)
            MATCHER.reset()
        }
    }

    private static void appendBlanks(final StringBuilder TEXT, final int NO_TO_APPEND) {
        for (int i = 0 ; i < NO_TO_APPEND ; i++) {
            TEXT.append(" ")
        }
    }
}
