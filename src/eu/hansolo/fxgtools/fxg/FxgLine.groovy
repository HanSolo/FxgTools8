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

import javafx.scene.shape.Line

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 07:58
 * To change this template use File | Settings | File Templates.
 */
class FxgLine extends FxgShape {
    FxgShapeType type = FxgShapeType.LINE
    double       x1
    double       y1
    double       x2
    double       y2
    double       rotation
    double       scaleX
    double       scaleY
    double       alpha


    Line getLine() {
        return new Line(x1, y1, x2, y2)
    }

    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX, final HashSet<String> NAME_SET) {
        StringBuilder code = new StringBuilder()
        String        name = checkName()
        switch (LANGUAGE) {
            case Language.JAVAFX_CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        CTX.save();\n")
                if (transformed) {
                    code.append("        CTX.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * WIDTH, ${transform.translateY / referenceHeight} * HEIGHT);\n")
                }
                code.append("        CTX.beginPath();\n")
                code.append("        CTX.moveTo(${x1 / referenceWidth} * WIDTH, ${y1 / referenceHeight} * HEIGHT);\n")
                code.append("        CTX.lineTo(${x2 / referenceWidth} * WIDTH, ${y2 / referenceHeight} * HEIGHT);\n")
                code.append("        CTX.closePath();\n")

                appendJavaFxCanvasFillAndStroke(code, name)
                appendJavaFxCanvasFilter(code, name)

                code.append("        CTX.restore();\n")
                return code.toString()

            case Language.CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                if (transformed) {
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * imageWidth, ${transform.translateY / referenceHeight} * imageHeight);\n")
                }
                code.append("        ctx.beginPath();\n")
                code.append("        ctx.moveTo(${x1 / referenceWidth} * imageWidth, ${y1 / referenceHeight} * imageHeight);\n")
                code.append("        ctx.lineTo(${x2 / referenceWidth} * imageWidth, ${y2 / referenceHeight} * imageHeight);\n")
                code.append("        ctx.closePath();\n")
                if (filled) {
                    appendCanvasFill(code, name)
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                }
                appendCanvasFilter(code, name)
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
