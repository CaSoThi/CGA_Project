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
import kotlin.collections.ArrayList
import kotlin.math.sin


class Scene(private val window: GameWindow) {
    private val staticShader: ShaderProgram
    private val skyboxShader: ShaderProgram


    // anstatt dem "flachen" Ground gewölbtes Ground Object verwednen?
    private val resGround: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
    private val objMeshGround: OBJLoader.OBJMesh = resGround.objects[0].meshes[0]

    private var groundMesh: Mesh


    // anstatt des cycles den Character einfügen?
    private var cycleRend = ModelLoader.loadModel(
        "project/assets/Light Cycle/Light Cycle/HQ_Movie cycle.obj",
        Math.toRadians(90.0f),
        Math.toRadians(0.0f),
        Math.toRadians(90.0f)
    ) ?: throw IllegalArgumentException("Could not load the model")

    private var groundRend = Renderable()

    private var tCamera = TronCamera()

    //Anlegen des Pointlights
    private var light = PointLight(Vector3f(), Vector3f())
    private var spotlight = Spotlight(Vector3f(), Vector3f())

    //private var spotlight2 = Spotlight(Vector3f(), Vector3f())


    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var einbool: Boolean = false


    // Vertices und Indices der CubeMap festlegen
    var size: Float = 500.0f
    private var skyboxVertices: FloatArray = floatArrayOf(
        -size, -size, size,
        size, -size, size,
        size, -size, -size,
        -size, -size, -size,
        -size, size, size,
        size, size, size,
        size, size, -size,
        -size, size, -size
    )

    private var skyboxIndices: IntArray = intArrayOf(
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

    // Collectable list
    private var collectables : MutableList<Star>
    private val collectableAmount : Int = 10

    private var starMesh: Mesh
    private var starRend =  Renderable()


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
        val facesCubeMap: Vector<String> = Vector()
        facesCubeMap.addAll(
            listOf(
                "project/assets/textures/nz.png",
                "project/assets/textures/pz.png",
                "project/assets/textures/py.png",
                "project/assets/textures/ny.png",
                "project/assets/textures/px.png",
                "project/assets/textures/nx.png"
            )
        )

        cubeMapTexture = cubeMap.loadCubeMap(facesCubeMap)

        // ---------------------------------------------------------------------------------------


        //Erzeugen der Sphere Attribute
        val stride = 8 * 4
        val attrPos = VertexAttribute(3, GL_FLOAT, stride, 0)
        val attrTC = VertexAttribute(2, GL_FLOAT, stride, 3 * 4)
        val attrNorm = VertexAttribute(3, GL_FLOAT, stride, 5 * 4)

        val objVertexAttributes = arrayOf(attrPos, attrTC, attrNorm)


        //-------------------------------------Material--------------------------------------------
        val emitTex = Texture2D("project/assets/textures/4k_venus_atmosphere.jpg", true)
        val diffTex = Texture2D("project/assets/textures/4k_venus_atmosphere.jpg", true)
        val specTex = Texture2D("project/assets/textures/4k_venus_atmosphere.jpg", true)

        val groundMaterial = Material(diffTex, emitTex, specTex, 10.0f, Vector2f(1.0f))

        emitTex.setTexParams(
            GL_REPEAT,
            GL_REPEAT,
            GL11.GL_LINEAR_MIPMAP_LINEAR,
            GL_LINEAR
        )     //Linear = zwischen farbwerten interpolieren
        diffTex.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        specTex.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        groundMesh = Mesh(objMeshGround.vertexData, objMeshGround.indexData, objVertexAttributes, groundMaterial)

        groundRend.meshList.add(groundMesh)
        groundRend.scaleLocal(Vector3f(5.0f))
        cycleRend.scaleLocal(Vector3f(0.1f))
        //cycleRend.rotateLocal(Math.toRadians(0.0f), Math.toRadians(90.0f), Math.toRadians(0.0f))
        //     cycleRend.parent = groundRend
        cycleRend.setPosition(
            groundRend.getWorldPosition().x + 5,
            groundRend.getWorldPosition().y,
            groundRend.getWorldPosition().z
        )

        tCamera.parent = cycleRend

        tCamera.rotateLocal(Math.toRadians(90.0f), Math.toRadians(45.0f), Math.toRadians(-90.0f))
        tCamera.translateLocal(Vector3f(0.0f, 0.5f, 4.0f))



        //----------------------------------------Licht------------------------------------------------
        light = PointLight(tCamera.getWorldPosition(), Vector3f(1.0f))
        light.translateLocal(Vector3f(1.0f, -5.0f, 0.0f))

        light.parent = cycleRend


        // Spotlight mit Neigung in x und z Richtung
        spotlight = Spotlight(Vector3f(0.0f, 0.0f, 0.0f), Vector3f(0.0f))
        spotlight.rotateLocal(Math.toRadians(-90.0f), Math.PI.toFloat(), 0.0f)
        spotlight.parent = cycleRend
        //spotlight2 = Spotlight(Vector3f(0.0f, 2.0f, -2.0f), Vector3f(1.0f))
        spotlight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)
        //spotlight2.parent = cycleRend



        //-----------------------------------Collectables-------------------------------------------

        val resStar: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/Star.obj")
        val objStar: OBJLoader.OBJMesh = resStar.objects[0].meshes[0]
        val starEmit = Texture2D("project/assets/textures/StarColor3.png", true)
        val starDiff = Texture2D("project/assets/textures/StarColor3.png", true)
        val starSpec = Texture2D("project/assets/textures/StarColor3.png", true)

        val starMaterial = Material(starDiff, starEmit, starSpec, 40.0f, Vector2f(1.0f))

        starEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        starMesh = Mesh(objStar.vertexData, objStar.indexData, objVertexAttributes, starMaterial)

        collectables = mutableListOf()
        for (i in 1..collectableAmount) {
            starRend.meshList.add(starMesh)
            starRend.scaleLocal(Vector3f(0.8f))
            starRend.rotateLocal(0.0f, 0.28f, 0.0f)


            var starLight = PointLight(starRend.getWorldPosition(), Vector3f(0.0f, 0.0f, 1.0f))

            var star = Star(starLight, starRend)

            star.setPosition(groundRend.getWorldPosition().x + 5.1f,
                    groundRend.getWorldPosition().y ,
                    groundRend.getWorldPosition().z )

            starRend.translateLocal(Vector3f(1.0f, 10f, 0.0f))
            collectables.add(star)
        }
    }


    fun render(dt: Float, t: Float) {

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        // ------------Skybox rendern----------------------------
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
        //staticShader.setUniform("farbe", Vector3f(abs(sin(t)), abs(sin(t / 2)), abs(sin(t / 3))))

        staticShader.setUniform("farbe", Vector3f(1.0f))


        cycleRend.render(staticShader)
        light.bind(staticShader, "byklePoint")
        spotlight.bind(staticShader, "bykleSpot", tCamera.getCalculateViewMatrix())
        staticShader.setUniform("farbe", Vector3f(0.0f, 0.0f, 0.0f))
        groundRend.render(staticShader)


        //------------------collectables rendern----------------

        for (star in collectables) {

            // Collision Detection
            if (star.distance(cycleRend) < 0.2f){
                star.collect()
            }
            star.render(staticShader, "byklePoint")

        }

    }


    fun update(dt: Float, t: Float) {
        //Farbe des Motorads wird verändert in Abhängigkeit der Zeit mit sinuswerten

        //light.lightCol = Vector3f(abs(sin(t)), abs(sin(t / 2)), abs(sin(t / 3)))
        if (window.getKeyState(GLFW_KEY_W)) {
            cycleRend.rotateAroundPoint(0.0f, 0.0f, Math.toRadians(0.25f), groundRend.getWorldPosition())
        }
        if (window.getKeyState(GLFW_KEY_A)) {
            cycleRend.rotateAroundPoint(0.0f, Math.toRadians(0.25f), 0.0f, groundRend.getWorldPosition())
        }
        if (window.getKeyState(GLFW_KEY_D)) {
            cycleRend.rotateAroundPoint(0.0f, Math.toRadians(-0.25f), 0.0f, groundRend.getWorldPosition())
        }
        if (window.getKeyState(GLFW_KEY_S)) {
            cycleRend.rotateAroundPoint(0.0f, 0.0f, Math.toRadians(-0.25f), groundRend.getWorldPosition())
        }
        if(window.getKeyState(GLFW_KEY_SPACE)) {
            cycleRend.translateLocal(Vector3f(0.0f, 0.0f, 0.0f))
           // cycleRend.translateLocal(Vector3f(0.0f, 5.0f, 0.0f))
        }

        // Animate stars
        for (star in collectables) {
            star.rotate(dt/4)
        }
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {
        //Bewegung in x Richtung durch Differenz zwischen alter und neuer Position
        var deltaX: Double = xpos - oldMousePosX
        var deltaY: Double = ypos - oldMousePosY
        oldMousePosX = xpos
        oldMousePosY = ypos

        if (einbool) {
            tCamera.rotateAroundPoint(0.0f, Math.toRadians(deltaX.toFloat() * 0.05f), 0.0f, Vector3f(0.0f))
        }
        einbool = true

    }

    fun cleanup() {}
}
