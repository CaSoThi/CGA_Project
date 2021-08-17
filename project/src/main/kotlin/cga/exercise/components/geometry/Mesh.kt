package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram
import cga.framework.GLError
import org.lwjgl.opengl.*
import org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray
import org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer

/**
 * Creates a Mesh object from vertexdata, intexdata and a given set of vertex attributes
 *
 * @param vertexdata plain float array of vertex data
 * @param indexdata  index data
 * @param attributes vertex attributes contained in vertex data
 * @throws Exception If the creation of the required OpenGL objects fails, an exception is thrown
 *
 * Created by Fabian on 16.09.2017.
 */
class Mesh(vertexdata: FloatArray, indexdata: IntArray, attributes: Array<VertexAttribute>,
           private var material : Material ?= null) {
    //private data
    // IDs erzeugen
    private var vao = glGenVertexArrays()
    private var vbo = glGenBuffers()
    private var ibo = glGenBuffers()
    private var indexcount = 0




    init {

        // ID aktivieren
        glBindVertexArray(vao)

        //Buffer wird aktiviert
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)

        //Daten in Buffer schreiben
        glBufferData(GL_ARRAY_BUFFER, vertexdata, GL_STATIC_DRAW); GLError.checkThrow()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexdata, GL_STATIC_DRAW); GLError.checkThrow()

        indexcount = indexdata.size

        //Vertex Attribute spezifizieren
        for (i in attributes.indices) {
            //Vertexattribut am Index i aktivieren
            glEnableVertexAttribArray(i)
            glVertexAttribPointer(i, attributes[i].n, attributes[i].type, false, attributes[i].stride, (attributes[i].offset).toLong())
        }

        glBindVertexArray(0)

    }

        /**
         * renders the mesh
         */
        fun render() {

            glBindVertexArray(vao)
            glDrawElements(GL11.GL_TRIANGLES, indexcount, GL_UNSIGNED_INT, 0)
            glBindVertexArray(0)

        }


        fun render(shaderProgram: ShaderProgram) {
            material?.bind(shaderProgram)
            render()

        }

        /**
         * Deletes the previously allocated OpenGL objects for this mesh
         */
        fun cleanup() {
            if (ibo != 0) GL15.glDeleteBuffers(ibo)
            if (vbo != 0) GL15.glDeleteBuffers(vbo)
            if (vao != 0) GL30.glDeleteVertexArrays(vao)
        }
    }
