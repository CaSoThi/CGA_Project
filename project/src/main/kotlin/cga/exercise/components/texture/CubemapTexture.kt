package cga.exercise.components.texture

import cga.exercise.components.geometry.Transformable
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.AMDSeamlessCubemapPerTexture
import org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImage
import kotlin.collections.ArrayList

class CubemapTexture(var vertices: FloatArray, var indices: IntArray): Transformable() {
    var skyboxVAO = GL30.glGenVertexArrays()
    var skyboxVBO = GL30.glGenBuffers()
    var skyboxIBO = GL30.glGenBuffers()
    var cubeMaptexID = GL30.glGenTextures()


    fun loadCubeMap(faces: ArrayList<String>) : Int {

        // Binding VAO, VBO and IBO
        glBindVertexArray(skyboxVAO)
        glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, skyboxIBO)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)
        glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
        glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindTexture(GL30.GL_TEXTURE_CUBE_MAP, cubeMaptexID)

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
        glEnable(AMDSeamlessCubemapPerTexture.GL_TEXTURE_CUBE_MAP_SEAMLESS)


        // Load a Face for each side of the cubemap (6 sides) to create the textured skybox
        for(i in 0..5) {
            val x = BufferUtils.createIntBuffer(1)
            val y = BufferUtils.createIntBuffer(1)
            val readChannels = BufferUtils.createIntBuffer(1)
            val imageData = STBImage.stbi_load(faces[i], x, y, readChannels, 4)
                    ?: throw Exception("Image file \"" + faces[i] + "\" couldn't be read:\n" + STBImage.stbi_failure_reason())
            STBImage.stbi_set_flip_vertically_on_load(false)

            GL30.glTexImage2D(GL30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL30.GL_RGBA8, x.get(), y.get(), 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, imageData)
            STBImage.stbi_image_free(imageData)
        }
        return cubeMaptexID
    }

}