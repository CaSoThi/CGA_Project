package cga.exercise.game

import cga.exercise.components.geometry.Renderable
import cga.exercise.components.shader.ShaderProgram
import cga.framework.ModelLoader
import org.joml.Math

class Animation(val path: String, val startNumber: Int, val endNumber: Int, var rotx: Float, var roty: Float, var rotz: Float) {

    val animationList = arrayListOf<Renderable?>()
    var currentFrame = startNumber
    var renderCycle = 0
    var movement = false

    init {
        for (i in startNumber .. endNumber) {
            animationList.add(ModelLoader.loadModel("${path}${i}.obj",
                    Math.toRadians(rotx), Math.toRadians(roty), Math.toRadians(rotz)))
        }
    }

    fun render(shaderProgram: ShaderProgram, dt: Float) {
        if (movement) {
            animationList[currentFrame]?.render(shaderProgram)
        }
        else {
            animationList[5]?.render(shaderProgram)
        }
    }

    fun update() {
        if (renderCycle == 0) {
            currentFrame++
        } else if (renderCycle >= 3) {
            renderCycle = -1
        }
        renderCycle++
        if (currentFrame > endNumber) {
            currentFrame = startNumber
        }
    }

    fun setParent(p: Renderable) {
        for (m in animationList) {
            m?.parent = p
        }
    }

    fun rotateLocal(pitch: Float, yaw: Float, roll: Float) {
        for (m in animationList) {
            m?.rotateLocal(pitch, yaw, roll)
        }
    }
}