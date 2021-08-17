package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram

open class Renderable (val meshList : MutableList<Mesh> = mutableListOf()) : Transformable(), IRenderable{

    override fun render(shaderProgram: ShaderProgram) {

        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false)

        for (i in meshList){
            i.render(shaderProgram)
        }

    }

}