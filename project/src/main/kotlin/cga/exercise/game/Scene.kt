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
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.pow
import kotlin.random.Random
import kotlin.system.exitProcess


class Scene(private val window: GameWindow) {
    private val staticShader: ShaderProgram
    private val skyboxShader: ShaderProgram
    private val toonShader: ShaderProgram
    private val negativeShader: ShaderProgram

    private var shaderInUse: ShaderProgram

    private val resGround: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/planet.obj")
    private val objMeshGround: OBJLoader.OBJMesh = resGround.objects[0].meshes[0]

    private var groundMesh: Mesh

    private var character: Animation
    private var player = ModelLoader.loadModel(
        "project/assets/character/char_0.obj", Math.toRadians(180.0f),
        Math.toRadians(-90.0f),
        Math.toRadians(90.0f)
    )

    //Checks character rotation during orthographic camera
    private var turnedCharacterBack = false
    private var turnedCharacterForth = true

    private var planet = Renderable()

    private var tCamera = TronCamera()

    private var turnedCamera = false

    // Counts translations of camera during zoom
    private var turnedCamCounter = 0

    // Define lights
    private var light = PointLight(Vector3f(), Vector3f())
    private var spotlight = SpotLight(Vector3f(), Vector3f())

    private var collectedAllStars = false
    private var pressedEnter = false

    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var einbool: Boolean = false

    // Define Vertices and Indices of Cubemap
    private var size: Float = 500.0f
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
    private var cubeMapTexture = glGenTextures()


    private var direction = 0.0f
    private var difference: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)

    // Collectable list
    private var collectables: MutableList<Star>
    private val collectableAmount: Int = 20
    private var score: Int = 0

    private var finalStar: Star

    //Obstacles
    private var obstacles: MutableList<Renderable>
    private var obstacleAmount = 20

    // Background Objects
    private var saturnRend: Renderable
    private var neptuneRend: Renderable
    private var earthRend: Renderable
    private var ufoRend: Renderable

    // Jumpingvaria
    private var jumpSpeed = 0f
    private var jumpDirection = false // False = going up, True = going down
    private var canJump = true


    // Scene setup
    init {

        // GAME INSTRUCTION
        println("Welcome to your space adventure!\n")
        println("-----------------------------Controls-----------------------------")
        println("Press 'S' oder 'W' to move upwards or downwards the planet.")
        println("Press 'SPACE' to jump.")
        println("Press 'F1' or 'F2' to change the camera perspective.")
        println("Press '1', '2' or '3' to switch between shaders.\n")
        println("----------------------------How to play----------------------------\n")
        println("Try to get all the stars and finish by collecting the big star!")
        println("Have fun!")
        println("--------------------------------Start------------------------------\n")



        staticShader = ShaderProgram("project/assets/shaders/tron_vert.glsl", "project/assets/shaders/tron_frag.glsl")
        skyboxShader = ShaderProgram("project/assets/shaders/skyBoxVert.glsl", "project/assets/shaders/skyBoxFrag.glsl")
        toonShader = ShaderProgram("project/assets/shaders/toon_vert.glsl", "project/assets/shaders/toon_frag.glsl")
        negativeShader =
            ShaderProgram("project/assets/shaders/tron_vert.glsl", "project/assets/shaders/negative_frag.glsl")

        // Default to static shader
        shaderInUse = staticShader

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); GLError.checkThrow()
        glDisable(GL_CULL_FACE); GLError.checkThrow()
        glFrontFace(GL_CCW)
        glCullFace(GL_BACK)
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()

        //-------------------------------------CubeMap--------------------------------------------

        // Loading Cubemap faces
        val facesCubeMap: ArrayList<String> = arrayListOf()
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

        // --------------------------------Creating Vertex Attributes---------------------------------
        val stride = 8 * 4
        val attrPos = VertexAttribute(3, GL_FLOAT, stride, 0)
        val attrTC = VertexAttribute(2, GL_FLOAT, stride, 3 * 4)
        val attrNorm = VertexAttribute(3, GL_FLOAT, stride, 5 * 4)

        val objVertexAttributes = arrayOf(attrPos, attrTC, attrNorm)

        //-------------------------------------Material------------------------------------------------
        val emitTex = Texture2D("project/assets/textures/grass.PNG", true)
        val diffTex = Texture2D("project/assets/textures/grass.PNG", true)
        val specTex = Texture2D("project/assets/textures/grass.PNG", true)

        val groundMaterial = Material(diffTex, emitTex, specTex, 1.0f, Vector2f(20.0f))

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
        player?.scaleLocal(Vector3f(0.02f))
        player?.setPosition(
            planet.getWorldPosition().x + 5.1f,
            planet.getWorldPosition().y,
            planet.getWorldPosition().z
        )

        character = Animation("project/assets/character/char_", 0, 19, 0f, 180f, 0f)
        character.setParent(player!!)

        character.rotateLocal(
            Math.toRadians(-90.0f),
            Math.toRadians(180.0f),
            Math.toRadians(90.0f)
        )

        tCamera.parent = player

        tCamera.rotateLocal(Math.toRadians(90.0f), Math.toRadians(50.0f), Math.toRadians(-90.0f))
        tCamera.translateLocal(Vector3f(0.0f, 0.5f, 15.0f))

        //----------------------------------------Light------------------------------------------------
        light = PointLight(tCamera.getWorldPosition(), Vector3f(2.0f))
        light.translateLocal(Vector3f(0.0f, -10.0f, 0.0f))

        light.parent = player
        //light.parent = tCamera

        // Spotlight mit Neigung in x und z Richtung
        spotlight = SpotLight(Vector3f(0.0f, 0.0f, 0.0f), Vector3f(0.0f))
        spotlight.rotateLocal(Math.toRadians(-90.0f), Math.PI.toFloat(), 0.0f)
        spotlight.parent = tCamera
        spotlight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)

        //-----------------------------------Collectables-------------------------------------------

        val starEmit = Texture2D("project/assets/textures/StarColor3.png", true)
        val starDiff = Texture2D("project/assets/textures/StarColor3.png", true)
        val starSpec = Texture2D("project/assets/textures/StarColor3.png", true)

        starEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        starSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val starMaterial = Material(starDiff, starEmit, starSpec, 40.0f, Vector2f(1.0f))


        collectables = mutableListOf()
        for (i in 0 until collectableAmount) {
            val resStar: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/Star2.obj")
            val objStar: OBJLoader.OBJMesh = resStar.objects[0].meshes[0]

            val starMesh = Mesh(objStar.vertexData, objStar.indexData, objVertexAttributes, starMaterial)
            val starRend = Renderable()

            starRend.meshList.add(starMesh)

            starRend.scaleLocal(Vector3f(0.1f))

            val starLight = PointLight(starRend.getWorldPosition(), Vector3f(0.6f))
            starLight.parent = starRend


            val star = Star(starLight, starRend, starMaterial)

            star.setPosition(
                planet.getWorldPosition().x + 5.1f,
                planet.getWorldPosition().y,
                planet.getWorldPosition().z
            )
            if (i % 2 == 0) {
                star.translate(Vector3f(2.0f, 0.0f, 0.0f))
            }


            star.rotateAroundPoint(0.0f, (i + 1) * PI.toFloat(), (i + 1).toFloat(), planet.getWorldPosition())

            collectables.add(star)
        }

        // Final star appears once all others are collected
        val resFinalStar: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/Star2.obj")
        val objFinalStar: OBJLoader.OBJMesh = resFinalStar.objects[0].meshes[0]
        val finalStarMesh = Mesh(objFinalStar.vertexData, objFinalStar.indexData, objVertexAttributes, starMaterial)
        val finalStarRend = Renderable()
        finalStarRend.scaleLocal(Vector3f(0.3f))
        finalStarRend.rotateLocal(9.2f, 0.9f, 0.0f)
        finalStarRend.meshList.add(finalStarMesh)
        val finalStarLight = PointLight(finalStarRend.getWorldPosition(), Vector3f(1f))
        finalStarLight.parent = finalStarRend
        finalStar = Star(finalStarLight, finalStarRend, starMaterial)
        finalStar.setPosition(
            planet.getWorldPosition().x + 5.4f,
            planet.getWorldPosition().y,
            planet.getWorldPosition().z
        )


        //-----------------------Obstacle Objects---------------------------------------------------

        obstacles = mutableListOf()
        for (i in 0 until obstacleAmount) {
            val barrier = OBJLoader.loadOBJ("project/assets/models/Barrier.obj")
            val objBarrierMesh = barrier.objects[0].meshes[0]

            val barrierEmit = Texture2D("project/assets/textures/barrier1NM.png", true)
            val barrierDiff = Texture2D("project/assets/textures/barrier1Diffuse.png", true)
            val barrierSpec = Texture2D("project/assets/textures/barrier1Specular.png", true)

            val barrierMaterial = Material(barrierDiff, barrierEmit, barrierSpec, 10.0f, Vector2f(1.0f))

            barrierEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
            barrierDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
            barrierSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

            val barrierMesh =
                Mesh(objBarrierMesh.vertexData, objBarrierMesh.indexData, objVertexAttributes, barrierMaterial)

            val barrierRend = Renderable()
            barrierRend.meshList.add(barrierMesh)
            barrierRend.scaleLocal(Vector3f(0.2f))
            barrierRend.setPosition(
                planet.getWorldPosition().x + 5.1f,
                planet.getWorldPosition().y,
                planet.getWorldPosition().z
            )
            barrierRend.rotateLocal(Math.toRadians(90.0f), 0.0f, Math.toRadians(-90.0f))
            barrierRend.rotateAroundPoint(
                0.0f,
                (i + 1) * PI.toFloat(),
                (i + 1).toFloat(),
                planet.getWorldPosition()
            )
            obstacles.add(barrierRend)
        }


        //-----------------------Background Objects--------------------------------------------------
        val resSaturn: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/saturn2.obj")
        val objMeshSaturn: OBJLoader.OBJMesh = resSaturn.objects[0].meshes[0]

        val saturnEmit = Texture2D("project/assets/textures/2k_saturn.jpg", true)
        val saturnDiff = Texture2D("project/assets/textures/2k_saturn.jpg", true)
        val saturnSpec = Texture2D("project/assets/textures/2k_saturn.jpg", true)

        val saturnMaterial = Material(saturnDiff, saturnEmit, saturnSpec, 10.0f, Vector2f(1.0f))

        saturnEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        saturnDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        saturnSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val saturnMesh = Mesh(objMeshSaturn.vertexData, objMeshSaturn.indexData, objVertexAttributes, saturnMaterial)

        saturnRend = Renderable()
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

        val neptuneMesh =
            Mesh(objMeshNeptune.vertexData, objMeshNeptune.indexData, objVertexAttributes, neptuneMaterial)

        neptuneRend = Renderable()
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

        val earthMesh = Mesh(objMeshEarth.vertexData, objMeshEarth.indexData, objVertexAttributes, earthMaterial)

        earthRend = Renderable()
        earthRend.meshList.add(earthMesh)
        earthRend.scaleLocal(Vector3f(2.0f))
        earthRend.translateGlobal(Vector3f(-14.0f, -5.0f, 6.0f))


        val resUfo: OBJLoader.OBJResult = OBJLoader.loadOBJ("project/assets/models/ufo.obj")
        val objMeshUfo: OBJLoader.OBJMesh = resUfo.objects[0].meshes[0]

        val ufoEmit = Texture2D("project/assets/textures/ufo.jpg", true)
        val ufoDiff = Texture2D("project/assets/textures/ufo.jpg", true)
        val ufoSpec = Texture2D("project/assets/textures/ufo.jpg", true)

        val ufoMaterial = Material(ufoDiff, ufoEmit, ufoSpec, 10.0f, Vector2f(1.0f))

        ufoEmit.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ufoDiff.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ufoSpec.setTexParams(GL_REPEAT, GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val ufoMesh = Mesh(objMeshUfo.vertexData, objMeshUfo.indexData, objVertexAttributes, ufoMaterial)

        ufoRend = Renderable()
        ufoRend.meshList.add(ufoMesh)
        ufoRend.scaleLocal(Vector3f(0.5f))
        ufoRend.translateGlobal(Vector3f(14.0f, -6.0f, -12.0f))
        ufoRend.rotateLocal(0.0f, 0.0f, 3.0f)
    }


    fun render(dt: Float, t: Float) {

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        // -----------------------rendering Skybox----------------------------
        glDepthFunc(GL_LEQUAL)
        skyboxShader.use()

        skyboxShader.setUniform("view", tCamera.getCalculateViewMatrix(), false)
        skyboxShader.setUniform("projection", tCamera.getCalculateProjectionMatrix(), false)

        glBindVertexArray(cubeMap.skyboxVAO)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_CUBE_MAP, cubeMapTexture)
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0)

        glBindVertexArray(0);
        glDepthFunc(GL_LESS);

        //-----------------rendering Camera, Light, Character and Planet-----------------------

        shaderInUse.use()
        tCamera.bind(shaderInUse)
        shaderInUse.setUniform("farbe", Vector3f(1.0f))
        light.bind(shaderInUse, "Point")
        spotlight.bind(shaderInUse, "Spot", tCamera.getCalculateViewMatrix())
        shaderInUse.setUniform("farbe", Vector3f(0.4f))
        planet.render(shaderInUse)
        character.render(shaderInUse, dt)
        shaderInUse.setUniform("farbe", Vector3f(0.0f))

        //---------------------rendering obstacles--------------------------------------------
        for (i in 0 until obstacleAmount) {
            if (i % 2 == 0) {
                obstacles[i].render(shaderInUse)
            }
        }


        //----------------------rendering Background Objects-----------------------------------

        shaderInUse.setUniform("farbe", Vector3f(0.4f))
        saturnRend.render(shaderInUse)
        neptuneRend.render(shaderInUse)
        earthRend.render(shaderInUse)
        ufoRend.render(shaderInUse)


        //------------------rendering Collectables---------------------------------------------
        for (i in 0 until collectableAmount) {
            collectables[i].render(shaderInUse, "Point")
        }
        if (score >= collectableAmount) {
            finalStar.render(shaderInUse, "Point")
        }
    }

    fun lengthdir_x(length: Float, dir: Float): Float =
        (length * (Math.cos(dir.toDouble()))).toFloat() // Calculates point based on Direction and length - X

    fun lengthdir_z(length: Float, dir: Float): Float =
        (length * (-Math.sin(dir.toDouble()))).toFloat() // Calculates point based on Direction and length - Y

    fun update(dt: Float, t: Float) {
        //-----------------------Player Movement on planet--------------------------------------

        character.update()

        // Character is not moving
        if (!window.getKeyState(GLFW_KEY_S) && !window.getKeyState(GLFW_KEY_W)) {
            character.movement = false
        }

        if (collectedAllStars) {

            if (window.getKeyState(GLFW_KEY_ENTER)) {
                pressedEnter = true
                tCamera.translateGlobal(
                    Vector3f(
                        difference.x() / 20 * turnedCamCounter,
                        difference.y() / 20 * turnedCamCounter,
                        difference.z() / 20 * turnedCamCounter
                    ).negate()
                )
                collectedAllStars = false
            }
        } else {
            if (jumpSpeed == 0f) {
                if (checkCollisionWithObstacles()) {
                    //Disables movement
                } else {
                    if (window.getKeyState(GLFW_KEY_W)) {

                        character.movement = true
                        direction -= 0.2f
                        player!!.setPosition(
                            lengthdir_x(5.1f, Math.toRadians(direction)),
                            lengthdir_z(5.1f, Math.toRadians(direction)),
                            lengthdir_z(5.1f, 0.0f)
                        )
                        player!!.rotateLocal(0.0f, 0.0f, Math.toRadians(0.2f))
                        if (turnedCamera) {
                            if (!turnedCharacterForth && turnedCharacterBack) {
                                character.rotateLocal(
                                    Math.toRadians(0.0f),
                                    Math.toRadians(-180.0f),
                                    Math.toRadians(0.0f)
                                )
                                turnedCharacterForth = true
                                turnedCharacterBack = false
                            }
                        } else {
                            turnedCharacterForth = true

                        }
                    } else if (window.getKeyState(GLFW_KEY_S)) {

                        character.movement = true
                        direction += 0.2f
                        player!!.setPosition(
                            lengthdir_x(5.1f, Math.toRadians(direction)),
                            lengthdir_z(5.1f, Math.toRadians(direction)),
                            lengthdir_z(5.1f, 0.0f)
                        )
                        player!!.rotateLocal(0.0f, 0.0f, Math.toRadians(-0.2f))

                        if (turnedCamera) {
                            if (!turnedCharacterBack && turnedCharacterForth) {
                                character.rotateLocal(
                                    Math.toRadians(0.0f),
                                    Math.toRadians(180.0f),
                                    Math.toRadians(0.0f)
                                )
                                turnedCharacterBack = true
                                turnedCharacterForth = false

                            }
                        } else {

                            // Wenn aus orthographischer Kamera in perspektivische gewechselt wird und character nach hinten schaut, soll er nach vorne gedreht werden
                            if (turnedCharacterBack) {
                                character.rotateLocal(
                                    Math.toRadians(0.0f),
                                    Math.toRadians(-180.0f),
                                    Math.toRadians(0.0f)
                                )
                            }
                            turnedCharacterBack = false
                        }

                    }
                }

            }
        }

        if (!turnedCamera) {
            if (window.getKeyState(GLFW_KEY_F1)) {
                tCamera.translateLocal(Vector3f(20.0f, 8f, -10.0f))
                tCamera.rotateLocal(Math.toRadians(-45.0f), Math.toRadians(90.0f), Math.toRadians(90.0f))
                turnedCamera = true
            }
        } else {
            if (window.getKeyState(GLFW_KEY_F2)) {
                tCamera.rotateLocalBack(
                    Math.toRadians(45.0f),
                    Math.toRadians(-90.0f),
                    Math.toRadians(-90.0f)
                )
                tCamera.translateLocal(Vector3f(20.0f, 8f, -10.0f).negate())
                turnedCamera = false
            }
        }


        //---------------------------Jump Handling------------------------------------------------

        // Player is on the ground
        if (checkCollisionWithPlanet()) {
            jumpSpeed = 0f
            canJump = true
            jumpDirection = false
        }

        // Player is on the ground and presses space
        if (window.getKeyState(GLFW_KEY_SPACE) && canJump) {
            canJump = false
            character.movement = false
            jumpSpeed = -0.015f
        }

        // Player is airborne
        if (!canJump) {
            jumpSpeed += (if (jumpDirection) -1 else 1) * 0.0005f


            // Calculate jumping vector
            var jumpingVector = Vector3f(
                planet.x() - player!!.x(),
                planet.y() - player!!.y(),
                planet.z() - player!!.z()
            )

            jumpingVector = jumpingVector.mul(jumpSpeed)

            val oldCharacterPosition = player!!.getWorldPosition()
            val newCharacterPosition = oldCharacterPosition.add(jumpingVector)
            player!!.setPosition(newCharacterPosition.x(), newCharacterPosition.y(), newCharacterPosition.z())

            if (jumpSpeed > 0.015) {
                jumpDirection = true

            }

        }

        //------------------Animate stars & check for player collision-------------------------------
        for (star in collectables) {
            if (star.distance(player!!) < 0.2f) {
                if (star.collect()) {
                    score++
                    println("Collected $score/$collectableAmount")
                }
            }
            star.rotate(dt)
        }



        if (score >= collectableAmount && !pressedEnter && !collectedAllStars) {
            println("You got all stars! Now catch the big one!")
            collectedAllStars = true
        }


        if (collectedAllStars) {
            if (difference.x == 0.0f && difference.y == 0.0f && difference.z == 0.0f) {

                difference = Vector3f(
                    finalStar.x() - tCamera.getWorldPosition().x,
                    finalStar.y() - tCamera.getWorldPosition().y,
                    finalStar.z() - tCamera.getWorldPosition().z
                )

            }

            if (pointDistance3d(
                    tCamera.getWorldPosition().x,
                    tCamera.getWorldPosition().y,
                    tCamera.getWorldPosition().z,
                    finalStar.x(),
                    finalStar.y(),
                    finalStar.z()
                ) > 1.06
            ) {

                tCamera.translateGlobal(Vector3f(difference.x() / 20, difference.y() / 20, difference.z() / 20))
                turnedCamCounter++
            }
        }


        if (finalStar.distance(player!!) < 0.3f && score >= collectableAmount) {
            if (finalStar.collect()) {
                println("\nCongrats! You won!")

            }
        }

        //--------------------Movement of Background Objects-------------------------
        ufoRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        ufoRend.rotateLocal(0.0f, dt * 5, 0.0f)
        earthRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        neptuneRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())
        saturnRend.rotateAroundPoint(dt / 20, 0.0f, 0.0f, planet.getWorldPosition())


        //---------------------Handle shader switching--------------------------------
        if (window.getKeyState(GLFW_KEY_1)) {
            shaderInUse = staticShader
        }
        if (window.getKeyState(GLFW_KEY_2)) {
            shaderInUse = toonShader
        }
        if (window.getKeyState(GLFW_KEY_3)) {
            shaderInUse = negativeShader
        }
    }

    fun pointDistance3d(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float =
        Math.sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2) + (z2 - z1).pow(2))

    fun checkCollisionWithPlanet(): Boolean {
        // val collisionMin = 5.1f
        val collisionMax = 5.101f
        val currentDifference =
            pointDistance3d(
                planet.x(),
                planet.y(),
                planet.z(),
                player!!.x(),
                player!!.y() + jumpSpeed,
                player!!.z()
            )
        if (currentDifference <= collisionMax) {
            return true
        }
        return false
    }

    fun checkCollisionWithObstacles(): Boolean {
        for (i in 0 until obstacleAmount) {
            if (i % 2 == 0) {
                val currentDiff = pointDistance3d(
                    player!!.x(),
                    player!!.y(),
                    player!!.z(),
                    obstacles[i].x(),
                    obstacles[i].y(),
                    obstacles[i].z()
                )
                if (currentDiff <= 0.01) {
                    return true
                }
            }
        }
        return false
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {

        // If camera is set to orthographical camera, the player should not be able to look around
        if (turnedCamera || collectedAllStars) return

        //Bewegung in x Richtung durch Differenz zwischen alter und neuer Position
        val deltaX: Double = xpos - oldMousePosX
        oldMousePosX = xpos
        oldMousePosY = ypos


        if (einbool) {
            tCamera.rotateAroundPoint(Math.toRadians(deltaX.toFloat() * 0.05f), 0.0f, 0.0f, Vector3f(0.0f))
        }
        einbool = true

    }

    fun cleanup() {}
}