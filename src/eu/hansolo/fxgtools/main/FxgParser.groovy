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

import eu.hansolo.fxgtools.fxg.FxgColor
import eu.hansolo.fxgtools.fxg.FxgElement
import eu.hansolo.fxgtools.fxg.FxgEllipse
import eu.hansolo.fxgtools.fxg.FxgFill
import eu.hansolo.fxgtools.fxg.FxgFilter
import eu.hansolo.fxgtools.fxg.FxgLine
import eu.hansolo.fxgtools.fxg.FxgLinearGradient
import eu.hansolo.fxgtools.fxg.FxgNoFill
import eu.hansolo.fxgtools.fxg.FxgPath
import eu.hansolo.fxgtools.fxg.FxgRadialGradient
import eu.hansolo.fxgtools.fxg.FxgRectangle
import eu.hansolo.fxgtools.fxg.FxgRichText
import eu.hansolo.fxgtools.fxg.FxgShadow
import eu.hansolo.fxgtools.fxg.FxgShape
import eu.hansolo.fxgtools.fxg.FxgShapeType
import eu.hansolo.fxgtools.fxg.FxgVariable
import groovy.transform.TupleConstructor
import groovy.xml.Namespace
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.effect.InnerShadow
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Paint
import javafx.scene.paint.RadialGradient
import javafx.scene.paint.Stop
import javafx.scene.shape.ArcTo
import javafx.scene.shape.ClosePath
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.Ellipse
import javafx.scene.shape.FillRule
import javafx.scene.shape.HLineTo
import javafx.scene.shape.Line
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.QuadCurveTo
import javafx.scene.shape.Rectangle
import javafx.scene.shape.Shape
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.shape.VLineTo
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.transform.Transform

import java.util.regex.Pattern

/**
 * User: han.solo at muenster.de
 * Date: 27.08.11
 * Time: 18:42
 */
class FxgParser {

    // Variable declarations
    private final Namespace              D               = new Namespace("http://ns.adobe.com/fxg/2008/dt")
    private final Namespace              FXG             = new Namespace("http://ns.adobe.com/fxg/2008")
    private final Pattern                VAR_PATTERN     = Pattern.compile("[\\n\\r\\t\\.:;]*")
    private final Pattern                SPACE_PATTERN   = Pattern.compile("[\\s\\-]+")
    private boolean                      useOriginalSize = false;
    private String                       lastNodeType
    private String                       elementName
    private HashSet<String>              elementNameSet  = []
    String                               fxgVersion
    double                               originalWidth
    double                               originalHeight
    private double                       width
    private double                       height
    private double                       scaleFactorX    = 1.0
    private double                       scaleFactorY    = 1.0
    double                               aspectRatio
    private double                       offsetX
    private double                       offsetY
    private double                       groupOffsetX
    private double                       groupOffsetY
    private double                       lastShapeAlpha
    private HashMap<String, FxgVariable> properties      = new HashMap<String, FxgVariable>()
    private Transform                    groupTransform
    @TupleConstructor()
    private class FxgStroke {
        Color          color
        StrokeLineJoin join
        StrokeLineCap  cap
        float          width
    }
    @TupleConstructor()
    private class FxgPaint {
        double     startX  = -1
        double     startY  = -1
        double     endX    = -1
        double     endY    = -1
        double     centerX = -1
        double     centerY = -1
        double     radius  = -1
        List<Stop> stops
        Color      color

        Paint getPaint() {
            if (startX != -1 && startY != -1 && endX != -1 && endY != -1 && !stops.empty) {
                return new LinearGradient(startX, startY, endX, endY, false, CycleMethod.NO_CYCLE, stops)
            } else if (centerX != -1 && centerY != -1 && radius != -1 && !stops.empty) {
                return new RadialGradient(0, 0, centerX, centerY, radius, false, CycleMethod.NO_CYCLE, stops)
            } else if (color != null) {
                return color
            }
            return null
        }
    }
    private class FxgPathReader {
        protected List   path
        protected double scaleFactorX
        protected double scaleFactorY

        FxgPathReader(List newPath, final SCALE_FACTOR_X, final SCALE_FACTOR_Y){
            path = newPath
            scaleFactorX = SCALE_FACTOR_X
            scaleFactorY = SCALE_FACTOR_Y
        }
        String read() {
            path.remove(0)
        }
        double nextX() {
            read().toDouble() * scaleFactorX
        }
        double nextY() {
            read().toDouble() * scaleFactorY
        }
    }


    // ******************** Methods *******************************************
    Map<String, Group> parse(String fileName, double width, double height, boolean keepAspect) {
        return parse(new XmlParser().parse(new File(fileName)), width, height, keepAspect)
    }

    Map<String, Group> parse(Node fxg, double width, double height, boolean keepAspect) {
        Map<String, Group> groups = [:]
        prepareParameters(fxg, width, height, keepAspect)

        def layers
        if (fxg.Group[0].attribute(D.layerType) && fxg.Group[0].attribute(D.userLabel)) { // fxg contains page attribute
            layers = fxg.Group[0].findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        } else {                                                                          // fxg does not contain page (Fireworks standard)
            layers = fxg.Group.findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        }

        layers.eachWithIndex {def layer, int i ->
            String layerName = groups.keySet().contains(layer.attribute(D.userLabel)) ? layer.attribute(D.userLabel) : layer.attribute(D.userLabel) + "_$i"
            if (layerName.toLowerCase().startsWith("properties")) {
                convertProperties(layer)
            } else {
                Group group = new Group()
                convertLayer(layer, group)
            }
        }

        return groups
    }

    Map<String, List<FxgElement>> getElements(final String FILE_NAME) {
        return getElements(new XmlParser().parse(new File(FILE_NAME)))
    }

    Map<String, List<FxgElement>> getElements(final Node FXG) {
        Map<String, List<FxgElement>> elements = [:]

        originalWidth = (FXG.@viewWidth ?: 100).toInteger()
        originalHeight = (FXG.@viewHeight ?: 100).toInteger()

        width = originalWidth
        height = originalHeight

        scaleFactorX = 1.0
        scaleFactorY = 1.0

        aspectRatio = originalHeight / originalWidth

        def layers
        if (FXG.Group[0].attribute(D.layerType) && FXG.Group[0].attribute(D.userLabel)) { // fxg contains page attribute
            layers = FXG.Group[0].findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        } else {                                                                          // fxg does not contain page (Fireworks standard)
            layers = FXG.Group.findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        }
        String layerName
        int shapeIndex = 0
        layers.eachWithIndex {def layer, int i ->
            layerName = layer.attribute(D.userLabel)// + "$i"
            if (elements.keySet().contains(layerName)) {
                layerName += "$i"
            }
            layerName = layerName.replaceAll(VAR_PATTERN, "")
            layerName = layerName.replaceAll(SPACE_PATTERN, "_")
            List shapes = []
            if (layerName.toLowerCase().startsWith("properties")) {
                convertProperties(layer)
            } else {
                shapeIndex = convertLayer(layerName, layer, elements, shapes, shapeIndex)
            }
        }
        return elements
    }

    Rectangle getDimension(final Node FXG) {
        originalWidth  = (int)(FXG.@viewWidth ?: 100).toDouble()
        originalHeight = (int)(FXG.@viewHeight ?: 100).toDouble()
        fxgVersion     = FXG.@version
        return new Rectangle(originalWidth, originalHeight)
    }

    Rectangle getDimension(final String FILE_NAME) {
        return getDimension(new XmlParser().parse(new File(FILE_NAME)))
    }

    HashMap<String, FxgVariable> getProperties() {
        return properties;
    }


    // ******************** Parse to FXG shapes   *****************************
    private FxgRectangle parseFxgRectangle(final NODE, final String LAYER_NAME, final int INDEX) {
        String elementName = validateElementName(LAYER_NAME, NODE.attribute(D.userLabel)?:"Rectangle", INDEX)
        double x           = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y           = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width       = (NODE.@width ?: 0).toDouble() * scaleFactorX
        double height      = (NODE.@height ?: 0).toDouble() * scaleFactorY
        double scaleX      = (NODE.@scaleX ?: 1).toDouble()
        double scaleY      = (NODE.@scaleY ?: 1).toDouble()
        double rotation    = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha     = (NODE.@alpha ?: 1).toDouble()
        double radiusX     = (NODE.@radiusX ?: 0).toDouble() * scaleFactorX
        double radiusY     = (NODE.@radiusY ?: 0).toDouble() * scaleFactorY
        String cssShape    = "M ${x} ${y} L ${x + width} ${y} L ${x + width} ${y + height} L ${x} ${y + height} L ${x} ${y} Z"

        return new FxgRectangle(layerName: LAYER_NAME,
                                shapeName: elementName,
                                x        : x,
                                y        : y,
                                width    : width,
                                height   : height,
                                radiusX  : radiusX,
                                radiusY  : radiusY,
                                alpha    : lastShapeAlpha,
                                rotation : rotation,
                                scaleX   : scaleX,
                                scaleY   : scaleY,
                                cssShape :  cssShape)
    }

    private FxgEllipse parseFxgEllipse(final NODE, final String LAYER_NAME, final int INDEX) {
        String elementName = validateElementName(LAYER_NAME, NODE.attribute(D.userLabel)?:"Ellipse", INDEX)
        double x           = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y           = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width       = (NODE.@width ?: 0).toDouble() * scaleFactorX
        double height      = (NODE.@height ?: 0).toDouble() * scaleFactorY
        double scaleX      = (NODE.@scaleX ?: 1).toDouble()
        double scaleY      = (NODE.@scaleY ?: 1).toDouble()
        double rotation    = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha     = (NODE.@alpha ?: 1).toDouble()
        String cssShape    = ""

        return new FxgEllipse(layerName: LAYER_NAME,
                              shapeName: elementName,
                              x        : x,
                              y        : y,
                              width    : width,
                              height   : height,
                              alpha    : lastShapeAlpha,
                              rotation : rotation,
                              scaleX   : scaleX,
                              scaleY   : scaleY,
                              cssShape : cssShape)
    }

    private FxgLine parseFxgLine(final NODE, final String LAYER_NAME, final int INDEX) {
        String elementName = validateElementName(LAYER_NAME, NODE.attribute(D.userLabel)?:"Line", INDEX)
        double xFrom       = ((NODE.@xFrom ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yFrom       = ((NODE.@yFrom ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double xTo         = ((NODE.@xTo ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yTo         = ((NODE.@yTo ?: 0).toDouble() + groupOffsetY) * scaleFactorX
        double scaleX      = (NODE.@scaleX ?: 1).toDouble()
        double scaleY      = (NODE.@scaleY ?: 1).toDouble()
        double rotation    = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha     = (NODE.@alpha ?: 1).toDouble()
        String cssShape    = "M ${xFrom} ${yFrom} L ${xTo} ${yTo} Z"
        return new FxgLine(layerName: LAYER_NAME,
                           shapeName: elementName,
                           x1       : xFrom,
                           y1       : yFrom,
                           x2       : xTo,
                           y2       : yTo,
                           alpha    : lastShapeAlpha,
                           rotation : rotation,
                           scaleX   : scaleX,
                           scaleY   : scaleY,
                           cssShape : cssShape)
    }

    private FxgPath parseFxgPath(final NODE, final String LAYER_NAME, final String ELEMENT_NAME) {
        String data     = NODE.@data ?: ''
        double x        = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y        = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double scaleX   = (NODE.@scaleX ?: 1).toDouble()
        double scaleY   = (NODE.@scaleY ?: 1).toDouble()
        double rotation = (NODE.@rotation ?: 0).toDouble()
        lastShapeAlpha  = (NODE.@alpha ?: 1).toDouble()
        String winding  = (NODE.@winding ?: 'evenOdd')
        final Path PATH = new Path()

        if (winding == 'evenOdd') {
            PATH.setFillRule(FillRule.EVEN_ODD)
        } else if (winding == 'nonZero') {
            PATH.setFillRule(FillRule.NON_ZERO)
        }

        String cssShape = data

        data            = data.replaceAll(/([A-Za-z])/, / $1 /) // alle einzelnen Grossbuchstaben in blanks huellen
        def pathList    = data.tokenize()
        def pathReader  = new FxgPathReader(pathList, scaleFactorX, scaleFactorY)

        processPath(pathList, pathReader, PATH, x, y)

        return new FxgPath(layerName: LAYER_NAME,
                           shapeName: ELEMENT_NAME,
                           path     : PATH,
                           rotation : rotation,
                           scaleX   : scaleX,
                           scaleY   : scaleY,
                           cssShape : cssShape)
    }

    private FxgRichText parseFxgRichText(final NODE, final String LAYER_NAME, final int INDEX) {
        String elementName  = validateElementName(LAYER_NAME, NODE.attribute(D.userLabel)?:"Font", INDEX)
        FxgRichText fxgText = new FxgRichText()
        fxgText.layerName   = LAYER_NAME
        fxgText.shapeName   = elementName
        def fxgLabel        = NODE.content[0].p[0]
        fxgLabel            = fxgLabel ?: NODE.content[0].div[0].p[0]
        String text
        double fontSize
        String colorString
        if (fxgLabel.span) {
            // Adobe Illustrator
            fxgLabel    = fxgLabel ?: NODE.content[0].div[0].p[0].span[0]
            text        = fxgLabel.text()
            fontSize    = (NODE.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (NODE.@color ?: '#000000')
        } else {
            // Adobe Fireworks
            text        = fxgLabel.text()
            fontSize    = (fxgLabel.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (NODE.content.p.@color[0] ?: '#000000')
        }
        float x = ((NODE.@x ?: 0).toDouble() + groupOffsetX) * (float) scaleFactorX
        float y = ((NODE.@y ?: 0).toDouble() + groupOffsetY) * (float) scaleFactorY
        double rotation       = ((NODE.@rotation ?: 0).toDouble())
        double scaleX         = (NODE.@scaleX ?: 1).toDouble()
        double scaleY         = (NODE.@scaleY ?: 1).toDouble()
        String fontFamily     = (fxgLabel.@fontFamily ?: 'sans-serif')
        String textDecoration = (NODE.@textDecoration ?: 'none')
        String lineThrough    = (NODE.@lineThrough ?: 'false')
        double alpha          = parseAlpha(NODE, 1.0)
        fxgText.x             = x
        fxgText.y             = (y + fontSize)
        fxgText.rotation      = rotation
        fxgText.scaleX        = scaleX
        fxgText.scaleY        = scaleY
        fxgText.fontSize      = fontSize
        fxgText.fontFamily    = fontFamily
        fxgText.color         = parseColor(colorString, alpha)
        fxgText.underline     = textDecoration == 'underline'
        fxgText.lineThrough   = lineThrough == 'true'
        fxgText.style         = NODE.@fontStyle == 'italic' ? FontPosture.ITALIC : FontPosture.REGULAR
        fxgText.weight        = fxgLabel.@fontWeight == 'bold' ? FontWeight.BOLD : FontWeight.NORMAL
        fxgText.font          = Font.font(fontFamily, fxgText.weight, fxgText.style, fontSize);
        fxgText.text          = text
        return fxgText
    }


    // ******************** Parse to JavaFX shapes   **************************
    private Rectangle parseRectangle(node) {
        double x       = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y       = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width   = (node.@width ?: 0).toDouble() * scaleFactorX
        double height  = (node.@height ?: 0).toDouble() * scaleFactorY
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()
        double radiusX = (node.@radiusX ?: 0).toDouble() * scaleFactorX
        double radiusY = (node.@radiusY ?: 0).toDouble() * scaleFactorY

        Rectangle rect = new Rectangle(x, y, width,height);
        rect.setArcWidth(radiusX > 0 ? radiusX : 0)
        rect.setArcHeight(radiusY > 0 ? radiusY : 0)
        return rect
    }

    private Ellipse parseEllipse(node) {
        double x       = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y       = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width   = (node.@width ?: 0).toDouble() * scaleFactorX
        double height  = (node.@height ?: 0).toDouble() * scaleFactorY
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()

        return new Ellipse(x + width / 2, y + height / 2, width, height)
    }

    private Line parseLine(node) {
        double xFrom   = ((node.@xFrom ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yFrom   = ((node.@yFrom ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double xTo     = ((node.@xTo ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yTo     = ((node.@yTo ?: 0).toDouble() + groupOffsetY) * scaleFactorX
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()

        return new Line(xFrom, yFrom, xTo, yTo)
    }

    private Path parsePath(node) {
        String data    = node.@data ?: ''
        double x       = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y       = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()
        String winding = (node.@winding ?: 'evenOdd')
        Path path = new Path()
        path.setFillRule(winding == 'evenOdd' ? FillRule.EVEN_ODD : FillRule.NON_ZERO)
        data = data.replaceAll(/(,)/, / /)
        data = data.replaceAll(/([A-Za-z])/, / $1 /) // wrap single characters in blanks
        def pathList = data.tokenize()
        def pathReader = new FxgPathReader(pathList, scaleFactorX, scaleFactorY)
        processPath(pathList, pathReader, path, x, y)
        return path
    }


    // ********** Parse Fill , Stroke, Transform, Filter, Effects, Color ******
    private FxgPaint parseFill(final NODE) {
        FxgPaint paint = new FxgPaint()
        if (NODE.fill) {
            def fill = NODE.fill[0]
            if (fill != null) {
                if (fill.SolidColor) {
                    convertSolidColor(paint, fill)
                }
                if (fill.LinearGradient) {
                    convertLinearGradient(paint, fill)
                }
                if (fill.RadialGradient) {
                    convertRadialGradient(paint, fill)
                }
            }
        }
        return paint
    }

    private FxgStroke parseStroke(final NODE) {
        FxgStroke      fxgStroke = new FxgStroke()
        Color          color     = Color.BLACK
        double         weight    = 0f
        StrokeLineCap  cap       = StrokeLineCap.SQUARE
        StrokeLineJoin join      = StrokeLineJoin.MITER
        if (NODE.stroke) {
            def stroke = NODE.stroke
            if (stroke.SolidColorStroke) {
                def solidColorStroke = stroke[0].SolidColorStroke
                String colorString   = (solidColorStroke[0].@color ?: '#000000')
                weight               = (solidColorStroke[0].@weight ?: 1.0)
                String caps          = (solidColorStroke[0].@caps ?: 'round')
                String joints        = (solidColorStroke[0].@joints ?: 'round')
                int alpha            = parseAlpha(solidColorStroke[0], lastShapeAlpha)
                color                = parseColor(colorString, alpha)
                switch(caps){
                    case 'none':
                        cap = StrokeLineCap.BUTT
                        break
                    case 'square':
                        cap = StrokeLineCap.SQUARE
                        break
                    case 'round':
                        cap = StrokeLineCap.ROUND
                        break
                }
                switch(joints) {
                    case 'miter':
                        join = StrokeLineJoin.MITER
                        break
                    case 'bevel':
                        join = StrokeLineJoin.BEVEL
                        break
                    case 'round':
                        join = StrokeLineJoin.ROUND
                    break
                }
            }
        }
        fxgStroke.color = color
        fxgStroke.cap   = cap
        fxgStroke.join  = join
        fxgStroke.width = weight
        return fxgStroke
    }

    private Transform parseTransform(final NODE) {
        Transform transform = Transform.scale(1, 1)
        if (NODE.transform.Transform.matrix.Matrix) {
            def    matrix = NODE.transform.Transform.matrix.Matrix
            double a      = ((matrix.@a[0] ?: 0.0).toDouble()) // scaleX
            double b      = ((matrix.@b[0] ?: 0.0).toDouble()) // shearY
            double c      = ((matrix.@c[0] ?: 0.0).toDouble()) // shearX
            double d      = ((matrix.@d[0] ?: 0.0).toDouble()) // scaleY
            double tx     = ((matrix.@tx[0] ?: 0.0).toDouble() + groupOffsetX) * scaleFactorX // translateX
            double ty     = ((matrix.@ty[0] ?: 0.0).toDouble() + groupOffsetY) * scaleFactorY // translateY
            transform     = Transform.affine(a, b, c, d, tx, ty)
         }
        return transform
    }

    private List<FxgFilter> parseFilter(final NODE) {
        if (NODE.DropShadowFilter) {
            List<FxgFilter> filters = []
            NODE.DropShadowFilter.each {def shadow->
                FxgShadow fxgFilter   = new FxgShadow()
                fxgFilter.angle       = (shadow.@angle ?: 0).toInteger()
                String colorString    = (shadow.@color ?: '#000000')
                fxgFilter.distance    = (shadow.@distance ?: 0).toDouble() * scaleFactorX
                fxgFilter.alpha       = ((shadow.@alpha ?: 1).toDouble() * lastShapeAlpha)
                fxgFilter.alphaDouble = ((shadow.@alpha ?: 1).toDouble() * lastShapeAlpha)
                fxgFilter.blurX       = (shadow.@blurX ?: 0).toDouble() * scaleFactorX
                fxgFilter.blurY       = (shadow.@blurY ?: 0).toDouble() * scaleFactorY
                fxgFilter.inner       = (shadow.@inner ?: false)
                fxgFilter.color       = parseColor(colorString, fxgFilter.alpha)
                filters.add(fxgFilter)
            }
            return filters
        }
        return null
    }

    private List<Effect> parseEffects(final NODE) {
        if (NODE.DropShadowFilter) {
            List<Effect> effects = []
            Effect lastEffect
            NODE.DropShadowFilter.eachWithIndex {def shadow, i ->
                double angle       = (shadow.@angle ?: 0).toDouble()
                String colorString = (shadow.@color ?: '#000000')
                double distance    = (shadow.@distance ?: 0).toDouble() * scaleFactorX
                double alpha       = ((shadow.@alpha ?: 1.0).toDouble() * lastShapeAlpha)
                double blurX       = (shadow.@blurX ?: 0).toDouble() * scaleFactorX
                double blurY       = (shadow.@blurY ?: 0).toDouble() * scaleFactorY
                Color color        = parseColor(colorString, alpha)
                Point2D offset     = new Point2D(distance * Math.cos(Math.toRadians(-angle)), distance * Math.sin(Math.toRadians(angle)))
                Effect effect
                if (shadow.@inner) {
                    effect = new InnerShadow()
                    effect.setOffsetX(offset.getX())
                    effect.setOffsetY(offset.getY())
                    effect.setRadius(blurX)
                    effect.setColor(color)
                    effect.setBlurType(BlurType.GAUSSIAN)
                    if (i > 0 || effects.size() == 1) {
                        effect.setInput(lastEffect)
                    }
                    lastEffect = effect
                } else {
                    effect = new DropShadow()
                    effect.setOffsetX(offset.getX())
                    effect.setOffsetY(offset.getY())
                    effect.setRadius(blurX)
                    effect.setColor(color)
                    effect.setBlurType(BlurType.GAUSSIAN)
                    if (i > 0 || effects.size() == 1) {
                        effect.setInput(lastEffect)
                    }
                    lastEffect = effect
                }
                effects.add(effect)
            }
            return effects
        }
        return null
    }

    private Color parseColor(final NODE) {
        String color = (NODE.@color ?: '#000000')
        double alpha = (NODE.@alpha ?: 1).toDouble() * lastShapeAlpha
        return parseColor(color, alpha)
    }

    private Color parseColor(final String COLOR, final double ALPHA) {
        double red   = Integer.valueOf(COLOR[1..2], 16).intValue() / 255
        double green = Integer.valueOf(COLOR[3..4], 16).intValue() / 255
        double blue  = Integer.valueOf(COLOR[5..6], 16).intValue() / 255
        if (COLOR.size() == 7) {
            Color.color(red, green, blue, ALPHA)
        } else {
            double alpha = Double.valueOf(COLOR[7..8], 16).doubleValue()
            Color.color(red, green, blue, alpha)
        }
    }

    private double parseAlpha(final NODE, final double LAST_SHAPE_ALPHA) {
        String alphaString = (NODE.@alpha ?: '1.0')
        double alpha
        if (alphaString.equals('NaN') || alphaString.equals('-Infinity')) {
            alpha = 0.0
        } else if (alphaString.equals('Infinity')) {
            alpha = lastShapeAlpha
        } else {
            alpha = (Double.parseDouble(alphaString) * LAST_SHAPE_ALPHA)
        }
        return alpha
    }

    private processPath(final PATH_LIST, final FxgPathReader READER, final Path PATH, double x, double y) {
        while (PATH_LIST) {
            switch (READER.read()) {
                case "M":
                    PATH.getElements().add(new MoveTo(READER.nextX() + x, READER.nextY() + y))
                    break
                case "L":
                    PATH.getElements().add(new LineTo(READER.nextX() + x, READER.nextY() + y))
                    break
                case "C":
                    PATH.getElements().add(new CubicCurveTo(READER.nextX() + x, READER.nextY() + y, READER.nextX() + x, READER.nextY() + y, READER.nextX() + x, READER.nextY() + y))
                    break
                case "Q":
                    PATH.getElements().add(new QuadCurveTo(READER.nextX() + x, READER.nextY() + y, READER.nextX() + x, READER.nextY() + y))
                    break
                case "H":
                    PATH.getElements().add(new HLineTo(READER.nextX() + x))
                    break
                case "L":
                    PATH.getElements().add(new VLineTo(READER.nextY() + y))
                    break
                case "A":
                    PATH.getElements().add(new ArcTo(READER.nextX() + x, READER.nextY() + y, READER.nextX() + x, READER.nextY() + y))
                    break
                case "Z":
                    PATH.getElements().add(new ClosePath())
                    break
            }
        }
    }

    private void convertSolidColor(paint, node) {
        paint.color = parseColor((node.SolidColor[0] ?: '#000000'))
    }

    private convertLinearGradient(paint, node) {
        def linearGradient = node.LinearGradient[0]
        double x1          = (linearGradient.@x ?: 0).toDouble() * scaleFactorX
        double y1          = (linearGradient.@y ?: 0).toDouble() * scaleFactorY
        double scaleX      = (linearGradient.@scaleX ?: 0).toDouble()
        //double scaleY      = (linearGradient.@scaleY ?: 1).toDouble()
        double rotation    = Math.toRadians((linearGradient.@rotation ?: 0).toDouble())
        double x2          = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double y2          = Math.sin(rotation) * scaleX * scaleFactorY + y1

        double startX      = x1 + offsetX
        double startY      = y1 + offsetY
        double endX        = x2 + offsetX
        double endY        = y2 + offsetY

        List<Stop> stops    = convertGradientEntries(linearGradient.GradientEntry)

        paint.startX = startX
        paint.startY = startY
        paint.endX   = endX
        paint.endY   = endY
        paint.stops  = stops

    }

    private convertRadialGradient(paint, node) {
        def radialGradient  = node.RadialGradient[0]
        double x1           = (radialGradient.@x ?: 0).toDouble() * scaleFactorX + offsetX
        double y1           = (radialGradient.@y ?: 0).toDouble() * scaleFactorY + offsetY
        double scaleX       = (radialGradient.@scaleX ?: 0).toDouble()
        //double scaleY       = (radialGradient.@scaleY ?: 0).toDouble()
        double rotation     = Math.toRadians((radialGradient.@rotation ?: 0).toDouble())
        double x2           = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double y2           = Math.sin(rotation) * scaleX * scaleFactorY + y1

        Point2D center      = new Point2D(x1, y1)
        Point2D stop        = new Point2D(x2, y2)
        double radius       = center.distance(stop) / 2.0

        List<Stop> stops    = convertGradientEntries(radialGradient.GradientEntry)

        paint.centerX       = center.getX()
        paint.centerY       = center.getY()
        paint.radius        = radius ?: 0.001
        paint.stops         = stops
    }

    private List<Stop> convertGradientEntries(gradientEntries) {
        List<Stop> stops = new ArrayList<>()
        gradientEntries.each { def gradientEntry->
            double fraction = (gradientEntry.@ratio ?: 0).toDouble()
            double alpha    = (gradientEntry.@alpha ?: 1).toDouble() * lastShapeAlpha
            Color color     = gradientEntry.@color == null ? Color.BLACK : parseColor(gradientEntry.@color, alpha)
            stops.add(new Stop(fraction, color))
        }
        return stops
    }


    // ******************** Convert layers   **********************************
    private int convertLayer(final String LAYER_NAME, final Node LAYER, Map<String, List<FxgElement>> elements, List shapes, int index) {
        LAYER.eachWithIndex {Node node, int i->
            if ('visible' != node.@visible) {
                Paint paint
                FxgStroke stroke
                FxgShape fxgShape = null
                index += 1
                switch(node.name()) {
                    case FXG.Group:
                        elementName  = node.attribute(D.userLabel)?:"Group"
                        lastNodeType = "Group"
                        groupOffsetX = (node.@x ?: 0).toDouble()
                        groupOffsetY = (node.@y ?: 0).toDouble()
                        // Take group transforms into account
                        if (node.transform) {
                            groupTransform = parseTransform(node)
                        } else {
                            groupTransform = null
                        }
                        convertLayer(LAYER_NAME, node, elements, shapes, index)
                        break
                    case FXG.Rect:
                        fxgShape               = parseFxgRectangle(node, LAYER_NAME, i)
                        offsetX                = fxgShape.x
                        offsetY                = fxgShape.y
                        fxgShape.elementX      = fxgShape.x
                        fxgShape.elementY      = fxgShape.y
                        fxgShape.elementWidth  = fxgShape.width
                        fxgShape.elementHeight = fxgShape.height
                        lastNodeType           = "Rect"
                        break
                    case FXG.Ellipse:
                        fxgShape               = parseFxgEllipse(node, LAYER_NAME, i)
                        offsetX                = fxgShape.x
                        offsetY                = fxgShape.y
                        fxgShape.elementX      = fxgShape.x
                        fxgShape.elementY      = fxgShape.y
                        fxgShape.elementWidth  = fxgShape.width
                        fxgShape.elementHeight = fxgShape.height
                        lastNodeType           = "Ellipse"
                        break
                    case FXG.Line:
                        fxgShape               = parseFxgLine(node, LAYER_NAME, i)
                        offsetX                = fxgShape.x1
                        offsetY                = fxgShape.y1
                        fxgShape.elementX      = fxgShape.x1
                        fxgShape.elementY      = fxgShape.y1
                        fxgShape.elementWidth  = Math.abs(fxgShape.x2 - fxgShape.x1)
                        fxgShape.elementHeight = Math.abs(fxgShape.y2 - fxgShape.y1)
                        lastNodeType           = "Line"
                        break
                    case FXG.Path:
                        elementName = lastNodeType == "Group" ?  elementName : (node.attribute(D.userLabel)?:"Path")
                        if (elementName != null) {
                            elementName = validateElementName(LAYER_NAME, elementName, i)
                        }
                        fxgShape = parseFxgPath(node, LAYER_NAME, elementName)
                        offsetX                = groupOffsetX
                        offsetY                = groupOffsetY
                        fxgShape.elementX      = fxgShape.path.boundsInLocal.getMinX() + 1
                        fxgShape.elementY      = fxgShape.path.boundsInLocal.getMinY() + 1
                        fxgShape.elementWidth  = fxgShape.path.boundsInLocal.getWidth() - 2
                        fxgShape.elementHeight = fxgShape.path.boundsInLocal.getHeight() - 2
                        lastNodeType           = "Path"
                        break
                    case FXG.RichText:
                        elementName            = validateElementName(LAYER_NAME, node.attribute(D.userLabel)?:"Text", i)
                        fxgShape               = parseFxgRichText(node, LAYER_NAME, i)
                        FxgFill fxgFill        = new FxgColor(layerName: LAYER_NAME, shapeName: elementName, alpha: (double)(fxgShape.color.opacity), color: fxgShape.color)
                        fxgShape.fill          = fxgFill
                        fxgShape.elementX      = fxgShape.x
                        fxgShape.elementY      = fxgShape.y
                        fxgShape.elementWidth  = 0
                        fxgShape.elementHeight = 0
                        lastNodeType           = "RichText"
                        break
                }
                if (fxgShape != null) {
                    if (node.transform) {
                        fxgShape.transform   = parseTransform(node)
                        fxgShape.transformed = true
                    } else if (groupTransform != null) {
                        fxgShape.transform   = groupTransform
                        fxgShape.transformed = true
                    }
                    if (node.fill) {
                        FxgFill fxgFill
                        paint = parseFill(node).paint
                        if (paint instanceof Color) {
                            fxgFill = new FxgColor(layerName: LAYER_NAME, shapeName: elementName, alpha: ((Color) paint).opacity, color: (Color) paint)
                        } else if (paint instanceof LinearGradient) {
                            fxgFill = new FxgLinearGradient(layerName: LAYER_NAME, shapeName: elementName, startX: ((LinearGradient) paint).startX, startY: ((LinearGradient) paint).startY, endX: ((LinearGradient) paint).endX, endY: ((LinearGradient) paint).endY, stops: ((LinearGradient) paint).stops)
                        } else if (paint instanceof RadialGradient) {
                            fxgFill = new FxgRadialGradient(layerName: LAYER_NAME, shapeName: elementName, centerX: ((RadialGradient) paint).centerX, centerY: ((RadialGradient) paint).centerY, radius: ((RadialGradient) paint).radius, stops: ((RadialGradient) paint).stops)
                        } else {
                            fxgFill = new FxgNoFill()
                        }
                        fxgShape.fill   = fxgFill
                        fxgShape.filled = true
                    } else {
                        if (fxgShape.type == FxgShapeType.TEXT) {
                            fxgShape.filled = true
                        }
                        if (node.name() != FXG.RichText) {
                            fxgShape.fill   = new FxgNoFill()
                            fxgShape.filled = true
                        }
                    }
                    if (node.filters) {
                        fxgShape.filters = parseFilter(node.filters)
                        fxgShape.effects = parseEffects(node.filters)
                    }
                    if (node.stroke) {
                        FxgStroke fxgStroke = parseStroke(node)
                        fxgShape.stroke     = new eu.hansolo.fxgtools.fxg.FxgStroke(name: elementName, color: fxgStroke.color, cap: fxgStroke.cap, join: fxgStroke.join, width: fxgStroke.width)
                        fxgShape.stroked    = true
                    }
                    fxgShape.referenceWidth  = originalWidth
                    fxgShape.referenceHeight = originalHeight
                    shapes.add(new FxgElement(name: elementName, shape: fxgShape))

                    groupTransform = null
                }
            }
        }
        if (!shapes.isEmpty()) {
            elements.put(LAYER_NAME, shapes)
        }

        return index
    }

    private Shape paintShape(node, shape) {
        if (groupTransform != null) {
            shape.transforms.add(groupTransform)
        }
        shape.setFill(parseFill(node))
        parseStroke(node, shape)
        if(node.filters) {
            shape.setEffect(parseFilter(node, shape))
        }
        groupTransform = null
        return shape
    }

    private Group convertLayer(layer, group) {
        List<Shape>     nodes   = new ArrayList<>();
        double          x       = groupOffsetX * scaleFactorX
        double          y       = groupOffsetY * scaleFactorY
        double          width   = originalWidth * scaleFactorX
        double          height  = originalHeight * scaleFactorY
        final Rectangle iBounds = new Rectangle(x, y, width, height)
        iBounds.setOpacity(0.0)
        iBounds.setStroke(null)
        nodes.add(iBounds)

        layer.each {Node node->
            if ('false' != node.@visible) {
            Shape shape
            switch(node.name()) {
                case FXG.Group:
                    groupOffsetX = (node.@x ?: 0).toDouble()
                    groupOffsetY = (node.@y ?: 0).toDouble()
                    if (node.transform) {
                        groupTransform = parseTransform(node)
                    } else {
                        groupTransform = null
                    }
                    convertLayer(node, group)
                    break
                case FXG.Rect:
                    shape   = parseRectangle(node)
                    offsetX = shape.layoutBounds.minX
                    offsetY = shape.layoutBounds.minY
                    nodes.add(paintShape(node, shape))
                    break
                case FXG.Ellipse:
                    shape   = parseEllipse(node)
                    offsetX = shape.layoutBounds.minX
                    offsetY = shape.layoutBounds.minY
                    nodes.add(paintShape(node, shape))
                    break
                case FXG.Line:
                    shape   = parseLine(node)
                    offsetX = shape.layoutBounds.minX
                    offsetY = shape.layoutBounds.minY
                    nodes.add(paintShape(node, shape))
                    break
                case FXG.Path:
                    offsetX = groupOffsetX
                    offsetY = groupOffsetY
                    shape   = parsePath(node)
                    nodes.add(paintShape(node, shape))
                    break
                case FXG.RichText:
                    Text text = parseRichText(node)
                    nodes.add(text)
                    break
            }
        }
        }
        group.getChildren().setAll(nodes)
        return group
    }

    private void prepareParameters(def fxg, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        properties.clear()
        originalWidth  = (int)(fxg.@viewWidth ?: 100).toDouble()
        originalHeight = (int)(fxg.@viewHeight ?: 100).toDouble()

        width          = WIDTH
        height         = KEEP_ASPECT ? WIDTH * (originalHeight / originalWidth) : HEIGHT

        aspectRatio    = originalHeight / originalWidth

        if (useOriginalSize) {
            scaleFactorX = 1.0
            scaleFactorY = 1.0
        } else {
            scaleFactorX   = width / originalWidth
            scaleFactorY   = height / originalHeight
        }
    }

    private String validateElementName(String layerName, String elementName, final int INDEX) {
        if (elementNameSet.contains(layerName + elementName.capitalize())) {
            elementName += "${INDEX}"
        } else {
            elementNameSet.add(layerName + elementName.capitalize())
        }
        return elementName
    }


    // ******************** Convert properties ********************************
    private void convertProperties(final Node LAYER) {
        LAYER.each {Node node->
            String[] propertyDefinition = (node.attribute(D.userLabel)?:"").split("_")
            if (propertyDefinition.length > 0) {
                if (propertyDefinition.length >= 2) {
                    properties.put(propertyDefinition[1], new FxgVariable(name: propertyDefinition[1], type: propertyDefinition[0], defaultValue: propertyDefinition[2]))
                } else {
                    String defaultValue
                    if (propertyDefinition[0].toLowerCase().equals("double")) {
                        defaultValue = "0.0";
                    } else if (propertyDefinition[0].toLowerCase().equals("int")) {
                        defaultValue = "0";
                    } else if (propertyDefinition[0].toLowerCase().equals("long")) {
                        defaultValue = "0l";
                    } else if (propertyDefinition[0].toLowerCase().equals("boolean")) {
                        defaultValue = "false";
                    } else if (propertyDefinition[0].toLowerCase().equals("string")) {
                        defaultValue = "\"\"";
                    } else if (propertyDefinition[0].toLowerCase().equals("object")) {
                        defaultValue = "new Object()";
                    } else {
                        defaultValue = "";
                    }
                    properties.put(propertyDefinition[1], new FxgVariable(name: propertyDefinition[1], type: propertyDefinition[0], defaultValue: defaultValue))
                }
            }
        }
    }
}
