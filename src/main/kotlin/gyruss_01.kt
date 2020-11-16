import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.noise.*
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.IntVector2
import org.openrndr.shape.contour
import kotlin.math.cos
import kotlin.math.sin
import opensimplex.OpenSimplex2S
import org.openrndr.draw.DepthTestPass
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.math.Vector3
import org.openrndr.shape.ShapeContour
import org.openrndr.ffmpeg.ScreenRecorder

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

class circularNoiseSweep(val seed: Int, val frames: Int, val radius: Double=1000.0){
    // Gets us a noise value at a position in 2-space which loops through a large
    // loop to allow for a continous seamless loop of noise in time.

    fun valueAt(frame: Int, x: Double, y: Double): Double{
        // We'll assume that we just rotate the X,Y around the Y axis for simplicity, and
        // we'll offset the x,y,z positions by 2x radius into positive space to prevent
        // issues with the usable range of noise.
        val calcangle = (frame.toDouble()/frames)*2*Math.PI
//        println("Frame: $frame, calcAngle: ${calcangle*180/Math.PI}")
        val noiseX = 0.0 +//(2*radius) + // offset it
                (x+radius)*cos(calcangle)
        val noiseY = y //2*radius + y // Simple eh? Just offset to be sure it's in pos space
        val noiseZ = 0.0 + // (2*radius) + // offset it
                (x+radius)*sin(calcangle)

        return simplex(seed, noiseX, noiseY, noiseZ)
    }
}


fun endCapLoop(width: Int, height: Int, radius: Double, zPos: Double,
               frames: Int, frameCount: Int,
               circlePoints: Int, noiseSweep: circularNoiseSweep): List<Vector3> {

    val pradians = (2 * Math.PI) / circlePoints
    val points =
        (0..(circlePoints - 1)).map { seg->
            val theta = seg * 2 * Math.PI / circlePoints
            val xp = cos(theta) * 8.0
            val yp = sin(theta) * 8.0
            val rad = radius + ((radius / 10) * noiseSweep.valueAt(frameCount, xp, yp))
            val angle = (seg / circlePoints) * 2 * Math.PI
            val x = (rad) * cos(seg * pradians) + width / 2
            val y = (rad) * sin(seg * pradians) + height / 2
            Vector3(x, y, zPos)
        }
    return points
}

fun main() = application {
    configure {
        width = 1024
        height = 1024
        position = IntVector2(20, 20)
    }
//    oliveProgram {
    program {

        // We are going to use a 3d noise field and spin a circle around an offset point in space to generate
        // a loop of noise so that we can make this animated in gif form. Imagine holding a hula hoop at arms
        // length and spinning in place in a circle.

        val circlePoints = 512
        val frames = 60
        val radius = 400.0
        val pradians = (2*Math.PI)/circlePoints
        val noiseSweep = circularNoiseSweep(1, frames, 0.0)
//        val simplex = OpenSimplex2S(12L)//opensimplex.OpenSimplex2S(12)

/*
        extend(ScreenRecorder()) {
            profile = GIFProfile()
        }
*/
        extend {
            drawer.perspective(45.0, width * 1.0 / height, 0.0, 1200.0)
            drawer.lookAt(Vector3(1000+width/2.0, 1000+height/2.0, -1000.0), Vector3(width/2.0, height/2.0, 500.0), Vector3.UNIT_Y)
            drawer.depthWrite = true
            drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL


            // drawer.clear(ColorRGBa.WHITE)
            drawer.stroke = ColorRGBa(0.5, 0.8, 0.8, 0.25)
            drawer.strokeWeight = 8.0
            drawer.fill = ColorRGBa(0.5, 0.5, 0.5, 0.25)

            drawer.lineStrip(
                endCapLoop(width, height, radius, 1000.0,
                    frameCount+frames, frameCount,
                    circlePoints, noiseSweep))

            drawer.lineStrip(
                endCapLoop(width, height, radius, 0.0,
                    frameCount, frameCount,
                    circlePoints, noiseSweep))
            drawer.stroke = ColorRGBa(0.8, 0.5, 0.8, 0.5)
/*
            val c = contour {
            */
            for(seg in 0 until (circlePoints) step 8) {
                val theta = seg * 2 * Math.PI / circlePoints
                val xp = cos(theta) * 8.0
                val yp = sin(theta) * 8.0
                val rad = radius + ((radius / 10) * noiseSweep.valueAt(frameCount, xp, yp))
                val angle = (seg / circlePoints) * 2 * Math.PI
                val x = (rad) * cos(seg * pradians) + width / 2
                val y = (rad) * sin(seg * pradians) + height / 2

//                    moveOrLineTo(x , y)

//                    for(i in frameCount until (frameCount+frames)){
                drawer.lineStrip((frameCount until (frameCount + frames)).map { frameno ->
                    val rad2 = radius + ((radius / 10) * noiseSweep.valueAt(frameno, xp, yp))
                    val z = 1000.0 * ((frameno - frameCount).toDouble() / frames)
                    Vector3(
                        (rad2) * cos(seg * pradians) + width / 2,
                        (rad2) * sin(seg * pradians) + height / 2,
                        z
                    )
                })
            }
/*

                    // Draw line segments from z 0 until z 1000, split evenly
                    // into #framecount chunks, each one with it's radius modified along the way.
                    // to match what we have as the upcoming radius at that point.
//                    val rad2 = radius + ((radius/10) * noiseSweep.valueAt(i, xp, yp))
                }
                //drawer.lineSegment(Vector3(x, y, 0.0), Vector3(x, y, 1000.0))
//                    println("Lined to $x,$y")
                close()
            }
*/

            if (frameCount == frames) {
                //application.exit()
            }


//            drawer.contour(c)
        }
    }
}