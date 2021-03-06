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

import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:36
 * To change this template use File | Settings | File Templates.
 */
class FxgRichText extends FxgShape{
    FxgShapeType type = FxgShapeType.TEXT
    double       x
    double       y
    double       rotation
    double       scaleX
    double       scaleY
    double       fontSize
    Font         font
    boolean      underline
    boolean      lineThrough
    String       text
    String       fontFamily
    FontPosture  style
    FontWeight   weight
    double       alpha
    Color        color


    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX, final HashSet<String> NAME_SET) {
        StringBuilder code = new StringBuilder()
        String        name = checkName()
        switch (LANGUAGE) {
            case Language.JAVAFX:
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}${shapeName.toUpperCase()}${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                String fontWeight  = (FontWeight.BOLD == weight ? "FontWeight.BOLD" : "FontWeight.NORMAL")
                String fontPosture = (FontPosture.ITALIC == style ? "FontPosture.ITALIC" : "FontPosture.REGULAR")
                code.append("        final Text ${name} = new Text();\n")
                code.append("        ${name}.setText(\"${text.trim()}\");\n")
                //code.append("        ${name}.setFont(Font.font(\"${font.getFontName()}\", ${fontWeight}, ${fontPosture}, ${font.size / referenceWidth} * WIDTH));\n")
                code.append("        ${name}.setFont(Font.font(\"${fontFamily}\", ${fontWeight}, ${fontPosture}, ${font.size / referenceWidth} * WIDTH));\n")
                code.append("        ${name}.setX(${x / referenceWidth} * WIDTH);\n")
                code.append("        ${name}.setY(${y / referenceHeight} * HEIGHT);\n")
                code.append("        ${name}.setTextOrigin(VPos.BOTTOM);\n")
                code.append(lineThrough ? "        ${name}.setStrikeThrough(true);\n" : "")
                code.append(underline ? "        ${name}.setUnderline(true);\n" : "")
                if (transformed) {
                    code.append("        final Affine ${name}_Transform = new Affine();\n")
                    code.append("        ${name}_Transform.setMxx(${transform.scaleX});\n")
                    code.append("        ${name}_Transform.setMyx(${transform.shearY});\n")
                    code.append("        ${name}_Transform.setMxy(${transform.shearX});\n")
                    code.append("        ${name}_Transform.setMyy(${transform.scaleY});\n")
                    code.append("        ${name}_Transform.setTx(${transform.translateX / referenceWidth} * WIDTH);\n")
                    code.append("        ${name}_Transform.setTy(${transform.translateY / referenceHeight} * HEIGHT);\n")
                    code.append("        ${name}.getTransforms().add(${name}_Transform);\n")
                }
                code.append("        ${name}.getTransforms().add(new Rotate(${rotation}, ${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT));\n")
                code.append("        ${name}.getTransforms().add(new Scale(${scaleX}, ${scaleY}));\n")
                appendJavaFxPaint(code, name)
                appendJavaFxFilter(code, name)
                code.append("\n")
                return code.toString()
                break;

            case Language.JAVAFX_CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        CTX.save();\n")
                if (transformed) {
                    code.append("        CTX.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * WIDTH, ${transform.translateY / referenceHeight} * HEIGHT);\n")
                }
                if (rotation != 0) {
                    code.append("        CTX.translate(${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT);\n")
                    code.append("        CTX.rotate(${Math.toRadians(rotation)});\n")
                    code.append("        CTX.translate(${-x / referenceWidth} * WIDTH, ${-y / referenceHeight} * HEIGHT);\n")
                }
                if (scaleX != 1 || scaleY != 1) {
                    code.append("        CTX.scale(${scaleX}, ${scaleY});\n")
                }
                if (fill.type != null) {
                    code.append("        CTX.font = '")
                    style == FontPosture.ITALIC ? code.append("italic "):code.append("")
                    weight == FontWeight.BOLD ? code.append("bold "):code.append("")
                    code.append("${(int) fontSize}px ")
                    code.append("${font.family}';\n")
                    code.append("        CTX.textBaseline = 'bottom';\n")
                    code.append("        CTX.fillText('${text.trim()}', ${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT);\n")
                }

                // maybe the next line is not needed
                appendJavaFxCanvasFillAndStroke(code, name)
                if (stroked) {
                    code.append("        CTX.strokeText('${text.trim()}', ${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT);\n")
                }
                appendJavaFxCanvasFilter(code, name)

                if (scaleX != 1 || scaleY != 1) {
                    code.append("        CTX.scale(${-scaleX}, ${-scaleY});\n")
                }
                code.append("        CTX.restore();\n")
                return code.toString()

            case Language.CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                if (transformed) {
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * width, ${transform.translateY / referenceHeight} * height);\n")
                }
                if (rotation != 0) {
                    code.append("        ctx.translate(${x / referenceWidth} * width, ${y / referenceHeight} * height);\n")
                    code.append("        ctx.rotate(${Math.toRadians(rotation)});\n")
                    code.append("        ctx.translate(${-x / referenceWidth} * width, ${-y / referenceHeight} * height);\n")
                }
                if (scaleX != 1 || scaleY != 1) {
                    code.append("        ctx.scale(${scaleX}, ${scaleY});\n")
                }
                if (fill.type != null) {
                    code.append("        ctx.font = '")
                    style == FontPosture.ITALIC ? code.append("italic "):code.append("")
                    weight = FontWeight.BOLD ? code.append("bold "):code.append("")
                    code.append("${(int) fontSize}px ")
                    code.append("${font.family}';\n")
                    code.append("        ctx.textBaseline = 'bottom';\n")
                    code.append("        ctx.fillText('${text.trim()}', ${x / referenceWidth} * width, ${y / referenceHeight} * height);\n")
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                    code.append("        ctx.strokeText('${text.trim()}', ${x / referenceWidth} * width, ${y / referenceHeight} * height);\n")
                }
                appendCanvasFilter(code, name)
                if (scaleX != 1 || scaleY != 1) {
                    code.append("        ctx.scale(${-scaleX}, ${-scaleY});\n")
                }
                code.append("        ctx.restore();\n")
                return code.toString()

            default:
                return "NOT SUPPORTED"
        }
    }

    private String checkName() {
        String name = "${shapeName.toUpperCase()}"
        name = name.startsWith("E_") ? name.replaceFirst("E_", "") : name
        name = name.replaceAll("_?RR[0-9]+_([0-9]+_)?", "")
        name = name.replace("_E_", "")
        name = name.startsWith("_") ? name.replaceFirst("_", "") : name
        return name
    }
}
