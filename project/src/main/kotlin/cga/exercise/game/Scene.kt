
package cga.exercise.game


import cga.exercise.components.camera.TronCamera
import cga.exercise.components.geometry.*
import cga.exercise.components.light.PointLight
import cga.exercise.components.light.Spotlight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.CubemapTexture
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader
import cga.framework.OBJLoader
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15.*
import kotlin.math.abs
import org.joml.Math
import org.joml.Vector2f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.AMDSeamlessCubemapPerTexture.GL_TEXTURE_CUBE_MAP_SEAMLESS
import org.lwjgl.opengl.ARBTextureStorage.glTexStorage2D
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import org.lwjgl.stb.STBImage
import java.nio.IntBuffer
import java.util.*
import kotlin.math.sin


/**
 * Created by Fabian on 16.09.2017.
 */
class Scene(private val window: GameWindow) {
    private val staticShader: ShaderProgram
    private val skyboxShader : ShaderProgram


    // anstatt dem "flachen" Ground gewölbtes Ground Object verwednen?
    private val resGround : OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
    private val objMeshGround : OBJLoader.OBJMesh = resGround.objects[0].meshes[0]

    private var groundMesh : Mesh


    // anstatt des cycles den Character einfügen?
    private var cycleRend = ModelLoader.loadModel("project/assets/Light Cycle/Light Cycle/HQ_Movie cycle.obj", Math.toRadians(-90.0f),Math.toRadians(90.0f), 0.0f) ?: throw IllegalArgumentException("Could not load the model")

    private var groundRend = Renderable()

    private var tCamera  = TronCamera()

    //Anlegen des Pointlights
    private var light = PointLight(Vector3f(), Vector3f())
    private var spotlight = Spotlight(Vector3f(), Vector3f())

    private var spotlight2 = Spotlight(Vector3f(), Vector3f())


    private var oldMousePosX : Double = -1.0
    private var oldMousePosY : Double = -1.0
    private var einbool : Boolean = false


    // Vertices und Indices der CubeMap festlegen
    var size : Float = 500.0f
    private var skyboxVertices : FloatArray  = floatArrayOf(
            -size, -size, size,
            size, -size, size,
            size, -size, -size,
            -size, -size, -size,
            -size, size, size,
            size, size, size,
            size, size, -size,
            -size, size, -size)

    private var skyboxIndices : IntArray = intArrayOf(
            //right
            1, 2, 6,
            6, 5, 1,
            //left
            0, 4, 7,
            7, 3, 0,
            //top
            4, 5, 6,
            6, 7, 4,
            //bottom
            0, 3, 2,
            2, 1, 0,
            //back
            0, 1, 5,
            5, 4, 0,
            //front
            3, 7, 6,
            6, 2, 3
    )

    private var cubeMap = CubemapTexture(skyboxVertices, skyboxIndices)

    var cubeMapTexture = glGenTextures()

    //scene setup
    init {
        staticShader = ShaderProgram("project/assets/shaders/tron_vert.glsl", "project/assets/shaders/tron_frag.glsl")
        skyboxShader = ShaderProgram("project/assets/shaders/skyBoxVert.glsl", "project/assets/shaders/skyBoxFrag.glsl")


        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); GLError.checkThrow()
        glDisable(GL_CULL_FACE); GLError.checkThrow()
        //glEnable(GL_CULL_FACE)
        glFrontFace(GL_CCW)
        glCullFace(GL_BACK)
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()

        //-------------------------------------CubeMap--------------------------------------------

        // Einzelne Faces der CubeMap laden
        val facesCubeMap : Vector<String> = Vector()
        facesCubeMap.addAll(listOf("project/assets/textures/nz.png",
                                    "project/assets/textures/pz.png",
                                    "project/assets/textures/py.png",
                                    "project/assets/textures/ny.png",
                                    "project/assets/textures/px.png",
                                    "project/assets/textures/nx.png"))

        cubeMapTexture = cubeMap.loadCubeMap(facesCubeMap)

        // ---------------------------------------------------------------------------------------


        //Erzeugen der Sphere Attribute
        val stride = 8 * 4
        val attrPos = VertexAttribute(3, GL_FLOAT, stride, 0)
        val attrTC = VertexAttribute(2, GL_FLOAT, stride, 3*4)
        val attrNorm = VertexAttribute(3, GL_FLOAT, stride, 5*4)

        val objVertexAttributes = arrayOf(attrPos, attrTC, attrNorm)


        //-------------------------------------Material--------------------------------------------
        val emitTex : Texture2D = Texture2D("project/assets/textures/ground_emit.png", true)
        val diffTex : Texture2D = Texture2D("project/assets/textures/ground_diff.png", true)
        val specTex : Texture2D = Texture2D("project/assets/textures/ground_spec.png", true)

        val groundMaterial = Material(diffTex, emitTex, specTex, 60.0f, Vector2f(64.0f,64.0f))

        emitTex.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)     //Linear = zwischen farbwerten interpolieren
        diffTex.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        specTex.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)



        groundMesh = Mesh(objMeshGround.vertexData, objMeshGround.indexData, objVertexAttributes, groundMaterial)

        groundRend.meshList.add(groundMesh)
        groundRend.scaleLocal(Vector3f(2.0f))
        cycleRend.scaleLocal(Vector3f(0.8f))

        cycleRend.parent = groundRend



        tCamera.parent = cycleRend


        tCamera.rotateLocal(Math.toRadians(-10.0f), 0.0f, 0.0f)
        tCamera.translateLocal(Vector3f(0.0f, 0.5f, 4.0f))


        //----------------------------------------Licht------------------------------------------------
        light = PointLight(tCamera.getWorldPosition(), Vector3f(1f,1f,0f))
        light.parent = cycleRend


        // Spotlight mit Neigung in x und z Richtung
        spotlight = Spotlight(Vector3f(0.0f, 0.0f, -2.0f), Vector3f(1.0f))
        spotlight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)
        spotlight.parent = cycleRend

        spotlight2 = Spotlight(Vector3f(0.0f, 2.0f, -2.0f), Vector3f(1.0f))
        spotlight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)

        spotlight2.parent = cycleRend

    }


    fun render(dt: Float, t: Float) {

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        // Skybox rendern
        glDepthFunc(GL_LEQUAL)
        skyboxShader.use()

        skyboxShader.setUniform("view", tCamera.getCalculateViewMatrix(), false)
        skyboxShader.setUniform("projection", tCamera.getCalculateProjectionMatrix(), false)

        glBindVertexArray(cubeMap.skyboxVAO)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_CUBE_MAP, cubeMapTexture)
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0)

        //skyboxShader.setUniform("skybox", cubeMapTexture)
        glBindVertexArray(0);
        glDepthFunc(GL_LESS);

        // andere Sachen rendern
        staticShader.use()
        tCamera.bind(staticShader)
        staticShader.setUniform("farbe", Vector3f(abs(sin(t)),abs(sin(t/2)),abs(sin(t/3))))
        cycleRend.render(staticShader)
        light.bind(staticShader, "byklePoint")
        spotlight.bind(staticShader, "bykleSpot", tCamera.getCalculateViewMatrix())
        staticShader.setUniform("farbe", Vector3f(0.0f,0.0f,0.0f))
        groundRend.render(staticShader)
    }


    fun update(dt: Float, t: Float) {
        //Farbe des Motorads wird verändert in Abhängigkeit der Zeit mit sinuswerten

        light.lightCol = Vector3f(abs(sin(t)),abs(sin(t/2)),abs(sin(t/3)))
        if(window.getKeyState(GLFW_KEY_W)){
            cycleRend.translateLocal(Vector3f(0.0f, 0.0f, -5*dt))
            if(window.getKeyState(GLFW_KEY_A)){
                cycleRend.rotateLocal(0.0f, 2f*dt, 0.0f)
            }
            if(window.getKeyState(GLFW_KEY_D)){
                cycleRend.rotateLocal(0.0f, -2f*dt, 0.0f)
            }
        }
        if(window.getKeyState(GLFW_KEY_S)){
            cycleRend.translateLocal(Vector3f(0.0f, 0.0f, 5*dt))
            if(window.getKeyState(GLFW_KEY_A)){
                cycleRend.rotateLocal(0.0f, 2f*dt, 0.0f)
            }
            if(window.getKeyState(GLFW_KEY_D)){
                cycleRend.rotateLocal(0.0f, -2f*dt, 0.0f)
            }
        }

    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {
        //Bewegung in x Richtung durch Differenz zwischen alter und neuer Position
        var deltaX : Double = xpos - oldMousePosX
        var deltaY : Double = ypos - oldMousePosY
        oldMousePosX = xpos
        oldMousePosY = ypos

        if(einbool){
            tCamera.rotateAroundPoint(0.0f, Math.toRadians(deltaX.toFloat()*0.05f), 0.0f, Vector3f(0.0f))
        }
        einbool = true

    }

    fun cleanup() {}
}
