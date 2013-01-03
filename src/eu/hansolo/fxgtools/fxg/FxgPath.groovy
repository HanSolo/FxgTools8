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

import eu.hansolo.fxgtools.main.ShapeConverter
import javafx.scene.shape.*


/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 07:59
 * To change this template use File | Settings | File Templates.
 */
class FxgPath extends FxgShape {
    FxgShapeType type = FxgShapeType.PATH
    Path         path
    double       rotation
    double       scaleX
    double       scaleY
    double       alpha

    Path getPath() {
        return path;
    }

    String translateTo(final Language LANGUAGE, final int SHAPE_INDEX, final HashSet<String> NAME_SET) {
        StringBuilder code = new StringBuilder()
        String        name = "${shapeName}"
        switch (LANGUAGE) {
            case Language.JAVAFX_CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        CTX.save();\n")
                if (transformed) {
                    code.append("        CTX.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * WIDTH, ${transform.translateY / referenceHeight} * HEIGHT);\n")
                }
                code.append("        CTX.beginPath();\n")
                for (PathElement element : path.elements) {
                    if (element.getClass().equals(MoveTo)) {
                        code.append("        CTX.moveTo(${((MoveTo) element).x / referenceWidth} * WIDTH, ${((MoveTo) element).y / referenceHeight} * HEIGHT);\n")
                    } else if (element.getClass().equals(LineTo)) {
                        code.append("        CTX.lineTo(${((LineTo) element).x / referenceWidth} * WIDTH, ${((LineTo) element).y / referenceHeight} * HEIGHT);\n")
                    } else if (element.getClass().equals(QuadCurveTo)) {
                        code.append("        CTX.quadraticCurveTo(${((QuadCurveTo) element).x / referenceWidth} * WIDTH, ${((QuadCurveTo) element).y / referenceHeight} * HEIGHT, ${((QuadCurveTo) element).controlX / referenceWidth} * WIDTH, ${((QuadCurveTo) element).controlY / referenceHeight} * HEIGHT);\n")
                    } else if (element.getClass().equals(CubicCurveTo)) {
                        code.append("        CTX.bezierCurveTo(${((CubicCurveTo) element).x / referenceWidth} * WIDTH, ${((CubicCurveTo) element).y / referenceHeight} * HEIGHT, ${((CubicCurveTo) element).controlX1 / referenceWidth} * WIDTH, ${((CubicCurveTo) element).controlY1 / referenceHeight} * HEIGHT, ${((CubicCurveTo) element).controlX2 / referenceWidth} * WIDTH, ${((CubicCurveTo) element).controlY2 / referenceHeight} * HEIGHT);\n")
                    } else if (element.getClass().equals(ClosePath)) {
                        code.append("        CTX.closePath();\n")
                    }
                }
                appendJavaFxCanvasFillAndStroke(code, name)
                appendJavaFxCanvasFilter(code, name)
                code.append("        CTX.restore();\n")
                System.out.println("before: " + cssShape)
                cssShape = ShapeConverter.shapeToSvgString(getPath())
                System.out.println("after : " + cssShape)
                System.out.println("-------------------")
                return code.toString()

            case Language.CANVAS:
                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                if (transformed) {
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * imageWidth, ${transform.translateY / referenceHeight} * imageHeight);\n")
                }
                code.append("        ctx.beginPath();\n")
                for (PathElement element : path.elements) {
                    if (element.getClass().equals(MoveTo)) {
                        code.append("        ctx.moveTo(${((MoveTo) element).x / referenceWidth} * imageWidth, ${((MoveTo) element).y / referenceHeight} * imageHeight);\n")
                    } else if (element.getClass().equals(LineTo)) {
                        code.append("        ctx.lineTo(${((LineTo) element).x / referenceWidth} * imageWidth, ${((LineTo) element).y / referenceHeight} * imageHeight);\n")
                    } else if (element.getClass().equals(QuadCurveTo)) {
                        code.append("        ctx.quadraticCurveTo(${((QuadCurveTo) element).x / referenceWidth} * imageWidth, ${((QuadCurveTo) element).y / referenceHeight} * imageHeight, ${((QuadCurveTo) element).controlX / referenceWidth} * imageWidth, ${((QuadCurveTo) element).controlY / referenceHeight} * imageHeight);\n")
                    } else if (element.getClass().equals(CubicCurveTo)) {
                        code.append("        ctx.bezierCurveTo(${((CubicCurveTo) element).x / referenceWidth} * imageWidth, ${((CubicCurveTo) element).y / referenceHeight} * imageHeight, ${((CubicCurveTo) element).controlX1 / referenceWidth} * imageWidth, ${((CubicCurveTo) element).controlY1 / referenceHeight} * imageHeight, ${((CubicCurveTo) element).controlX2 / referenceWidth} * imageWidth, ${((CubicCurveTo) element).controlY2 / referenceHeight} * imageHeight);\n")
                    } else if (element.getClass().equals(ClosePath)) {
                        code.append("        ctx.closePath();\n")
                    }
                }
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
}
