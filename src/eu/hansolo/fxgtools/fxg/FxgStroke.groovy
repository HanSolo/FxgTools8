package eu.hansolo.fxgtools.fxg

import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 09:20
 * To change this template use File | Settings | File Templates.
 */
class FxgStroke {
    String         name
    Color          color
    StrokeLineCap  cap
    StrokeLineJoin join
    float          width

    String getHexColor() {
        return Integer.toHexString((int) (color).RGB & 0x00ffffff)
    }
}
