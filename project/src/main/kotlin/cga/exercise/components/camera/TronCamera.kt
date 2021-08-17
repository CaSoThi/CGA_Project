package cga.exercise.components.camera

import cga.exercise.components.geometry.Transformable
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Math


open class TronCamera(val fov : Float = Math.toRadians(90.0f), val asp : Float = 16.0f/9.0f,
                          val near : Float = 0.1f, val far : Float = 100.0f) : Transformable(), ICamera{




    override fun getCalculateViewMatrix(): Matrix4f {
        /*
    * Calculate the ViewMatrix according the lecture
    * values needed:
    *  - eye –> the position of the camera
    *  - center –> the point in space to look at
    *  - up –> the direction of 'up'
    */
        val eye = getWorldPosition()
        val center = getWorldPosition().sub(getWorldZAxis())
        val up = getWorldYAxis()

        return  Matrix4f().lookAt(eye, center, up)

    }
    override fun getCalculateProjectionMatrix(): Matrix4f {
        /*
     * Calculate the ProjectionMatrix according the lecture
     * values needed:
     *  - fov – the vertical field of view in radians (must be greater than zero and less than PI)
     *  - aspect – the aspect ratio (i.e. width / height; must be greater than zero)
     *  - zNear – near clipping plane distance
     *  - zFar – far clipping plane distance
     */
        return Matrix4f().perspective(fov, asp, near, far)

    }

    override fun bind(shader: ShaderProgram) {
        shader.setUniform("view_matrix", getCalculateViewMatrix(), false)
        shader.setUniform("proj_matrix", getCalculateProjectionMatrix(), false)
    }

}