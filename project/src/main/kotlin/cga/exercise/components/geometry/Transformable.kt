package cga.exercise.components.geometry

import org.joml.*
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.pow
import kotlin.math.sqrt


open class Transformable(var parent : Transformable? = null) : ITransformable{

    var modelMat = Matrix4f()

    /**
     * Rotates object around its own origin.
     * @param pitch radiant angle around x-axis ccw
     * @param yaw radiant angle around y-axis ccw
     * @param roll radiant angle around z-axis ccw
     */
    override fun rotateLocal(pitch: Float, yaw: Float, roll: Float){
        modelMat.rotateXYZ(pitch, yaw, roll)
    }

    /**
     * Rotates object around given rotation center.
     * @param pitch radiant angle around x-axis ccw
     * @param yaw radiant angle around y-axis ccw
     * @param roll radiant angle around z-axis ccw
     * @param altMidpoint rotation center
     */
    override fun rotateAroundPoint(pitch: Float, yaw: Float, roll: Float, altMidpoint: Vector3f){
        var temp = Matrix4f()

        temp.translate(altMidpoint)
        temp.rotateXYZ(pitch, yaw, roll)
        temp.translate(Vector3f(altMidpoint).negate())

        modelMat = temp.mul(modelMat)
    }

    /**
     * Translates object based on its own coordinate system.
     * @param deltaPos delta positions
     */
    override fun translateLocal(deltaPos: Vector3f){
        modelMat.translate(deltaPos)
    }

    /**
     * Translates object based on its parent coordinate system.
     * Hint: global operations will be left-multiplied
     * @param deltaPos delta positions (x, y, z)
     */
    override fun translateGlobal(deltaPos: Vector3f){
        var temp = Matrix4f()
        modelMat = temp.translate(deltaPos).mul(modelMat)
    }

    /**
     * Scales object related to its own origin
     * @param scale scale factor (x, y, z)
     */
    override fun scaleLocal(scale: Vector3f){
        modelMat.scale(scale)
    }

    /**
     * Returns position based on aggregated translations.
     * Hint: last column of model matrix
     * @return position
     */
    override fun getPosition(): Vector3f{
        return Vector3f(modelMat.m30(), modelMat.m31(), modelMat.m32())
    }

    /**
     * Returns position based on aggregated translations incl. parents.
     * Hint: last column of world model matrix
     * @return position
     */
    override fun getWorldPosition(): Vector3f{
        var world = getWorldModelMatrix()
        return Vector3f(world.m30(), world.m31(), world.m32())
    }

    /**
     * Returns x-axis of object coordinate system
     * Hint: first normalized column of model matrix
     * @return x-axis
     */
    override fun getXAxis(): Vector3f{
        return Vector3f(modelMat.m00(),modelMat.m01(), modelMat.m02()).normalize()
    }

    /**
     * Returns y-axis of object coordinate system
     * Hint: second normalized column of model matrix
     * @return y-axis
     */
    override fun getYAxis(): Vector3f{
        return Vector3f(modelMat.m10(),modelMat.m11(), modelMat.m12()).normalize()
    }

    /**
     * Returns z-axis of object coordinate system
     * Hint: third normalized column of model matrix
     * @return z-axis
     */
    override fun getZAxis(): Vector3f{
        return Vector3f(modelMat.m20(), modelMat.m21(), modelMat.m22()).normalize()
       // return Vector3f(modelMat.m02(), modelMat.m12(), modelMat.m22())
    }

    /**
     * Returns x-axis of world coordinate system
     * Hint: first normalized column of world model matrix
     * @return x-axis
     */
    override fun getWorldXAxis(): Vector3f{
        var worldMat = getWorldModelMatrix()
        return Vector3f(worldMat.m00(),worldMat.m01(), worldMat.m02()).normalize()
      //  return (Vector3f(worldMat.m00(),worldMat.m10(), worldMat.m20()))
    }

    /**
     * Returns y-axis of world coordinate system
     * Hint: second normalized column of world model matrix
     * @return y-axis
     */
    override fun getWorldYAxis(): Vector3f{
        var worldMat = getWorldModelMatrix()
        return Vector3f(worldMat.m10(),worldMat.m11(), worldMat.m12()).normalize()
        //return Vector3f(worldMat.m01(),worldMat.m11(), worldMat.m21())
    }

    /**
     * Returns z-axis of world coordinate system
     * Hint: third normalized column of world model matrix
     * @return z-axis
     */
    override fun getWorldZAxis(): Vector3f{
        var worldMat = getWorldModelMatrix()
        return Vector3f(worldMat.m20(), worldMat.m21(), worldMat.m22()).normalize()
    }

    /**
     * Returns multiplication of world and object model matrices.
     * Multiplication has to be recursive for all parents.
     * Hint: scene graph
     * @return world modelMatrix
     */
    override fun getWorldModelMatrix(): Matrix4f{
        var worldMat = getLocalModelMatrix()
        parent?.getWorldModelMatrix()?.mul(modelMat, worldMat)
        return worldMat
    }

    /**
     * Returns object model matrix
     * @return modelMatrix
     */
    override fun getLocalModelMatrix(): Matrix4f = Matrix4f(modelMat)


}



