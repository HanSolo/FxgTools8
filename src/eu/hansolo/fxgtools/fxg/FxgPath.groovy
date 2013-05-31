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
            case Language.JAVAFX:
                name = "${shapeName.toUpperCase()}"
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}_${shapeName.toUpperCase()}_${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }
                name = name.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
                name = name.replace("_E_", "_")
                int nameLength = name.length()

                code.append("        final Path $name = new Path();\n")
                code.append(FillRule.EVEN_ODD == path.getFillRule() ? "        ${name}.setFillRule(FillRule.EVEN_ODD);\n" : "        ${name}.setFillRule(FillRule.NON_ZERO);\n")
                for (PathElement element : path.getElements()) {
                    if (MoveTo.class.equals(element.getClass())) {
                        code.append("        ${name}.getElements().add(new MoveTo(${((MoveTo) element).getX() / referenceWidth} * WIDTH, ${((MoveTo) element).getY() / referenceHeight} * HEIGHT));\n")
                    } else if (LineTo.class.equals(element.getClass())) {
                        code.append("        ${name}.getElements().add(new LineTo(${((LineTo) element).getX() / referenceWidth} * WIDTH, ${((LineTo) element).getY() / referenceHeight} * HEIGHT));\n")
                    } else if (CubicCurveTo.class.equals(element.getClass())) {
                        code.append("        ${name}.getElements().add(new CubicCurveTo(${((CubicCurveTo) element).getControlX1() / referenceWidth} * WIDTH, ${((CubicCurveTo) element).getControlY1() / referenceHeight} * HEIGHT,\n")
                        code.append("        ")
                        for (int i = 0 ; i < nameLength ; i++) {
                            code.append(" ")
                        }
                        code.append("                                    ")
                        code.append("${((CubicCurveTo) element).getControlX2() / referenceWidth} * WIDTH, ${((CubicCurveTo) element).getControlY2() / referenceHeight} * HEIGHT,\n")
                        code.append("        ")
                        for (int i = 0 ; i < nameLength ; i++) {
                            code.append(" ")
                        }
                        code.append("                                    ")
                        code.append("${((CubicCurveTo) element).getX() / referenceWidth} * WIDTH, ${((CubicCurveTo) element).getY() / referenceHeight} * HEIGHT));\n")
                    } else if (QuadCurveTo.class.equals(element.getClass())) {
                        code.append("        ${name}.getElements().add(new QuadCurveTo(${((QuadCurveTo) element).getControlX() / referenceWidth} * WIDTH, ${((QuadCurveTo) element).getControlY() / referenceHeight} * HEIGHT,\n")
                        code.append("        ");
                        for (int i = 0 ; i < nameLength ; i++) {
                            code.append(" ")
                        }
                        code.append("                                   ")
                        code.append("${((QuadCurveTo) element).getX() / referenceWidth} * WIDTH, ${((QuadCurveTo) element).getY() / referenceHeight} * HEIGHT));\n")
                    } else if (ArcTo.class.equals(element.getClass())) {
                        fxPath.append("A ")
                              .append(((ArcTo) element).getX()).append(" ")
                              .append(((ArcTo) element).getY()).append(" ")
                              .append(((ArcTo) element).getRadiusX()).append(" ")
                              .append(((ArcTo) element).getRadiusY()).append(" ");
                    } else if (HLineTo.class.equals(element.getClass())) {
                        fxPath.append("H ")
                              .append(((HLineTo) element).getX()).append(" ");
                    } else if (VLineTo.class.equals(element.getClass())) {
                        fxPath.append("V ")
                              .append(((VLineTo) element).getY()).append(" ");
                    } else if (ClosePath.class.equals(element.getClass())) {
                        code.append("        ${name}.getElements().add(new ClosePath());\n")
                    }
                }
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
                appendJavaFxFillAndStroke(code, name)
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
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * width, ${transform.translateY / referenceHeight} * height);\n")
                }
                code.append("        ctx.beginPath();\n")
                for (PathElement element : path.elements) {
                    if (element.getClass().equals(MoveTo)) {
                        code.append("        ctx.moveTo(${((MoveTo) element).x / referenceWidth} * width, ${((MoveTo) element).y / referenceHeight} * height);\n")
                    } else if (element.getClass().equals(LineTo)) {
                        code.append("        ctx.lineTo(${((LineTo) element).x / referenceWidth} * width, ${((LineTo) element).y / referenceHeight} * height);\n")
                    } else if (element.getClass().equals(QuadCurveTo)) {
                        code.append("        ctx.quadraticCurveTo(${((QuadCurveTo) element).x / referenceWidth} * width, ${((QuadCurveTo) element).y / referenceHeight} * height, ${((QuadCurveTo) element).controlX / referenceWidth} * width, ${((QuadCurveTo) element).controlY / referenceHeight} * height);\n")
                    } else if (element.getClass().equals(CubicCurveTo)) {
                        code.append("        ctx.bezierCurveTo(${((CubicCurveTo) element).controlX1 / referenceWidth} * width, ${((CubicCurveTo) element).controlY1 / referenceHeight} * height, ${((CubicCurveTo) element).controlX2 / referenceWidth} * width, ${((CubicCurveTo) element).controlY2 / referenceHeight} * height, ${((CubicCurveTo) element).x / referenceWidth} * width, ${((CubicCurveTo) element).y / referenceHeight} * height);\n")
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
