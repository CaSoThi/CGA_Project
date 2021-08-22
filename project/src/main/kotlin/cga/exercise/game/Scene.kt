package cga.exercise.game


import cga.exercise.components.camera.TronCamera
import cga.exercise.components.geometry.*
import cga.exercise.components.light.PointLight
import cga.exercise.components.light.SpotLight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.CubemapTexture
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader
import cga.framework.OBJLoader
import org.joml.*
import org.lwjgl.opengl.GL11
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL30.*
import java.util.*
import kotlin.math.pow
import kotlin.random.Random;


class Scene(private val window: GameWindow) {
    private val staticShader: ShaderProgram
    private val skyboxShader: ShaderProgram
    private val toonShader: ShaderProgram

    private var shaderInUse: ShaderProgram

    // anstatt dem "flachen" Ground gewölbtes Ground Object verwednen?
    private val resGround: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
    private val objMeshGround: OBJLoader.OBJMesh = resGround.objects[0].meshes[0]

    private var groundMesh: Mesh


    // anstatt des cycles den Character einfügen?
    private var character = ModelLoader.loadModel(
        "project/assets/models/character.obj",
        Math.toRadians(180.0f),
        Math.toRadians(90.0f),
        Math.toRadians(90.0f)
    ) ?: throw IllegalArgumentException("Could not load the model")

    private var planet = Renderable()

    private var tCamera = TronCamera()

    //Anlegen des Pointlights
    private var light = PointLight(Vector3f(), Vector3f())
    private var spotlight = SpotLight(Vector3f(), Vector3f())

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

    private var yspeed = 0.0f
    private var canJump = true

    // Collectable list
    private var collectables: MutableList<Star>
    private val collectableAmount: Int = 20
    private var score: Int = 0

    private var finalStarRend = Renderable()
    private val finalStarMesh: Mesh
    private var finalStarLight = PointLight(Vector3f(), Vector3f())
    private var finalStar: Star

    //Background Objects
    private var saturnMesh: Mesh
    private var saturnRend = Renderable()

    private var neptuneMesh: Mesh
    private var neptuneRend = Renderable()

    private var earthMesh: Mesh
    private var earthRend = Renderable()

    private var ufoMesh: Mesh
    private var ufoRend = Renderable()


    //scene setup
    init {
        staticShader = ShaderProgram("project/assets/shaders/tron_vert.glsl", "project/assets/shaders/tron_frag.glsl")
        skyboxShader = ShaderProgram("project/assets/shaders/skyBoxVert.glsl", "project/assets/shaders/skyBoxFrag.glsl")
        toonShader = ShaderProgram("project/assets/shaders/tron_vert.glsl", "project/assets/shaders/toon_frag.glsl")

        // Default to static shader
        shaderInUse = staticShader

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
        //Erzeugen der Vertex Attribute
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

        planet.meshList.add(groundMesh)
        planet.scaleLocal(Vector3f(5.0f))
        character.scaleLocal(Vector3f(0.2f))
        //cycleRend.rotateLocal(Math.toRadians(0.0f), Math.toRadians(90.0f), Math.toRadians(0.0f))
        //     cycleRend.parent = groundRend
        character.setPosition(
            planet.getWorldPosition().x + 5.1f,
            planet.getWorldPosition().y,
            planet.getWorldPosition().z
        )

        tCamera.parent = character

        tCamera.rotateLocal(Math.toRadians(90.0f), Math.toRadians(45.0f), Math.toRadians(-90.0f))
        tCamera.translateLocal(Vector3f(0.0f, 0.5f, 1.0f))


        //----------------------------------------Licht------------------------------------------------
        light = PointLight(tCamera.getWorldPosition(), Vector3f(1.0f))
        light.translateLocal(Vector3f(1.0f, -5.0f, 0.0f))

        light.parent = character


        // Spotlight mit Neigung in x und z Richtung
        spotlight = SpotLight(Vector3f(0.0f, 0.0f, 0.0f), Vector3f(0.0f))
        spotlight.rotateLocal(Math.toRadians(-90.0f), Math.PI.toFloat(), 0.0f)
        spotlight.parent = character
        //spotlight2 = Spotlight(Vector3f(0.0f, 2.0f, -2.0f), Vector3f(1.0f))
        spotlight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)
        //spotlight2.parent = cycleRend

        //-----------------------------------Collectables-------------------------------------------

        val starEmit = Texture2D("project/assets/textures/StarColor3.png", true)
        val starDiff = Texture2D("project/assets/textures/StarColor3.png", true)
        val starSpec = Texture2D("project/assets/textures/StarColor3.png", true)

        starEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        var starMaterial = Material(starDiff, starEmit, starSpec, 40.0f, Vector2f(1.0f))


        collectables = mutableListOf()
        for (i in 0..collectableAmount) {
            val resStar: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/Star2.obj")
            val objStar: OBJLoader.OBJMesh = resStar.objects[0].meshes[0]

            val starMesh = Mesh(objStar.vertexData, objStar.indexData, objVertexAttributes, starMaterial)
            var starRend = Renderable()

            starRend.meshList.add(starMesh)

            starRend.scaleLocal(Vector3f(0.1f))
            //starRend.rotateLocal(0.0f, 2.8f, 0.0f)

            var starLight = PointLight(starRend.getWorldPosition(), Vector3f(0.6f))
            //starLight.translateGlobal(Vector3f(0.0f, 0.0f, 5.0f))
            starLight.parent = starRend


            var star = Star(starLight, starRend, starMaterial)

            star.setPosition(
                planet.getWorldPosition().x + 5.1f,
                planet.getWorldPosition().y,
                planet.getWorldPosition().z
            )

            val randomX = Random.nextFloat() * 360f
            val randomY = Random.nextFloat() * 360f

            star.rotateAroundPoint(0.0f, randomX, randomY, planet.getWorldPosition())

            collectables.add(star)
        }

        // Stern der erscheint, nachdem alle anderen Collectables eingesammelt wurden
        val resFinalStar: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/Star2.obj")
        val objFinalStar: OBJLoader.OBJMesh = resFinalStar.objects[0].meshes[0]
        finalStarMesh = Mesh(objFinalStar.vertexData, objFinalStar.indexData, objVertexAttributes, starMaterial)
        finalStarRend = Renderable()
        finalStarRend.scaleLocal(Vector3f(0.3f))
        finalStarRend.rotateLocal(0.0f, 0.9f, 0.0f)
        finalStarRend.meshList.add(finalStarMesh)
        finalStarLight = PointLight(finalStarRend.getWorldPosition(), Vector3f(1f))
        finalStarLight.parent = finalStarRend
        finalStarLight.translateLocal(Vector3f(1.0f, -1.0f, 1.0f))
        finalStar = Star(finalStarLight, finalStarRend, starMaterial)
        finalStar.setPosition(
            planet.getWorldPosition().x + 5.22f,
            planet.getWorldPosition().y,
            planet.getWorldPosition().z
        )
        val randomX = Random.nextFloat() * 360f
        val randomY = Random.nextFloat() * 360f
        finalStar.rotateAroundPoint(0.0f, randomX, randomY, planet.getWorldPosition())


        //-----------------------Background Objects--------------------------------------------------
        val resSaturn: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/saturn3.obj")
        val objMeshSaturn: OBJLoader.OBJMesh = resSaturn.objects[0].meshes[0]

        val saturnEmit = Texture2D("project/assets/textures/2k_saturn.jpg", true)
        val saturnDiff = Texture2D("project/assets/textures/2k_saturn.jpg", true)
        val saturnSpec = Texture2D("project/assets/textures/2k_saturn.jpg", true)

        val saturnMaterial = Material(saturnDiff, saturnEmit, saturnSpec, 10.0f, Vector2f(1.0f))

        saturnEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        saturnDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        saturnSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        saturnMesh = Mesh(objMeshSaturn.vertexData, objMeshSaturn.indexData, objVertexAttributes, saturnMaterial)

        saturnRend.meshList.add(saturnMesh)
        saturnRend.scaleLocal(Vector3f(2.0f))
        saturnRend.translateGlobal(Vector3f(0.0f, 15.0f, 10.0f))
        saturnRend.rotateLocal(0.0f, 0.0f, 2.0f)


        val resNeptune: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
        val objMeshNeptune: OBJLoader.OBJMesh = resNeptune.objects[0].meshes[0]

        val neptuneEmit = Texture2D("project/assets/textures/2k_neptune.jpg", true)
        val neptuneDiff = Texture2D("project/assets/textures/2k_neptune.jpg", true)
        val neptuneSpec = Texture2D("project/assets/textures/2k_neptune.jpg", true)

        val neptuneMaterial = Material(neptuneDiff, neptuneEmit, neptuneSpec, 10.0f, Vector2f(1.0f))

        neptuneEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        neptuneDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        neptuneSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        neptuneMesh = Mesh(objMeshNeptune.vertexData, objMeshNeptune.indexData, objVertexAttributes, neptuneMaterial)

        neptuneRend.meshList.add(neptuneMesh)
        neptuneRend.scaleLocal(Vector3f(2.0f))
        neptuneRend.translateGlobal(Vector3f(0.0f, -10.0f, -8.0f))
        neptuneRend.rotateLocal(0.0f, 0.0f, 2.0f)


        val resEarth: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
        val objMeshEarth: OBJLoader.OBJMesh = resEarth.objects[0].meshes[0]

        val earthEmit = Texture2D("project/assets/textures/2k_earth_daymap.jpg", true)
        val earthDiff = Texture2D("project/assets/textures/2k_earth_daymap.jpg", true)
        val earthSpec = Texture2D("project/assets/textures/2k_earth_daymap.jpg", true)

        val earthMaterial = Material(earthDiff, earthEmit, earthSpec, 10.0f, Vector2f(1.0f))

        earthEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        earthDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        earthSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        earthMesh = Mesh(objMeshEarth.vertexData, objMeshEarth.indexData, objVertexAttributes, earthMaterial)

        earthRend.meshList.add(earthMesh)
        earthRend.scaleLocal(Vector3f(2.0f))
        earthRend.translateGlobal(Vector3f(-14.0f, -5.0f, 6.0f))
        //earthRend.rotateLocal(0.0f, 0.0f, 2.0f)


        val resUfo: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/ufo.obj")
        val objMeshUfo: OBJLoader.OBJMesh = resUfo.objects[0].meshes[0]

        val ufoEmit = Texture2D("project/assets/textures/ufo.jpg", true)
        val ufoDiff = Texture2D("project/assets/textures/ufo.jpg", true)
        val ufoSpec = Texture2D("project/assets/textures/ufo.jpg", true)

        val ufoMaterial = Material(ufoDiff, ufoEmit, ufoSpec, 10.0f, Vector2f(1.0f))

        ufoEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ufoDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ufoSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        ufoMesh = Mesh(objMeshUfo.vertexData, objMeshUfo.indexData, objVertexAttributes, ufoMaterial)

        ufoRend.meshList.add(ufoMesh)
        ufoRend.scaleLocal(Vector3f(0.5f))
        ufoRend.translateGlobal(Vector3f(14.0f, -6.0f, -12.0f))
        ufoRend.rotateLocal(0.0f, 0.0f, 3.0f)


        // shadow mapping
        /*
        val depthMapFrameBuffer = glGenFramebuffers()
        val shadowWidth = 1024
        val shadowHeight = 1024
        val depthMap : Int = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, depthMap)
        GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowWidth, shadowHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glViewport(0, 0, shadowWidth, shadowHeight)
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFrameBuffer)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0)
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glClear(GL_DEPTH_BUFFER_BIT)
        val near_plane = 1.0f.toDouble()
        val far_plane = 7.5f.toDouble()
        val lightProjection = glOrtho(-10.0f.toDouble(), 10.0f.toDouble(), -10.0f.toDouble(), 10.0f.toDouble(), near_plane, far_plane)
        val lightView = Matrix4f().lookAt(Vector3f(-2.0f, 4.0f, -1.0f),Vector3f( 0.0f, 0.0f,  0.0f), Vector3f( 0.0f, 1.0f,  0.0f))
        //val lightSpaceMatrix : Matrix4f = lightView.mul(lightProjection)

         */
    }


    fun render(dt: Float, t: Float) {

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        // -----------------------Skybox rendern----------------------------
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

        //---------------------andere Sachen rendern-----------------------

        shaderInUse.use()
        tCamera.bind(shaderInUse)
        //staticShader.setUniform("farbe", Vector3f(abs(sin(t)), abs(sin(t / 2)), abs(sin(t / 3))))
        shaderInUse.setUniform("farbe", Vector3f(1.0f))
        character.render(shaderInUse)
        light.bind(shaderInUse, "byklePoint")
        spotlight.bind(shaderInUse, "bykleSpot", tCamera.getCalculateViewMatrix())
        shaderInUse.setUniform("farbe", Vector3f(0.0f, 0.0f, 0.0f))
        planet.render(shaderInUse)

        //-----------------Background Objekte rendern---------------------
        shaderInUse.use()
        shaderInUse.setUniform("farbe", Vector3f(0.5f))
        saturnRend.render(shaderInUse)
        neptuneRend.render(shaderInUse)
        earthRend.render(shaderInUse)
        ufoRend.render(shaderInUse)


        //------------------collectables rendern-------------------------
        for (i in 0 until collectableAmount) {
            collectables[i].render(shaderInUse, "byklePoint")
        }

        if (score >= collectableAmount) {
            finalStar.render(shaderInUse, "byklePoint")
        }
    }


    fun update(dt: Float, t: Float) {
        if (yspeed == 0f) {
            if (window.getKeyState(GLFW_KEY_W)) {
                character.rotateAroundPoint(0.0f, 0.0f, Math.toRadians(0.25f), planet.getWorldPosition())
            }
            if (window.getKeyState(GLFW_KEY_A)) {
                character.rotateAroundPoint(0.0f, Math.toRadians(-0.25f), 0.0f, planet.getWorldPosition())
            }
            if (window.getKeyState(GLFW_KEY_D)) {
                character.rotateAroundPoint(0.0f, Math.toRadians(0.25f), 0.0f, planet.getWorldPosition())
            }
            if (window.getKeyState(GLFW_KEY_S)) {
                character.rotateAroundPoint(0.0f, 0.0f, Math.toRadians(-0.25f), planet.getWorldPosition())
            }
        }


        // Handle shader switching
        if (window.getKeyState(GLFW_KEY_1)) {
            shaderInUse = staticShader
        }
        if (window.getKeyState(GLFW_KEY_2)) {
            shaderInUse = toonShader
        }


        // Check if char is not jumping and fell into planet


        if (checkPlayerPos()) {
            character.setPosition(
                planet.getWorldPosition().x + 5.1f,
                character.y(),
                character.z()
            )
        }
        /*if(character.y() > 0.04f) {
            character.setPosition(character.x(), 0.0f, character.z())
        }*/

        //Jumping

        //println(yspeed)
        if (checkCollisionWithPlanet()) {
            println("COLLISION")
            yspeed = 0.0f
            canJump = true


        }
        if (window.getKeyState(GLFW_KEY_SPACE) && canJump) {
            println("PRESSED")
            canJump = false;
            yspeed = -0.04f;
        }
        if (!canJump) {
            yspeed += 0.001f;

            character.setPosition(character.x(), character.y() + yspeed, character.z())
            println(character.y())
            if (yspeed > 0.04) {
                yspeed = 0.04f

            }

        }


        // Animate stars & check collision
        for (star in collectables) {
            // Collision Detection
            if (star.distance(character) < 0.2f) {
                if (star.collect()) {
                    score++
                    println(score)
                }
            }
            star.rotate(dt)
        }

        if (finalStar.distance(character) < 0.3f && score >= collectableAmount) {
            if (finalStar.collect()) {
                println("Du hast gewonnen! Man bist du krass!")
                
            }
        }

        //Background Objects
        ufoRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        earthRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        neptuneRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        saturnRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
    }

    fun pointDistance3d(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float =
        Math.sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2) + (z2 - z1).pow(2))

    fun checkCollisionWithPlanet(): Boolean {
        val collisionMin = 5.1f;
        val collisionMax = 5.101f
        val currentDifference =
            pointDistance3d(planet.x(), planet.y(), planet.z(), character.x(), character.y() + yspeed, character.z())
        //println(currentDifference)
        // println(collision)
        if (currentDifference <= collisionMax) {
            return true;
        }
        return false;
    }

    fun checkPlayerPos(): Boolean {
        val collision = 5.09f;
        val currentDifference =
            pointDistance3d(planet.x(), planet.y(), planet.z(), character.x(), character.y(), character.z())
        //println(currentDifference)
        // println(collision)
        if (currentDifference <= collision ) {
            return true;
        }
        return false;
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {
        /*//Bewegung in x Richtung durch Differenz zwischen alter und neuer Position
        var deltaX: Double = xpos - oldMousePosX
        var deltaY: Double = ypos - oldMousePosY
        oldMousePosX = xpos
        oldMousePosY = ypos

        if (einbool) {
            tCamera.rotateAroundPoint(Math.toRadians(deltaX.toFloat() * 0.05f), 0.0f, 0.0f, Vector3f(0.0f))
        }
        einbool = true
    */
    }

    fun cleanup() {}
}