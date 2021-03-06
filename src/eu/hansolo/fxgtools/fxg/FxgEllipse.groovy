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
import javafx.geometry.Point2D
import javafx.scene.shape.Ellipse

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 07:57
 * To change this template use File | Settings | File Templates.
 */
class FxgEllipse extends FxgShape {
    FxgShapeType type = FxgShapeType.ELLIPSE
    double       x
    double       y
    double       rotation
    double       scaleX
    double       scaleY
    double       width
    double       height
    double       alpha


    Ellipse getEllipse() {
            return new Ellipse(x, y, width, height)
        }

    Point2D getCenter() {
        return new Point2D((width / 2) + x, (height / 2) + y)
    }

    double getRadiusX() {
        return width/2
    }

    double getRadiusY() {
        return height/2
    }

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
                int nameLength = name.length()
                if (width.compareTo(height) == 0) {
                    code.append("        final Circle ${name} = new Circle(${center.x / referenceWidth} * WIDTH, ${center.y / referenceHeight} * HEIGHT, ${getRadiusX() / referenceWidth} * WIDTH);\n")
                } else {
                    code.append("        final Ellipse ${name} = new Ellipse(${center.x / referenceWidth} * WIDTH, ${center.y / referenceHeight} * HEIGHT,\n")
                    code.append("                      ")
                    for (int i = 0 ; i < nameLength ; i++) {
                        code.append(" ")
                    }
                    code.append("               ")
                    code.append("${radiusX / referenceWidth} * WIDTH, ${radiusY / referenceHeight} * HEIGHT);\n")
                }
                if (transformed) {
                    code.append("        final Affine ${name}Transform = new Affine();\n")
                    code.append("        ${name}Transform.setMxx(${transform.scaleX});\n")
                    code.append("        ${name}Transform.setMyx(${transform.shearY});\n")
                    code.append("        ${name}Transform.setMxy(${transform.shearX});\n")
                    code.append("        ${name}Transform.setMyy(${transform.scaleY});\n")
                    code.append("        ${name}Transform.setTx(${transform.translateX / referenceWidth} * WIDTH);\n")
                    code.append("        ${name}Transform.setTy(${transform.translateY / referenceHeight} * HEIGHT);\n")
                    code.append("        ${name}.getTransforms().add(${name}Transform);\n")
                }
                appendJavaFxFillAndStroke(code, name)
                appendJavaFxFilter(code, name)
                code.append("\n")

                return code.toString()
                break;

            case Language.JAVAFX_CANVAS:
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}${shapeName.toUpperCase()}${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }

                code.append("\n")
                code.append("        //${name}\n")
                code.append("        CTX.save();\n")
                if (transformed) {
                    code.append("        CTX.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * WIDTH, ${transform.translateY / referenceHeight} * HEIGHT);\n")
                }
                code.append("        CTX.scale(${width / height}, 1);\n")
                code.append("        CTX.beginPath();\n")
                code.append("        CTX.arc(${center.x / referenceWidth / (width / height)} * WIDTH, ${center.y / referenceHeight} * HEIGHT, ${radiusX / referenceWidth / (width / height)} * WIDTH, ${radiusX / referenceWidth / (width / height)} * WIDTH, 0, 360);\n")

                appendJavaFxCanvasFillAndStroke(code, name)
                appendJavaFxCanvasFilter(code, name)

                code.append("        CTX.restore();\n")
                cssShape = ShapeConverter.shapeToSvgString(getEllipse())
                return code.toString()

            case Language.CANVAS:
                if (NAME_SET.contains(name)) {
                    name = "${layerName.toUpperCase()}${shapeName.toUpperCase()}${SHAPE_INDEX}"
                } else {
                    NAME_SET.add(name)
                }

                code.append("\n")
                code.append("        //${name}\n")
                code.append("        ctx.save();\n")
                if (transformed) {
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * width, ${transform.translateY / referenceHeight} * height);\n")
                }
                code.append("        ctx.scale(${width / height}, 1);\n")
                code.append("        ctx.beginPath();\n")
                code.append("        ctx.arc(${center.x / referenceWidth / (width / height)} * width, ${center.y / referenceHeight} * height, ${radiusX / referenceWidth / (width / height)} * width, 0, 2 * Math.PI, false);\n")
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
