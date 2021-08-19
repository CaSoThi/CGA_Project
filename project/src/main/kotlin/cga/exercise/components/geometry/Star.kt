package cga.exercise.components.geometry

import cga.exercise.components.light.PointLight
import cga.exercise.components.shader.ShaderProgram
import org.joml.Math
import org.joml.Vector3f
import kotlin.math.pow
import kotlin.math.sqrt

class Star(light : PointLight, collectableObject: Renderable) {
    private var collectableObject : Renderable
    private var pointLight: PointLight
    private var collected : Boolean

    init {
        this.collectableObject = collectableObject
        this.pointLight = light
        this.pointLight.parent = collectableObject
        this.collected = false
    }

    fun render(shader : ShaderProgram, name : String) {
        if (!collected) {
            collectableObject.render(shader)
            pointLight.bind(shader, name)
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

    fun collect() {
        this.collected = true

    }
}