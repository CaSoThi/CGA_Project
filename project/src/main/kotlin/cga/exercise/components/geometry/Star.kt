package cga.exercise.components.geometry

import cga.exercise.components.camera.TronCamera
import cga.exercise.components.light.PointLight
import cga.exercise.components.light.SpotLight
import cga.exercise.components.shader.ShaderProgram
import org.joml.Math
import org.joml.Vector3f
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class Star(light : PointLight, collectableObject: Renderable, material: Material) {
    private var collectableObject : Renderable
    private var pointLight: PointLight
    private var collected : Boolean
    private var material : Material


    init {
        this.collectableObject = collectableObject
        this.pointLight = light
        this.collected = false
        this.material = material

    }

    fun render(shader : ShaderProgram, name : String) {
        if (!collected) {
            shader.use()
            shader.setUniform("farbe", Vector3f(1.0f))
            pointLight.bind(shader, name)
            collectableObject.render(shader)
        }
    }

    fun distance(other : Renderable) : Float {
        val pos1 = other.getWorldPosition()
        val pos2 = this.collectableObject.getWorldPosition()

        val distance = sqrt(
                   (pos1.x() - pos2.x()).toDouble().pow(2.0) +
                   (pos1.y() - pos2.y()).toDouble().pow(2.0) +
                   (pos1.z() - pos2.z()).toDouble().pow(2.0))

        return distance.toFloat()
    }

     fun setPosition(x: Float, y: Float, z: Float) {
        this.collectableObject.setPosition(x, y, z)
        this.pointLight.setPosition(x, y + 1f, z)
    }

    fun rotate(amount: Float) {
      collectableObject.rotateLocal(amount, 0.0f, 0.0f)
    }

    fun rotateAroundPoint(pitch: Float, yaw: Float, roll: Float, altMidpoint: Vector3f) {
        this.collectableObject.rotateAroundPoint(pitch, yaw, roll, altMidpoint)
        // When taking the center of the planet as the center:
        // pitch:
        // yaw: left / right from player
        // roll: front / back from player
    }

    fun collect() : Boolean {
        return if (collected) {
            false
        } else {
            this.collected = true
            true
        }
    }
}