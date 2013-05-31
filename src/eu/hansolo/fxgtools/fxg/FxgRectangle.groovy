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
import javafx.scene.shape.Rectangle
/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 06:54
 * To change this template use File | Settings | File Templates.
 */
class FxgRectangle extends FxgShape {
    FxgShapeType type = FxgShapeType.RECT
    double       x
    double       y
    double       rotation
    double       scaleX
    double       scaleY
    double       width
    double       height
    double       radiusX
    double       radiusY
    double       alpha


    Rectangle getRectangle() {
        Rectangle rectangle = new Rectangle(x, y, width, height)
        rectangle.setArcWidth(radiusX * 2)
        rectangle.setArcHeight(radiusY * 2)
        return rectangle
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
                code.append("        final Rectangle ${name} = new Rectangle(${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT,\n")
                code.append("                        ")
                for (int i = 0 ; i < nameLength ; i++) {
                    code.append(" ")
                }
                code.append("                 ")
                code.append("${width / referenceWidth} * WIDTH, ${height / referenceHeight} * HEIGHT);\n")

                if (radiusX > 0) {
                    code.append("        ${name}.setArcWidth(${radiusX * 2 / referenceWidth} * WIDTH);\n")
                }
                if (radiusY > 0) {
                    code.append("        ${name}.setArcHeight(${radiusY * 2 / referenceHeight} * HEIGHT);\n")
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
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    code.append("        CTX.beginPath();\n")
                    code.append("        CTX.rect(${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT, ${width / referenceWidth} * WIDTH, ${height / referenceHeight} * HEIGHT);\n")
                    code.append("        CTX.closePath();\n")
                } else {
                    code.append("        CTX.beginPath();\n")
                    code.append("        CTX.moveTo(${x / referenceWidth} * WIDTH + ${radiusX / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT);\n")
                    code.append("        CTX.lineTo(${x / referenceWidth} * WIDTH + ${width / referenceWidth} * WIDTH - ${radiusX / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT);\n")
                    code.append("        CTX.quadraticCurveTo(${x / referenceWidth} * WIDTH + ${width / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT, ${x / referenceWidth} * WIDTH + ${width / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${radiusX / referenceWidth} * WIDTH);\n")
                    code.append("        CTX.lineTo(${x / referenceWidth} * WIDTH + ${width / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${height / referenceHeight} * HEIGHT - ${radiusX / referenceWidth} * WIDTH);\n")
                    code.append("        CTX.quadraticCurveTo(${x / referenceWidth} * WIDTH + ${width / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${height / referenceHeight} * HEIGHT, ${x / referenceWidth} * WIDTH + ${width / referenceWidth} * WIDTH - ${radiusX / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${height / referenceHeight} * HEIGHT);\n")
                    code.append("        CTX.lineTo(${x / referenceWidth} * WIDTH + ${radiusX / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${height / referenceHeight} * HEIGHT);\n")
                    code.append("        CTX.quadraticCurveTo(${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${height / referenceHeight} * HEIGHT, ${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${height / referenceHeight} * HEIGHT - ${radiusX / referenceWidth} * WIDTH);\n")
                    code.append("        CTX.lineTo(${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT + ${radiusX / referenceWidth} * WIDTH);\n")
                    code.append("        CTX.quadraticCurveTo(${x / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT, ${x / referenceWidth} * WIDTH + ${radiusX / referenceWidth} * WIDTH, ${y / referenceHeight} * HEIGHT);\n")
                    code.append("        CTX.closePath();\n")
                }

                appendJavaFxCanvasFillAndStroke(code, name)
                appendJavaFxCanvasFilter(code, name)

                code.append("        CTX.restore();\n")
                cssShape = ShapeConverter.shapeToSvgString(getRectangle())
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
                if (radiusX.compareTo(0) == 0 && radiusY.compareTo(0) == 0) {
                    code.append("        ctx.beginPath();\n")
                    code.append("        ctx.rect(${x / referenceWidth} * width, ${y / referenceHeight} * height, ${width / referenceWidth} * width, ${height / referenceHeight} * height);\n")
                    code.append("        ctx.closePath();\n")
                } else {
                    code.append("        ctx.beginPath();\n")
                    code.append("        ctx.moveTo(${x / referenceWidth} * width + ${radiusX / referenceWidth} * width, ${y / referenceHeight} * height);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * width + ${width / referenceWidth} * width - ${radiusX / referenceWidth} * width, ${y / referenceHeight} * height);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * width + ${width / referenceWidth} * width, ${y / referenceHeight} * height, ${x / referenceWidth} * width + ${width / referenceWidth} * width, ${y / referenceHeight} * height + ${radiusX / referenceWidth} * width);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * width + ${width / referenceWidth} * width, ${y / referenceHeight} * height + ${height / referenceHeight} * height - ${radiusX / referenceWidth} * width);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * width + ${width / referenceWidth} * width, ${y / referenceHeight} * height + ${height / referenceHeight} * height, ${x / referenceWidth} * width + ${width / referenceWidth} * width - ${radiusX / referenceWidth} * width, ${y / referenceHeight} * height + ${height / referenceHeight} * height);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * width + ${radiusX / referenceWidth} * width, ${y / referenceHeight} * height + ${height / referenceHeight} * height);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * width, ${y / referenceHeight} * height + ${height / referenceHeight} * height, ${x / referenceWidth} * width, ${y / referenceHeight} * height + ${height / referenceHeight} * height - ${radiusX / referenceWidth} * width);\n")
                    code.append("        ctx.lineTo(${x / referenceWidth} * width, ${y / referenceHeight} * height + ${radiusX / referenceWidth} * width);\n")
                    code.append("        ctx.quadraticCurveTo(${x / referenceWidth} * width, ${y / referenceHeight} * height, ${x / referenceWidth} * width + ${radiusX / referenceWidth} * width, ${y / referenceHeight} * height);\n")
                    code.append("        ctx.closePath();\n")
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

    private String checkName() {
        String name = "${shapeName.toUpperCase()}"
        name = name.startsWith("E_") ? name.replaceFirst("E_", "") : name
        name = name.replaceAll("_?RR[0-9]+_([0-9]+_)?", "")
        name = name.replace("_E_", "")
        name = name.startsWith("_") ? name.replaceFirst("_", "") : name
        return name
    }
}
