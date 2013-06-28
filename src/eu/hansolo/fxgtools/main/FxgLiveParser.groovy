package eu.hansolo.fxgtools.main

import eu.hansolo.fxgtools.fxg.FxgVariable
import groovy.xml.Namespace
import javafx.geometry.Dimension2D
import javafx.geometry.Point2D
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.effect.InnerShadow
import javafx.scene.paint.*
import javafx.scene.shape.*
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.transform.Affine
import javafx.scene.transform.Scale
import javafx.scene.transform.Transform

/**
 * Created by
 * User: hansolo
 * Date: 04.03.13
 * Time: 16:58
 */
class FxgLiveParser {
    private final  Namespace D   = new Namespace("http://ns.adobe.com/fxg/2008/dt")
    private final  Namespace FXG = new Namespace("http://ns.adobe.com/fxg/2008")
    private double originalWidth
    private double originalHeight
    private double previewWidth
    private double previewHeight
    private double scaleFactorX = 1.0
    private double scaleFactorY = 1.0
    private double aspectRatio
    private double offsetX
    private double offsetY
    private double groupOffsetX
    private double groupOffsetY
    private double lastShapeAlpha
    private Affine groupTransform
    private HashMap<String, FxgVariable> properties = new HashMap<String, FxgVariable>()
    private class FxgPathReader {
        protected List   path
        protected double scaleFactorX
        protected double scaleFactorY

        FxgPathReader(List newPath, final SCALE_FACTOR_X, final SCALE_FACTOR_Y){
            path         = newPath
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


    // ********************   P U B L I C   M E T H O D S   ************************************************************
    Map<String, Group> parse(String fileName, double width, double height, boolean keepAspect) {
        if (fileName.toLowerCase().endsWith(".fxg")) {
            return parse(new XmlParser().parse(new File(fileName)), width, height, keepAspect)
        } else {
            return [:]
        }
    }

    Map<String, Group> parse(Node fxg, double width, double height, boolean keepAspect) {
        Map<String, Group> groups = new LinkedHashMap<>()
        prepareParameters(fxg, width, height, keepAspect)

        def layers
        if (fxg.Group[0].attribute(D.layerType) && fxg.Group[0].attribute(D.userLabel)) { // fxg contains page attribute
            layers = fxg.Group[0].findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        } else {                                                                          // fxg does not contain page (Fireworks standard)
            layers = fxg.Group.findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        }

        layers.eachWithIndex {def layer, int i ->
            String layerName = groups.keySet().contains(layer.attribute(D.userLabel)) ? layer.attribute(D.userLabel) + "_$i" : layer.attribute(D.userLabel)
            if (layerName.toLowerCase().startsWith("properties")) {
                convertProperties(layer)
            } else {
                Group  group      = new Group()
                groups[layerName] = convertLayer(layer, group)
            }
        }

        return groups
    }

    Dimension2D getDimension(final Node FXG) {
        originalWidth  = (int)(FXG.@viewWidth ?: 100).toDouble()
        originalHeight = (int)(FXG.@viewHeight ?: 100).toDouble()
        return new Dimension2D(originalWidth, originalHeight)
    }

    Dimension2D getDimension(final String FILE_NAME) {
        return getDimension(new XmlParser().parse(new File(FILE_NAME)))
    }

    HashMap<String, FxgVariable> getControlProperties() {
        return properties
    }


    // ********************   P R I V A T E   M E T H O D S   **********************************************************
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

    private processPath(pathList, FxgPathReader reader, Path path, double x, double y) {
        while (pathList) {
            switch (reader.read()) {
                case "M":
                    path.getElements().add(new MoveTo(reader.nextX() + x, reader.nextY() + y))
                    break
                case "L":
                    path.getElements().add(new LineTo(reader.nextX() + x, reader.nextY() + y))
                    break
                case "C":
                    path.getElements().add(new CubicCurveTo(reader.nextX() + x, reader.nextY() + y, reader.nextX() + x, reader.nextY() + y, reader.nextX() + x, reader.nextY() + y))
                    break
                case "Q":
                    path.getElements().add(new QuadCurveTo(reader.nextX() + x, reader.nextY() + y, reader.nextX() + x, reader.nextY() + y))
                    break
                case "H":
                    path.getElements().add(new HLineTo(reader.nextX() + x))
                    break
                case "V":
                    path.getElements().add(new VLineTo(reader.nextY() + y))
                    break
                case "A":
                    path.getElements().add(new ArcTo(reader.nextX() + x, reader.nextY() + y, reader.nextX() + x, reader.nextY() + y))
                    break
                case "Z":
                    path.getElements().add(new ClosePath())
                    break
            }
        }
    }

    private Text parseRichText(node) {
        def fxgLabel = node.content[0].p[0]
        fxgLabel = fxgLabel ?: node.content[0].div[0].p[0]
        String text
        double fontSize
        String colorString
        if (fxgLabel.span) {
            // Adobe Illustrator
            fxgLabel = fxgLabel ?: node.content[0].div[0].p[0].span[0]
            text = fxgLabel.text()
            fontSize = (node.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (node.@color ?: '#000000')
        } else {
            // Adobe Fireworks
            text = fxgLabel.text()
            fontSize = (fxgLabel.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (node.content.p.@color[0] ?: '#000000')
        }
        double x = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double rotation = ((node.@rotation ?: 0).toDouble())
        double scaleX = ((node.@scaleX ?: 1).toDouble())
        double scaleY = ((node.@scaleY ?: 1).toDouble())
        String fontFamily = (fxgLabel.@fontFamily ?: 'sans-serif')
        String fontStyle = (node.@fontStyle ?: 'normal')
        String textDecoration = (node.@textDecoration ?: 'none')
        boolean lineThrough = ((node.@lineThrough ?: 'false')) == 'true'
        double alpha = (node.@alpha ?: 1).toDouble() * lastShapeAlpha
        y += fontSize
        Color color = parseColor(colorString, alpha)
        boolean bold = ((fxgLabel.@fontWeight ?: 'normal') == 'bold') == 'bold'
        boolean italic = fontStyle == 'italic'
        boolean underline = textDecoration == 'underline'
        FontWeight fontWeight = (bold ? FontWeight.BOLD : FontWeight.NORMAL)
        FontPosture fontPosture = (italic ? FontPosture.ITALIC : FontPosture.REGULAR)
        Text richtext = new Text(text.trim())
        richtext.setFont(Font.font(fontFamily, fontWeight, fontPosture, fontSize))
        richtext.setX(x)
        richtext.setY(y)
        richtext.setTextOrigin(VPos.BOTTOM)
        richtext.setStrikethrough(lineThrough)
        richtext.setUnderline(underline)
        richtext.setFill(color)
        //richtext.boundsType = TextBoundsType.LOGICAL

        //richtext.getTransforms().add(new Rotate(rotation, richtext.x, richtext.y))
        richtext.getTransforms().add(new Scale(scaleX, scaleY))
        if (node.transform) {
            Transform transform = parseTransform(node)
            richtext.getTransforms().add(transform)
        }
        return richtext
    }

    private Paint parseFill(node) {
        Paint paint = null
        if (node.fill) {
            def fill = node.fill[0]
            if (fill != null) {
                if (fill.SolidColor) {
                    paint = parseColor(node.fill.SolidColor[0])
                }
                if (fill.LinearGradient){
                    paint = convertLinearGradient(node.fill)
                }
                if (fill.RadialGradient) {
                    paint = convertRadialGradient(node.fill)
                }
            }
        }
        return paint
    }

    private Shape parseStroke(node, shape) {
        if (node.stroke) {
            def stroke = node.stroke
            if (stroke.SolidColorStroke) {
                def solidColorStroke = stroke[0].SolidColorStroke
                String colorString   = (solidColorStroke[0].@color ?: '#000000')
                double weight        = (solidColorStroke[0].@weight ?: 1f).toDouble() * scaleFactorX
                String caps          = (solidColorStroke[0].@caps ?: 'round')
                String joints        = (solidColorStroke[0].@joints ?: 'round')
                int alpha            = (solidColorStroke[0].@alpha ?: 1).toDouble() * lastShapeAlpha
                Color color          = parseColor(colorString, alpha)

                //weight.compareTo(2.0) <= 0 ? shape.setStrokeType(StrokeType.OUTSIDE) : shape.setStrokeType(StrokeType.CENTERED)

                switch (caps) {
                    case 'none':
                        shape.setStrokeLineCap(StrokeLineCap.BUTT)
                        break
                    case 'round':
                        shape.setStrokeLineCap(StrokeLineCap.ROUND)
                        break
                    case 'square':
                        shape.setStrokeLineCap(StrokeLineCap.SQUARE)
                        break
                }
                switch (joints) {
                    case 'bevel':
                        shape.setStrokeLineJoin(StrokeLineJoin.BEVEL)
                        break
                    case 'round':
                        shape.setStrokeLineJoin(StrokeLineJoin.ROUND)
                        break
                    case 'mite':
                        shape.setStrokeLineJoin(StrokeLineJoin.MITER)
                        break
                }
                shape.setStrokeWidth(weight)
                shape.setStroke(color)
            }
        } else {
            shape.setStroke(null)
        }

        return shape
    }

    private Affine parseTransform(node) {
        Affine transform = new Affine()
        if (node.transform.Transform.matrix.Matrix) {
            def matrix = node.transform.Transform.matrix.Matrix
            transform.setMxx((matrix.@a[0] ?: 0.0).toDouble()) // scaleX                                        m00  a
            transform.setMyx((matrix.@b[0] ?: 0.0).toDouble()) // shearY                                        m10  b
            transform.setMxy((matrix.@c[0] ?: 0.0).toDouble()) // shearX                                        m11  c
            transform.setMyy((matrix.@d[0] ?: 0.0).toDouble()) // scaleY                                        m02  d
            transform.setTx(((matrix.@tx[0] ?: 0.0).toDouble() + groupOffsetX) * scaleFactorX) // translateX    m02  tx
            transform.setTy(((matrix.@ty[0] ?: 0.0).toDouble() + groupOffsetY) * scaleFactorY) // translateY    m12  ty
        }
        return transform
    }

    private Effect parseFilter(node, shape) {
        List<Effect> effectList = []
        if (node.filters) {
            node.filters.eachWithIndex { filter, i ->
                if (filter.DropShadowFilter) {
                    filter.DropShadowFilter.each { Node dropShadowNode ->
                        effectList.add(parseShadow(dropShadowNode))
                    }
                }
            }
            for (int i = 1 ; i < effectList.size() ; i++) {
                effectList[i].setInput(effectList[i-1])
            }
        }
        return effectList.isEmpty() ? null : effectList.last()
    }

    private Effect parseShadow(Node shadow) {
        Effect  effect
        double  angle       = Math.toRadians((shadow.@angle ?: 0).toDouble())
        String  colorString = (shadow.@color ?: '#000000')
        int     distance    = (shadow.@distance ?: 0).toDouble()
        double  alpha       = (shadow.@alpha ?: 1).toDouble() * lastShapeAlpha
        int     blurX       = (shadow.@blurX ?: 0).toDouble()
        int     blurY       = (shadow.@blurY ?: 0).toDouble()
        boolean inner       = (shadow.@inner ?: false)
        Color   color       = parseColor(colorString, alpha)
        double  offsetX     = distance * Math.cos(angle)
        double  offsetY     = distance * Math.sin(angle)
        double  minSize     = originalWidth < originalHeight ? originalWidth : originalHeight
        if (inner) {
            effect = new InnerShadow()
            effect.setOffsetX(offsetX * scaleFactorX)
            effect.setOffsetY(offsetY * scaleFactorY)
            effect.setRadius(blurX * 2 * scaleFactorX)
            effect.setColor(color)
            effect.setBlurType(BlurType.TWO_PASS_BOX)
        } else {
            effect = new DropShadow()
            effect.setOffsetX(offsetX * scaleFactorX)
            effect.setOffsetY(offsetY * scaleFactorY)
            effect.setRadius(blurX * 2 * scaleFactorX)
            effect.setColor(color)
            effect.setBlurType(BlurType.TWO_PASS_BOX)
        }
        return effect
    }

    private Color parseColor(node) {
        String color = (node.@color ?: '#000000')
        double alpha = (node.@alpha ?: 1).toDouble() * lastShapeAlpha
        return parseColor(color, alpha)
    }

    private Color parseColor(String color, double alpha) {
        assert color.size() == 7 || 9
        double red   = Integer.valueOf(color[1..2], 16).intValue() / 255
        double green = Integer.valueOf(color[3..4], 16).intValue() /255
        double blue  = Integer.valueOf(color[5..6], 16).intValue() / 255
        if (color.size() == 9) {
            alpha = Integer.valueOf(color[7..8], 16).intValue() / 255
        }
        return Color.color(red, green, blue, alpha)
    }

    private Paint convertLinearGradient(node) {
        def linearGradient     = node.LinearGradient[0]
        double x1              = (linearGradient.@x ?: 0).toDouble() * scaleFactorX
        double y1              = (linearGradient.@y ?: 0).toDouble() * scaleFactorY
        double scaleX          = (linearGradient.@scaleX ?: 0).toDouble()
        //double scaleY = (linearGradient.@scaleY ?: 1).toDouble()
        double rotation        = Math.toRadians((linearGradient.@rotation ?: 0).toDouble())
        double x2              = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double y2              = Math.sin(rotation) * scaleX * scaleFactorY + y1

        Stop[] stops           = convertGradientEntries(linearGradient.GradientEntry)

        LinearGradient gradient = new LinearGradient(x1 + offsetX, y1 + offsetY, x2 + offsetX, y2 + offsetY, false, CycleMethod.NO_CYCLE, stops)

        return gradient
    }

    private Paint convertRadialGradient(node) {
        def        radialGradient  = node.RadialGradient[0]
        double     x1              = (radialGradient.@x ?: 0).toDouble() * scaleFactorX
        double     y1              = (radialGradient.@y ?: 0).toDouble() * scaleFactorY
        double     scaleX          = (radialGradient.@scaleX ?: 0).toDouble()
        //double scaleY = (radialGradient.@scaleY ?: 0).toDouble()
        double     rotation        = Math.toRadians((radialGradient.@rotation ?: 0).toDouble())
        double     x2              = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double     y2              = Math.sin(rotation) * scaleX * scaleFactorY + y1
        Point2D    center          = new Point2D(x1, y1)
        Point2D    stop            = new Point2D(x2, y2)
        double     radius          = (center.distance(stop) / 2.0)

        List<Stop> stops           = convertGradientEntries(radialGradient.GradientEntry)
        RadialGradient gradient    = new RadialGradient(0, 0, center.x + offsetX, center.y + offsetY, radius, false, CycleMethod.NO_CYCLE, stops)

        return gradient
    }

    private List<Stop> convertGradientEntries(gradientEntries) {
        List<Stop> stops = []
        gradientEntries.each { def gradientEntry->
            double fraction = (gradientEntry.@ratio ?: 0).toDouble()
            double alpha    = (gradientEntry.@alpha ?: 1).toDouble() * lastShapeAlpha
            Color  color    = gradientEntry.@color == null ? Color.BLACK : parseColor(gradientEntry.@color, alpha)
            stops.add(new Stop(fraction, color))
        }
        return stops
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
        double x      = groupOffsetX * scaleFactorX
        double y      = groupOffsetY * scaleFactorY
        double width  = originalWidth * scaleFactorX
        double height = originalHeight * scaleFactorY

        final  Rectangle iBounds = new Rectangle(width, height)
        iBounds.setOpacity(0.0)
        iBounds.setStroke(null)
        group.getChildren().add(iBounds)

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
                        shape = parseRectangle(node)
                        offsetX = shape.layoutBounds.minX
                        offsetY = shape.layoutBounds.minY
                        group.getChildren().add(paintShape(node, shape))
                        break
                    case FXG.Ellipse:
                        shape = parseEllipse(node)
                        offsetX = shape.layoutBounds.minX
                        offsetY = shape.layoutBounds.minY
                        group.getChildren().add(paintShape(node, shape))
                        break
                    case FXG.Line:
                        shape = parseLine(node)
                        offsetX = shape.layoutBounds.minX
                        offsetY = shape.layoutBounds.minY
                        group.getChildren().add(paintShape(node, shape))
                        break
                    case FXG.Path:
                        offsetX = groupOffsetX
                        offsetY = groupOffsetY
                        shape = parsePath(node)
                        group.getChildren().add(paintShape(node, shape))
                        break
                    case FXG.RichText:
                        Text text = parseRichText(node)
                        group.getChildren().add(text)
                        break
                }
            }
        }

        return group
    }

    private void prepareParameters(def fxg, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        originalWidth  = (int)(fxg.@viewWidth ?: WIDTH).toDouble()
        originalHeight = (int)(fxg.@viewHeight ?: HEIGHT).toDouble()
        aspectRatio    = originalHeight / originalWidth

        if (originalWidth <= WIDTH && originalHeight <= HEIGHT) {
            previewWidth  = originalWidth
            previewHeight = originalHeight
        } else {
            if (originalWidth > originalHeight) {
                // wider than height
                previewWidth  = WIDTH
                previewHeight = KEEP_ASPECT ? WIDTH * aspectRatio : HEIGHT
            } else if (originalHeight > originalWidth) {
                // higher than wide
                previewWidth  = KEEP_ASPECT ? HEIGHT / aspectRatio : WIDTH
                previewHeight = HEIGHT
            } else {
                // square and bigger than preview area
                previewWidth  = WIDTH
                previewHeight = HEIGHT
            }
        }

        scaleFactorX   = previewWidth / originalWidth
        scaleFactorY   = previewHeight / originalHeight
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
