package cga.exercise.components.light

import cga.exercise.components.shader.ShaderProgram
import org.joml.*

// Übergabe der Winkel für Spotlight Radius/Größe
open class Spotlight(lightPos: Vector3f, lightCol: Vector3f, attParam: Vector3f = Vector3f(1.0f, 0.05f, 0.01f),
                     var angle: Vector2f = Vector2f(Math.toRadians(15.0f), Math.toRadians(180.0f))) : PointLight(lightPos, lightCol, attParam) {

    fun bind(shaderProgram: ShaderProgram, name: String, viewMatrix: Matrix4f) {
        super.bind(shaderProgram, name)
        shaderProgram.setUniform(name + "LightAngle", angle)
        shaderProgram.setUniform(name + "LightDir", getWorldZAxis().negate().mul(Matrix3f(viewMatrix)))
    }
}