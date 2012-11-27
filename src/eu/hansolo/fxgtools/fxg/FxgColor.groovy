package eu.hansolo.fxgtools.fxg

import javafx.scene.paint.Color

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:02
 * To change this template use File | Settings | File Templates.
 */
class FxgColor extends FxgFill {
    FxgFillType type  = FxgFillType.SOLID_COLOR
    double      alpha
    Color       color

    String getHexColor() {
        return Color.web(color.toString(), alpha)
    }
}
