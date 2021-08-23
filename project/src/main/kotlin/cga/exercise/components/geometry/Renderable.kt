package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram
import org.joml.Math

open class Renderable (val meshList : MutableList<Mesh> = mutableListOf()) : Transformable(), IRenderable{

    //Get Position information
    fun x() = this.getWorldPosition().x
    fun y() = this.getWorldPosition().y
    fun z() = this.getWorldPosition().z



    override fun render(shaderProgram: ShaderProgram) {

        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false)

        for (i in meshList){
            i.render(shaderProgram)
        }

    }

}