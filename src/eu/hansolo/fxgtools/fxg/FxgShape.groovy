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
package eu.hansolo.fxgtools.fxg

import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.effect.InnerShadow
import javafx.scene.paint.Color
import javafx.scene.paint.Stop
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.transform.Transform
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight


/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 06:46
 * To change this template use File | Settings | File Templates.
 */
abstract class FxgShape {
    HashSet<String> importSet       = []
    String          layerName
    String          shapeName
    FxgShapeType    type
    FxgFill         fill
    FxgStroke       stroke
    List<FxgFilter> filters         = []
    List<Effect>    effects         = []
    boolean         filled
    boolean         stroked
    boolean         transformed
    double          referenceWidth
    double          referenceHeight
    double          elementX
    double          elementY
    double          elementWidth
    double          elementHeight
    Transform       transform
    String          cssShape

    abstract String translateTo(final Language LANGUAGE, final int SHAPE_INDEX, final HashSet<String> NAME_SET)


    // JAVA_FX
    protected void appendJavaFxFillAndStroke(StringBuilder code, String elementName) {
        if (filled) {
            appendJavaFxPaint(code, elementName)
        }
        if (stroked) {
            /*
            if (stroke.stroke.lineWidth < 2) {
                code.append("        ${elementName}.setStrokeType(StrokeType.OUTSIDE);\n")
            } else {
                code.append("        ${elementName}.setStrokeType(StrokeType.CENTERED);\n")
            }
            */
            importSet.add("import javafx.scene.shape.StrokeType;")
            importSet.add("import javafx.scene.shape.StrokeLineCap;")
            importSet.add("import javafx.scene.shape.StrokeLineJoin;")
            code.append("        ${elementName}.setStrokeType(StrokeType.CENTERED);\n")
            switch (stroke.cap) {
                case StrokeLineCap.BUTT:
                    code.append("        ${elementName}.setStrokeLineCap(StrokeLineCap.BUTT);\n")
                    break
                case StrokeLineCap.ROUND:
                    code.append("        ${elementName}.setStrokeLineCap(StrokeLineCap.ROUND);\n")
                    break
                case StrokeLineCap.SQUARE:
                    code.append("        ${elementName}.setStrokeLineCap(StrokeLineCap.SQUARE);\n")
                    break
            }
            switch (stroke.join) {
                case StrokeLineJoin.BEVEL:
                    code.append("        ${elementName}.setStrokeLineJoin(StrokeLineJoin.BEVEL);\n")
                    break
                case StrokeLineJoin.ROUND:
                    code.append("        ${elementName}.setStrokeLineJoin(StrokeLineJoin.ROUND);\n")
                    break
                case StrokeLineJoin.MITER:
                    code.append("        ${elementName}.setStrokeLineJoin(StrokeLineJoin.MITER);\n")
                    break
            }
            code.append("        ${elementName}.setStrokeWidth(${stroke.width / referenceWidth} * WIDTH);\n")
            code.append("        ${elementName}.setStroke(")
            appendJavaFxColor(code, stroke.color)
            code.append(");\n")
        } else {
            code.append("        ${elementName}.setStroke(null);\n")
        }
    }

    protected void appendJavaFxPaint(StringBuilder code, String elementName) {
        elementName = elementName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
        elementName = elementName.replace("_E_", "_")
        int nameLength = elementName.length()

        importSet.add("import javafx.scene.shape.Shape;")

        // add call to css id
        code.append("        //${elementName}.getStyleClass().add(\"${layerName.toLowerCase()}-${elementName.toLowerCase().replaceAll('_', '-')}\");\n")

        code.append("        final Paint ${elementName}_FILL = ")

        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                importSet.add("import javafx.scene.paint.Paint;")
                importSet.add("import javafx.scene.paint.Color;")
                appendJavaFxColor(code, fill.color)
                code.append(";\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                importSet.add("import javafx.scene.paint.Paint;")
                importSet.add("import javafx.scene.paint.Color;")
                importSet.add("import javafx.scene.paint.LinearGradient;")
                importSet.add("import javafx.scene.paint.CycleMethod;")
                importSet.add("import javafx.scene.paint.Stop;")
                code.append("new LinearGradient(${fill.startX / referenceWidth} * WIDTH, ${fill.startY / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("${fill.endX / referenceWidth} * WIDTH, ${fill.endY / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("false, CycleMethod.NO_CYCLE,\n")
                intendCode(code, 8, nameLength, 39)
                appendJavaFxStops(code, fill.stops, (47 + nameLength))
                code.append(");\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                importSet.add("import javafx.scene.paint.Paint;")
                importSet.add("import javafx.scene.paint.Color;")
                importSet.add("import javafx.scene.paint.LinearGradient;")
                importSet.add("import javafx.scene.paint.CycleMethod;")
                importSet.add("import javafx.scene.paint.Stop;")
                importSet.add("import javafx.scene.paint.RadialGradient;")
                code.append("new RadialGradient(0, 0,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("${fill.centerX / referenceWidth} * WIDTH, ${fill.centerY / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("${fill.radius / referenceWidth} * WIDTH,\n")
                intendCode(code, 8, nameLength, 39)
                code.append("false, CycleMethod.NO_CYCLE,\n")
                intendCode(code, 8, nameLength, 39)
                appendJavaFxStops(code, fill.stops, (47 + nameLength))
                code.append(");\n")
                break
            case FxgFillType.NONE:
                importSet.add("import javafx.scene.paint.Paint;")
                code.append("null;\n")
                break
        }
        code.append("        ${elementName}.setFill(${elementName}_FILL);\n")
    }

    protected void appendJavaFxFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            final double FILTER_WIDTH_FACTOR = 3.6
            final double FILTER_OFFSET_FACTOR = 1.2
            String lastFilterName
            Effect effect
            Effect lastEffect
            double referenceSize = referenceWidth <= referenceHeight ? referenceWidth : referenceHeight
            filters.eachWithIndex { filter, i ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {
                            effect = new InnerShadow()
                            effect.offsetX(filter.getOffset().x / referenceSize * FILTER_OFFSET_FACTOR)
                            effect.offsetY(filter.getOffset().y / referenceSize * FILTER_OFFSET_FACTOR)
                            effect.radius(filter.blurX / 2.0 / referenceSize * FILTER_WIDTH_FACTOR)
                            effect.color(Color.color(filter.color.red, filter.color.green, filter.color.blue, filter.alphaDouble))
                            effect.setBlurType(BlurType.GAUSSIAN)
                            if (i > 0 || filters.size() == 1) {
                                effect.setInput(lastEffect)
                            }
                            lastEffect = effect
                        } else {
                            effect = new DropShadow()
                            effect.offsetX(filter.getOffset().x / referenceSize * FILTER_OFFSET_FACTOR)
                            effect.offsetY(filter.getOffset().y / referenceSize * FILTER_OFFSET_FACTOR)
                            effect.radius(filter.blurX / 2.0 / referenceSize * FILTER_WIDTH_FACTOR)
                            effect.color(Color.color(filter.color.red, filter.color.green, filter.color.blue, filter.alphaDouble))
                            effect.blurType(BlurType.GAUSSIAN)
                            if (i > 0 || filters.size() == 1) {
                                effect.setInput(lastEffect)
                            }
                            lastEffect = effect
                        }
                        break;
                }
                effects.add(effect)
            }
            code.append("        ${elementName}.setEffect(${lastFilterName});\n")
        }
    }

    private void appendJavaFxColor(StringBuilder code, Color color) {
        code.append("Color.color(${color.red}, ${color.green}, ${color.blue}, ${color.opacity})")
    }

    private void appendJavaFxStops(StringBuilder code, List<Stop> stops, int offset) {
        stops.each { stop ->
            code.append("new Stop(${stop.offset}, Color.color(${stop.color.red}, ${stop.color.green}, ${stop.color.blue}, ${stop.color.opacity}))")
        }
    }

    public String createCssFillStrokeShape(String elementName, boolean fill, boolean stroke) {
        StringBuilder cssCode = new StringBuilder()
        elementName = elementName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
        elementName = elementName.replace("_E_", "_")

        double refWidth  = elementWidth != 0 ? elementWidth : referenceWidth
        double refHeight = elementHeight != 0 ? elementHeight : referenceHeight

        cssCode.append(".")
        cssCode.append("${elementName.toLowerCase().replaceAll('_', '-')}")
        cssCode.append(" {\n")

        if (fill) {
            cssCode.append(createCssFill(refWidth, refHeight))
        }

        if (stroke) {
            cssCode.append(createCssStroke(refWidth, refHeight))
        } else {
            //cssCode.append("    -fx-stroke          : transparent;\n");
        }

        if (cssShape != null && !cssShape.isEmpty()) {
            cssCode.append("    -fx-scale-shape     : true;\n")
            cssCode.append("    -fx-shape           : \"").append(cssShape.trim()).append("\";\n")
        }
        cssCode.append(createCssEffect(refWidth, refHeight))

        cssCode.append("}\n\n")
        return cssCode.toString()
    }

    private String createCssFill(double refWidth, double refHeight) {
        StringBuilder cssCode = new StringBuilder()

        if (getClass().equals(FxgRichText.class)) {
            cssCode.append("     -fx-font-family    : ").append(fontFamily).append(";\n")
            cssCode.append("     -fx-font-size      : ").append((int) (fontSize / refHeight) * 100).append("%;\n")
            cssCode.append("     -fx-font-weight    : ")
            if (FontWeight.NORMAL == weight) {
                cssCode.append('normal').append(";\n")
            } else {
                cssCode.append('bold').append(";\n")
            }
            cssCode.append("     -fx-font-style     : ")
            if (FontPosture.REGULAR == style) {
                cssCode.append('normal').append(";\n")
            } else {
                cssCode.append('italic').append(";\n")
            }
            cssCode.append("     -fx-fill      : ")
        } else {
            cssCode.append("    -fx-background-color: ")
        }
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                cssCode.append(createCssColor(fill.color)).append(";\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                cssCode.append("linear-gradient(")
                cssCode.append("from ${(int) ((fill.startX - elementX) / refWidth) * 100}% ${(int) ((fill.startY - elementY) / refHeight) * 100}% ")
                cssCode.append("to ${(int) ((fill.endX - elementX) / refWidth) * 100}% ${(int) ((fill.endY - elementY) / refHeight) * 100}%, \n")
                for (int i = 0 ; i < fill.stops.size() ; i++) {
                    cssCode.append("                                          ")
                    cssCode.append(createCssColor(fill.stops[i].color)).append(" ")
                    cssCode.append("${(int) (fill.stops[i].offset * 100)}%")
                    if (i < fill.stops.size() - 1) {
                        cssCode.append(", \n")
                    }
                }
                cssCode.append(");\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                cssCode.append("radial-gradient(")
                cssCode.append("focus-angle 0deg, focus-distance 0%, \n")
                cssCode.append("                                          ")
                cssCode.append("center ${(int) ((fill.centerX - elementX) / refWidth) * 100}% ${(int) ((fill.centerY - elementY) / refHeight) * 100}%, \n")
                cssCode.append("                                          ")
                cssCode.append("radius ${(int) (fill.radius / refWidth) * 100}%, \n")
                //cssCode.append("reflect, ")
                for (int i = 0 ; i < fill.stops.size() ; i++) {
                    cssCode.append("                                          ")
                    cssCode.append(createCssColor(fill.stops[i].color)).append(" ")
                    cssCode.append("${(int) (fill.stops[i].offset * 100)}%")
                    if (i < fill.stops.size() - 1) {
                        cssCode.append(", \n")
                    }
                }
                cssCode.append(");\n")
                break
            case FxgFillType.NONE:
                cssCode.append("transparent;\n")
                break
        }
        return cssCode.toString()
    }

    private String createCssStroke(double refWidth, double refHeight) {
        StringBuilder cssCode = new StringBuilder()
        cssCode.append("    -fx-stroke          : ").append(createCssColor(stroke.color)).append(";\n")

        cssCode.append("    -fx-stroke-line-cap : ")
        switch (stroke.cap) {
            case StrokeLineCap.BUTT:
                cssCode.append("butt;\n")
                break
            case StrokeLineCap.ROUND:
                cssCode.append("round;\n")
                break
            case StrokeLineCap.SQUARE:
                cssCode.append("square;\n")
                break
        }

        cssCode.append("    -fx-stroke-line-join: ")
        switch (stroke.join) {
            case StrokeLineJoin.BEVEL:
                cssCode.append("bevel;\n")
                break
            case StrokeLineJoin.ROUND:
                cssCode.append("round;\n")
                break
            case StrokeLineJoin.MITER:
                cssCode.append("miter;\n")
                break
        }
        cssCode.append("    -fx-stroke-width    : ${stroke.width};\n")
        return cssCode.toString()
    }

    private String createCssColor(Color color) {
        StringBuilder cssColor = new StringBuilder()
        if (Double.compare(color.getOpacity(), 0) == 0) {
            cssColor.append("transparent")
        } else if (Double.compare(color.getOpacity(), 1.0) == 0) {
            cssColor.append("rgb(")
            cssColor.append("${(int) (color.getRed() * 255.0)}, ")
            cssColor.append("${(int) (color.getGreen() * 255.0)}, ")
            cssColor.append("${(int) (color.getBlue() * 255.0)})")
        } else {
            cssColor.append("rgba(")
            cssColor.append("${(int) (color.getRed() * 255.0)}, ")
            cssColor.append("${(int) (color.getGreen() * 255.0)}, ")
            cssColor.append("${(int) (color.getBlue() * 255.0)}, ")
            cssColor.append("${new Double(color.getOpacity()).round(5)})")
        }
        return cssColor.toString()
    }

    private String createCssEffect(double refWidth, double refHeight) {
        StringBuilder cssCode = new StringBuilder()
        if (effects.size() == 1) {
            Effect effect = effects[0]
            if (effect.class.equals(InnerShadow.class)) {
                cssCode.append("    -fx-effect          : innershadow(gaussian, ")
                cssCode.append(createCssColor(Color.web(((InnerShadow) effect).color.toString()))).append(", ")
                cssCode.append((int) (((InnerShadow) effect).radius / refWidth * 100)).append("%, ")
                cssCode.append("0.0, ")
                cssCode.append((int) (((InnerShadow) effect).offsetX / refWidth * 100)).append("%, ")
                cssCode.append((int) (((InnerShadow) effect).offsetY / refHeight * 100)).append("%);\n")
            } else if (effect.class.equals(DropShadow.class)) {
                cssCode.append("    -fx-effect          : dropshadow(gaussian, ")
                cssCode.append(createCssColor(Color.web(((DropShadow) effect).color.toString()))).append(", ")
                cssCode.append((int) (((DropShadow) effect).radius / refWidth * 100)).append("%, ")
                cssCode.append("0.0, ")
                cssCode.append((int) (((DropShadow) effect).offsetX / refWidth * 100)).append("%, ")
                cssCode.append((int) (((DropShadow) effect).offsetY / refHeight * 100)).append("%);\n")
            }
        }
        return cssCode.toString()
    }


    // JAVA_FX_CANVAS
    protected void appendJavaFxCanvasFillAndStroke(StringBuilder code, String elementName) {
        if (filled) {
            appendJavaFxCanvasPaint(code, elementName)
        }
        if (stroked) {
            /*
            if (stroke.stroke.lineWidth < 2) {
                code.append("        ${elementName}.setStrokeType(StrokeType.OUTSIDE);\n")
            } else {
                code.append("        ${elementName}.setStrokeType(StrokeType.CENTERED);\n")
            }
            */
            importSet.add("import javafx.scene.shape.StrokeType;")
            importSet.add("import javafx.scene.shape.StrokeLineCap;")
            importSet.add("import javafx.scene.shape.StrokeLineJoin;")
            switch (stroke.cap) {
                case StrokeLineCap.BUTT:
                    code.append("        CTX.setLineCap(StrokeLineCap.BUTT);\n")
                    break
                case StrokeLineCap.ROUND:
                    code.append("        CTX.setLineCap(StrokeLineCap.ROUND);\n")
                    break
                case StrokeLineCap.SQUARE:
                    code.append("        CTX.setLineCap(StrokeLineCap.SQUARE);\n")
                    break
            }
            switch (stroke.join) {
                case StrokeLineJoin.BEVEL:
                    code.append("        CTX.setLineJoin(StrokeLineJoin.BEVEL);\n")
                    break
                case StrokeLineJoin.ROUND:
                    code.append("        CTX.setLineJoin(StrokeLineJoin.ROUND);\n")
                    break
                case StrokeLineJoin.MITER:
                    code.append("        CTX.setLineJoin(StrokeLineJoin.MITER);\n")
                    break
            }
            code.append("        CTX.setLineWidth(${stroke.width / referenceWidth} * WIDTH);\n")
            code.append("        CTX.setStroke(")
            appendJavaFxColor(code, stroke.color)
            code.append(");\n")
            code.append("        CTX.stroke();\n")
        }
    }

    protected void appendJavaFxCanvasPaint(StringBuilder code, String elementName) {
        elementName = elementName.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
        elementName = elementName.replace("_E_", "_")
        int nameLength = elementName.length()

        importSet.add("import javafx.scene.shape.Shape;")

        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                importSet.add("import javafx.scene.paint.Paint;")
                importSet.add("import javafx.scene.paint.Color;")
                code.append("        CTX.setFill(")
                appendJavaFxColor(code, fill.color)
                code.append(");\n")
                code.append("        CTX.fill();\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                importSet.add("import javafx.scene.paint.Paint;")
                importSet.add("import javafx.scene.paint.Color;")
                importSet.add("import javafx.scene.paint.LinearGradient;")
                importSet.add("import javafx.scene.paint.CycleMethod;")
                importSet.add("import javafx.scene.paint.Stop;")
                code.append("        CTX.setFill(")
                code.append("new LinearGradient(${fill.startX / referenceWidth} * WIDTH, ${fill.startY / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, 0, 31)
                code.append("${fill.endX / referenceWidth} * WIDTH, ${fill.endY / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, 0, 31)
                code.append("false, CycleMethod.NO_CYCLE,\n")
                intendCode(code, 8, 0, 31)
                appendJavaFxStops(code, stops, (39))
                code.append("));\n")
                code.append("        CTX.fill();\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                importSet.add("import javafx.scene.paint.Paint;")
                importSet.add("import javafx.scene.paint.Color;")
                importSet.add("import javafx.scene.paint.LinearGradient;")
                importSet.add("import javafx.scene.paint.CycleMethod;")
                importSet.add("import javafx.scene.paint.Stop;")
                importSet.add("import javafx.scene.paint.RadialGradient;")
                code.append("        CTX.setFill(")
                code.append("new RadialGradient(0, 0,\n")
                intendCode(code, 8, 0, 31)
                code.append("${fill.centerX / referenceWidth} * WIDTH, ${fill.centerY / referenceHeight} * HEIGHT,\n")
                intendCode(code, 8, 0, 31)
                code.append("${fill.radius / referenceWidth} * WIDTH,\n")
                intendCode(code, 8, 0, 31)
                code.append("false, CycleMethod.NO_CYCLE,\n")
                intendCode(code, 8, 0, 31)
                appendJavaFxStops(code, fill.stops, (39))
                code.append("));\n")
                code.append("        CTX.fill();\n")
                break
            case FxgFillType.NONE:
                importSet.add("import javafx.scene.paint.Paint;")
                break
        }
    }

    protected void appendJavaFxCanvasFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            final double FILTER_WIDTH_FACTOR = 3.6
            final double FILTER_OFFSET_FACTOR = 1.2
            String lastFilterName
            double referenceSize = referenceWidth <= referenceHeight ? referenceWidth : referenceHeight
            filters.eachWithIndex { filter, i ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {
                            code.append("\n")
                            code.append("        CTX.applyEffect(InnerShadowBuilder.create()\n")
                            code.append("        //.width(${filter.blurX / referenceSize * FILTER_WIDTH_FACTOR} * ${elementName}.getLayoutBounds().getWidth())\n")
                            code.append("        //.height(${filter.blurY / referenceSize * FILTER_WIDTH_FACTOR} * ${elementName}.getLayoutBounds().getHeight())\n")
                            code.append("        .offsetX(${filter.getOffset().x / referenceSize * FILTER_OFFSET_FACTOR} * SIZE)\n")
                            code.append("        .offsetY(${filter.getOffset().y / referenceSize * FILTER_OFFSET_FACTOR} * SIZE)\n")
                            code.append("        .radius(${filter.blurX / 2.0 / referenceSize * FILTER_WIDTH_FACTOR} * ${elementName}.getLayoutBounds().getWidth())\n")
                            code.append("        .color(Color.color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.alphaDouble}))\n")
                            code.append("        .blurType(BlurType.GAUSSIAN)\n")
                            if (i > 0 || filters.size() == 1) {
                                code.append("        .input(${lastFilterName})\n")
                            }
                            code.append("        .build());\n")
                            lastFilterName = "${elementName}_INNER_SHADOW${i}"
                        } else {
                            code.append("\n")
                            code.append("        CTX.applyEffect(DropShadowBuilder.create()\n")
                            code.append("        //.width(${filter.blurX / referenceSize * FILTER_WIDTH_FACTOR} * ${elementName}.getLayoutBounds().getWidth())\n")
                            code.append("        //.height(${filter.blurY / referenceSize * FILTER_WIDTH_FACTOR} * ${elementName}.getLayoutBounds().getHeight())\n")
                            code.append("        .offsetX(${filter.getOffset().x / referenceSize * FILTER_OFFSET_FACTOR} * SIZE)\n")
                            code.append("        .offsetY(${filter.getOffset().y / referenceSize * FILTER_OFFSET_FACTOR} * SIZE)\n")
                            code.append("        .radius(${filter.blurX / 2.0 / referenceSize * FILTER_WIDTH_FACTOR} * ${elementName}.getLayoutBounds().getWidth())\n")
                            code.append("        .color(Color.color(${filter.color.red / 255}, ${filter.color.green / 255}, ${filter.color.blue / 255}, ${filter.alphaDouble}))\n")
                            code.append("        .blurType(BlurType.GAUSSIAN)\n")
                            if (i > 0 || filters.size() == 1) {
                                code.append("        ${elementName}_DROP_SHADOW${i}.setInput(${lastFilterName});\n")
                            }
                            code.append("        .build());\n")
                            lastFilterName = "${elementName}_DROP_SHADOW${i}"
                        }
                        break;
                }
            }
        }
    }


    // CANVAS
    protected void appendCanvasFill(StringBuilder code, String elementName) {
        switch(fill.type) {
            case FxgFillType.SOLID_COLOR:
                code.append("        ctx.fillStyle = ")
                appendCanvasColor(code, fill.color)
                code.append(";\n")
                code.append("        ctx.fill();\n")
                break
            case FxgFillType.LINEAR_GRADIENT:
                code.append("        var ${elementName} = ctx.createLinearGradient((${(fill.startX) / referenceWidth} * width), (${(fill.startY) / referenceHeight} * height), ((${(fill.endX) / referenceWidth}) * width), ((${(fill.endY) / referenceHeight}) * height));\n")
                appendCanvasStops(code, fill.stops, elementName)
                code.append("        ctx.fillStyle = ${elementName};\n")
                code.append("        ctx.fill();\n")
                break
            case FxgFillType.RADIAL_GRADIENT:
                code.append("        var ${elementName} = ctx.createRadialGradient((${fill.centerX / referenceWidth}) * width, ((${fill.centerY / referenceHeight}) * height), 0, ((${fill.centerX / referenceWidth}) * width), ((${fill.centerY / referenceHeight}) * height), ${fill.radius / referenceWidth} * width);\n")
                appendCanvasStops(code, fill.stops, elementName)
                code.append("        ctx.fillStyle = ${elementName};\n")
                code.append("        ctx.fill();\n")
                break
        }
    }

    protected void appendCanvasStroke(StringBuilder code, String elementName) {
            switch (stroke.cap) {
                case StrokeLineCap.BUTT:
                    code.append("        ctx.lineCap = 'butt';\n")
                    break
                case StrokeLineCap.ROUND:
                    code.append("        ctx.lineCap = 'round';\n")
                    break
                case StrokeLineCap.SQUARE:
                    code.append("        ctx.lineCap = 'square';\n")
                    break
            }
            switch (stroke.join) {
                case StrokeLineJoin.BEVEL:
                    code.append("        ctx.lineJoin = 'bevel';\n")
                    break
                case StrokeLineJoin.ROUND:
                    code.append("        ctx.lineJoin = 'round';\n")
                    break
                case StrokeLineJoin.MITER:
                    code.append("        ctx.lineJoin = 'miter';\n")
                    break
            }
            code.append("        ctx.lineWidth = ${stroke.width / referenceWidth} * width;\n")
            code.append("        ctx.strokeStyle = ")
            appendCanvasColor(code, stroke.color)
            code.append(";\n")
            code.append("        ctx.stroke();\n")
    }

    protected void appendCanvasFilter(StringBuilder code, String elementName) {
        if (!filters.isEmpty()) {
            filters.each { filter ->
                switch(filter.type) {
                    case FxgFilterType.SHADOW:
                        if (filter.inner) {

                        } else {
                            code.append("        ctx.shadowOffsetX = ${filter.getOffset().x / referenceWidth} * width;\n")
                            code.append("        ctx.shadowOffsetY = ${filter.getOffset().y / referenceHeight} * height;\n")
                            code.append("        ctx.shadowColor = ")
                            code.append("'rgba(${(int) (filter.color.red * 255)}, ${(int) (filter.color.green * 255)}, ${(int) (filter.color.blue * 255)}, ${filter.color.opacity})'")
                            code.append(";\n")
                            code.append("        ctx.shadowBlur = ${filter.blurX / referenceWidth} * width;\n")
                            code.append("        ctx.fill();\n")
                        }
                        break;
                }
            }
        }
    }

    private void appendCanvasColor(StringBuilder code, Color color) {
        if (color.getOpacity().compareTo(1.0) == 0) {
            code.append("'rgb(${(int) (color.red * 255)}, ${(int) (color.green * 255)}, ${(int) (color.blue * 255)})'")
        } else {
            code.append("'rgba(${(int) (color.red * 255)}, ${(int) (color.green * 255)}, ${(int) (color.blue * 255)}, ${color.opacity})'")
        }
    }

    private void appendCanvasStops(StringBuilder code, List<Stop> stops, String elementName) {
        stops.each {stop ->
            code.append("        ${elementName}.addColorStop(${stop.offset}, ")
            if (stop.color.opacity.compareTo(1.0) == 0) {
                code.append("'rgb(${(int) (stop.color.red * 255)}, ${(int) (stop.color.green * 255)}, ${(int) (stop.color.blue * 255)})'")
            } else {
                code.append("'rgba(${(int) (stop.color.red * 255)}, ${(int) (stop.color.green * 255)}, ${(int) (stop.color.blue * 255)}, ${stop.color.opacity})'")
            }
            code.append(");\n")
        }
    }


    // TOOLS
    private void intendCode(StringBuilder code, int intend, int nameLength, int offset) {
        int numberOfSpaces = intend + nameLength + offset
        for (int i = 0 ; i < numberOfSpaces ; i++) {
            code.append(" ")
        }
    }
}
