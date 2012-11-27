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
                    code.append("        ctx.setTransform(${transform.scaleX}, ${transform.shearY}, ${transform.shearX}, ${transform.scaleY}, ${transform.translateX / referenceWidth} * imageWidth, ${transform.translateY / referenceHeight} * imageHeight);\n")
                }
                if (rotation != 0) {
                    code.append("        ctx.translate(${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
                    code.append("        ctx.rotate(${Math.toRadians(rotation)});\n")
                    code.append("        ctx.translate(${-x / referenceWidth} * imageWidth, ${-y / referenceHeight} * imageHeight);\n")
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
                    code.append("        ctx.fillText('${text.trim()}', ${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
                }
                if (stroked) {
                    appendCanvasStroke(code, name)
                    code.append("        ctx.strokeText('${text.trim()}', ${x / referenceWidth} * imageWidth, ${y / referenceHeight} * imageHeight);\n")
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
