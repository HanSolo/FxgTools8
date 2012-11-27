package eu.hansolo.fxgtools.fxg

import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:03
 * To change this template use File | Settings | File Templates.
 */
class FxgLinearGradient extends FxgFill {
    FxgFillType type      = FxgFillType.LINEAR_GRADIENT
    double      startX
    double      startY
    double      endX
    double      endY
    List<Stop>  stops


    LinearGradient getLinearGradient() {
        return new LinearGradient(startX, startY, endX, endY, false, CycleMethod.NO_CYCLE, stops)
    }
}
