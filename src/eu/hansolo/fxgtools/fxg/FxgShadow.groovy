package eu.hansolo.fxgtools.fxg

import javafx.geometry.Point2D
import javafx.scene.paint.Color

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 13:40
 */
class FxgShadow extends FxgFilter{
    FxgFilterType type     = FxgFilterType.SHADOW
    boolean       inner
    int           angle
    int           distance
    double        alpha
    double        alphaDouble
    double        blurX
    double        blurY
    Color         color

    Point2D getOffset() {
        return new Point2D(distance * Math.cos(Math.toRadians(-angle)), distance * Math.sin(Math.toRadians(angle)))
    }
}
